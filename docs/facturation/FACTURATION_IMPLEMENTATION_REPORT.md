# Facturation V2 — Rapport d'implémentation

**Date** : 2026-11-25  
**Commit message proposé** : `feat(billing): implement enterprise billing workspace and financial dashboard with hierarchical RBAC`

## Livré

### Structure module
- Sidebar catégorisée (5 domaines + tableau de bord + retour portail)
- Sidebar rétractable desktop, mobile drawer
- Header avec fil d'Ariane
- Routes `/facturation/*` + redirects v1 (`/contrats`, `/privees`, `/sync`)

### Dashboard
- KPI médicaments / privé / contrat depuis `consommation-services`
- Graphique évolution CSS (pas de nouvelle dépendance chart)
- Top produits
- Cartes « Donnée indisponible » pour encaissements / créances / prestations / contrats

### Pages opérationnelles
- Médicaments (hub + contrats + privées) — moteur ventes existant
- Tarifs — sync Beach ↔ CH protégée
- Produits — catalogue Stock

### Pages structurées (shell)
Contrats, Ayants droit, Sociétés, Prestations, Imputations, Classes, Médecins, Services, Pharmacies, Stock, Utilisateurs

### RBAC
- Migration `V29__rbac_facturation_v2_sidebar.sql` (platform / gateway / stocks)
- Seed `RbacSchemaInitializer.seedModuleFacturationV2`
- Catalogue seed + libellés FR
- `PermissionRoute` sur chaque entrée
- Script `scripts/apply-rbac-v29-facturation.ps1`
- Compliance check étendu V29

### Documentation
- `docs/facturation/FACTURATION_ARCHITECTURE.md`
- `docs/facturation/FACTURATION_DATA_MAPPING.md`
- `docs/facturation/FACTURATION_DASHBOARD_KPI.md`
- `docs/facturation/FACTURATION_IMPLEMENTATION_REPORT.md`
- Inventaire RBAC mis à jour

## Non livré / backlog métier

- Agrégation prestations médicales
- Encaissements / créances / caisse
- CRUD conventions / ayants droit / sociétés
- Endpoints dashboard Facturation dédiés (isolation permissions API fine)
- Bibliothèque de graphiques interactive (drill-down avancé)

## Commandes de mise à jour (environnement local)

```powershell
# 1. Appliquer le catalogue RBAC V29
cd c:\Users\HP\Videos\STSDEV\cmkerp
.\scripts\apply-rbac-v29-facturation.ps1

# 2. Conformité RBAC
bash scripts/rbac-compliance-check.sh

# 3. Rebuild / redémarrage gateway (Flyway off : seed au boot aussi)
# Redémarrer le processus gateway Java, puis se reconnecter ADMIN

# 4. Frontend
cd c:\Users\HP\Videos\STSDEV\consolenotification
npm run desktop:dev
```

## Recette minimale

| Contrôle | Attendu |
| --- | --- |
| ADMIN voit les 15 menus | Oui après V29 + sync |
| Rôle sans `SIDEBAR_FACTURATION_MEDICAMENTS` | Menu masqué + route redirigée |
| Dashboard sans inventer encaissements | « Donnée indisponible » |
| Ventes médicaments | Fonctionnelles sous `/facturation/medicaments/...` |
| Sync tarifs | Sous Tarifs, permission `STOCK_FACTURATION_SYNC` |
| `SIDEBAR_FACTURATION` DMI | Inchangé |

## Distinction des états

| Opérationnel | Structuré | Complément métier requis |
| --- | --- | --- |
| Dashboard (médicaments) | Contrats / ayants / sociétés | Conventions REST |
| Médicaments ventes | Prestations | Agrégation soins |
| Tarifs sync | Imputations / classes / médecins / services | Confirmation schéma |
| Produits | Pharmacies / stock lecture / utilisateurs | API lecture seule + lien admin |
