#!/usr/bin/env python3
"""
Script pour convertir un dump Navicat MySQL en migration Flyway baseline.

Usage:
    python process_dump_to_flyway.py
"""

import re
from pathlib import Path
from typing import List, Tuple, Set

def extract_create_tables(content: str) -> List[Tuple[str, str]]:
    """Extrait les CREATE TABLE du dump."""
    tables = []

    # Pattern pour capturer CREATE TABLE complet (multiligne)
    # Format: CREATE TABLE `nom` ( ... ) ENGINE = ... ;
    pattern = r'CREATE TABLE\s+`?(\w+)`?\s*\((.*?)\)\s*(ENGINE\s*=\s*[^;]+);'

    matches = re.finditer(pattern, content, re.DOTALL | re.IGNORECASE | re.MULTILINE)

    for match in matches:
        table_name = match.group(1)
        table_body = match.group(2).strip()
        engine_part = match.group(3).strip()

        # Reconstruire le CREATE TABLE complet avec IF NOT EXISTS
        full_create = f"CREATE TABLE IF NOT EXISTS `{table_name}` (\n{table_body}\n) {engine_part};"
        tables.append((table_name, full_create))

    return tables

def extract_foreign_keys(table_body: str) -> Set[str]:
    """Extrait les noms de tables référencées par les FK."""
    fk_refs = set()
    # Chercher FOREIGN KEY ... REFERENCES `table`
    pattern = r'FOREIGN\s+KEY.*?REFERENCES\s+`?(\w+)`?'
    matches = re.finditer(pattern, table_body, re.IGNORECASE)
    for match in matches:
        fk_refs.add(match.group(1))
    return fk_refs

def order_tables_by_dependencies(tables: List[Tuple[str, str]]) -> List[Tuple[str, str]]:
    """Ordonne les tables selon leurs dépendances FK (tri topologique)."""
    # Extraire les FK pour chaque table
    fk_map = {}
    table_dict = {}

    for table_name, create_sql in tables:
        table_dict[table_name] = create_sql
        # Extraire le corps de la table (entre parenthèses)
        body_match = re.search(r'\(([^)]+)\)\s*ENGINE', create_sql, re.DOTALL)
        if body_match:
            table_body = body_match.group(1)
            fk_map[table_name] = extract_foreign_keys(table_body)
        else:
            fk_map[table_name] = set()

    # Tri topologique
    ordered = []
    remaining = set(table_dict.keys())
    processed = set()

    while remaining:
        # Trouver les tables sans dépendances non traitées
        ready = [
            name for name in remaining
            if not fk_map.get(name, set()) - processed
        ]

        if not ready:
            # Cycle détecté ou erreur, prendre la première restante
            name = next(iter(remaining))
            ready = [name]

        for name in ready:
            ordered.append((name, table_dict[name]))
            processed.add(name)
            remaining.remove(name)

    return ordered

def clean_sql_content(content: str) -> str:
    """Nettoie le contenu SQL en supprimant les instructions Navicat."""
    lines = content.split('\n')
    cleaned_lines = []
    skip_until_semicolon = False

    for line in lines:
        stripped = line.strip()

        # Ignorer les lignes à supprimer
        if re.match(r'^/\*', stripped):  # Commentaires Navicat /* ... */
            continue
        if re.match(r'^SET\s+(NAMES|CHARACTER_SET|FOREIGN_KEY_CHECKS)', stripped, re.IGNORECASE):
            continue
        if re.match(r'^DROP\s+TABLE', stripped, re.IGNORECASE):
            continue
        if re.match(r'^(CREATE|USE)\s+DATABASE', stripped, re.IGNORECASE):
            continue
        if re.match(r'^LOCK\s+TABLE', stripped, re.IGNORECASE):
            skip_until_semicolon = True
            continue
        if re.match(r'^UNLOCK\s+TABLE', stripped, re.IGNORECASE):
            skip_until_semicolon = False
            continue
        if skip_until_semicolon:
            if ';' in line:
                skip_until_semicolon = False
            continue
        if re.match(r'^INSERT\s+INTO', stripped, re.IGNORECASE):
            # Ignorer les INSERT jusqu'à la fin de la ligne ou jusqu'au prochain CREATE
            continue
        if re.match(r'^DROP\s+(PROCEDURE|TRIGGER|FUNCTION)', stripped, re.IGNORECASE):
            # Ignorer les procédures/triggers (seront dans une migration séparée)
            skip_until_semicolon = True
            continue
        if re.match(r'^CREATE\s+(PROCEDURE|TRIGGER|FUNCTION)', stripped, re.IGNORECASE):
            skip_until_semicolon = True
            continue
        if skip_until_semicolon and 'delimiter' in stripped.lower():
            skip_until_semicolon = False
            continue

        # Garder les commentaires SQL standards (-- ...)
        if stripped.startswith('--') and 'Table structure' in stripped:
            # Garder les commentaires de structure de table
            cleaned_lines.append(line)
        elif stripped.startswith('--') and not any(x in stripped.lower() for x in ['navicat', 'dump', 'source', 'target']):
            # Garder les commentaires utiles
            cleaned_lines.append(line)
        elif stripped and not skip_until_semicolon:
            cleaned_lines.append(line)

    return '\n'.join(cleaned_lines)

def process_dump(input_file: str, output_file: str):
    """Traite le dump et génère le fichier Flyway."""
    print(f"Lecture du dump: {input_file}")
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    print("Nettoyage du contenu (suppression instructions Navicat)...")
    cleaned = clean_sql_content(content)

    print("Extraction des CREATE TABLE...")
    tables = extract_create_tables(cleaned)

    if not tables:
        print("Aucune table trouvee avec le pattern standard, tentative alternative...")
        # Fallback: chercher tous les CREATE TABLE même sans pattern parfait
        pattern = r'CREATE\s+TABLE\s+`?(\w+)`?\s*\([^;]+\);'
        matches = re.finditer(pattern, cleaned, re.DOTALL | re.IGNORECASE)
        for m in matches:
            # Extraire le CREATE TABLE complet
            start = m.start()
            # Trouver la fin (dernier ; avant le prochain CREATE ou fin)
            end_match = re.search(r'\)\s*ENGINE[^;]+;', cleaned[start:], re.DOTALL | re.IGNORECASE)
            if end_match:
                end = start + end_match.end()
                create_sql = cleaned[start:end]
                table_name = m.group(1)
                tables.append((table_name, create_sql.replace('CREATE TABLE', 'CREATE TABLE IF NOT EXISTS')))

    print(f"Trouve {len(tables)} tables")

    print("Ordonnancement selon les dependances FK...")
    ordered_tables = order_tables_by_dependencies(tables)

    print(f"Generation du fichier Flyway: {output_file}")

    # Créer le répertoire si nécessaire
    Path(output_file).parent.mkdir(parents=True, exist_ok=True)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("-- ==========================================\n")
        f.write("-- FLYWAY BASELINE - Schéma CMK-ERP\n")
        f.write("-- ==========================================\n")
        f.write("-- V1 : baseline schéma CMK-ERP (export cmkerp-v24prod du 29/11/2025)\n")
        f.write("-- Généré automatiquement depuis le dump Navicat\n")
        f.write("--\n")
        f.write("-- Ce fichier contient uniquement la structure (DDL),\n")
        f.write("-- sans données métiers.\n")
        f.write("-- Les données de référence seront dans V2__seed_reference_data.sql\n")
        f.write("-- Les procédures stockées et triggers seront dans V3__procedures_triggers.sql\n")
        f.write("-- ==========================================\n\n")

        for table_name, create_sql in ordered_tables:
            f.write(f"-- Table: {table_name}\n")
            f.write(create_sql)
            f.write("\n\n")

    print(f"Fichier genere: {output_file}")
    print(f"   {len(ordered_tables)} tables incluses")
    print(f"   Ordre respecte selon les dependances FK")

if __name__ == '__main__':
    input_file = r'c:\Users\HP\Desktop\Waves\cmkerp-v24prod.sql'
    output_file = Path(__file__).parent.parent.parent / 'cmkerp-platform' / 'src' / 'main' / 'resources' / 'db' / 'migration' / 'V1__baseline_cmkerp_schema.sql'

    if not Path(input_file).exists():
        print(f"❌ Fichier introuvable: {input_file}")
        exit(1)

    process_dump(input_file, str(output_file))
