# CLINIQUE Migration — Rapport maître

Date : **2026-11-25 (nuit)**  
Source officielle de suivi. Nomenclature des lots **inchangée**.

## Distinction obligatoire

| Niveau | Signification |
| --- | --- |
| **Implémenté** | Code présent dans le dépôt |
| **Testé** | Preuve unitaire / manuelle exécutée |
| **Validé recette** | Démontré sur SQL Server **isolé** |
| **Autorisé production** | Flag write + allow-list + autorisation métier |

## Feuille de route Facturation V1 — **1/8 partiel**

| # | Livrable | État |
| --- | --- | --- |
| 0 | Lot 0 Infrastructure / protection | **Implémenté + testé live** — recette isolée encore bloquée |
| 1 | Lot 1 Contrats TCONTRAT | **Implémenté + preuve live TCC→SYNCED** — recette isolée encore bloquée |
| 2 | Lot 2 Services, Imputations, Locations | **Moteur générique prêt** — import ERP autorisé ; écriture Beach **non** autorisée |
| 3 | Lot 3 Sociétés, Classes, Tarifs | Préparé (catalogue V31) |
| 4 | Lot 4 Médecins, Ayants droit, Utilisateurs | Préparé (UI vide tant qu’import non exécuté) |
| 5 | Lot 5 Pharmacies, Produits, Stock, Médicaments | Préparé |
| 6 | Lot 6 Prestations | Préparé (import-only) |
| 7 | Recette finale + dashboard financier | Exigence maintenue — pas démarrée |

## Lot 0 / Lot 1 — preuves

| Capacité | Preuve |
| --- | --- |
| Réplication ERP → Beach | `TCC` outbox `SYNCED` attempt=1 ; Beach `TCONTRAT` = `TEST2` (lecture SQL Server 192.168.100.254) |
| Sync entrante | Logs `Inbound TCONTRAT : MAJ=0 conflits=0 nouveaux=0` |
| Login après croissance RBAC | JWT sans catalogue permissions ; cookies compacts |
| Atomicité métier + outbox | Même `@Transactional` create/update ; doc `CLINIQUE_OUTBOX_TRANSACTION_BOUNDARY.md` |
| Worker / rollback statut | `REQUIRES_NEW` **uniquement** claim/SYNCED/FAILED ; flush **après** `afterCommit` |
| Allow-list prod | `CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT` — non étendue automatiquement |
| CH / DDL / DELETE | `CH_WRITE=false`, `DELETE=false`, aucun DDL |
| Recette SQL Server isolée | **BLOQUÉ** — Docker / Testcontainers indisponible sur ce poste (`NO_DOCKER`) |

### Verdict Lots 0–1

**Techniquement opérationnels sur Beach de développement** pour TCONTRAT.  
**Non déclarés « Validé recette isolée »** tant qu’une instance SQL Server dédiée n’existe pas.  
**Autorisation écriture production** : limitée à `TCONTRAT` via allow-list ; documenter explicitement l’accord métier avant prod hospitalière.

## Transactions Outbox (audit)

Voir `docs/integration/CLINIQUE_OUTBOX_TRANSACTION_BOUNDARY.md`.

- Intention créée **dans** la TX MySQL de la fiche.
- `REQUIRES_NEW` = worker uniquement (pas l’enregistrement initial).
- Référentiels Lot 2+ : statut outbox via `beachEntityWriteEnabled(entity)` → hors allow-list = `SUSPENDED`.

## Lot 2 — démarrage

Architecture : `TSERVICE` / `TIMPUTATION` / `TLOCATION` → `erp_clinic_referentials` (pas de tables `erp_services` dupliquées).

| Source Beach | Volumes lus (2026-11-25) | ERP MySQL |
| --- | --- | --- |
| TSERVICE | 43 | 0 (import à exécuter) |
| TIMPUTATION | 49 | 0 |
| TLOCATION | 3 | 0 |

Écriture Beach Lot 2 : **interdite** tant que `WRITE_ENTITIES` n’inclut pas explicitement ces entités.

## Dashboard financier

Livrable **maintenu** pour la recette finale Facturation V1 (prestations, médicaments, société/contrat/service/site, évolution temporelle).  
Ne pas présenter facturé = encaissé sans source de règlement.

## Configuration cible sûre

```
CLINIC_BEACH_SYNC_ENABLED=true
CLINIC_BEACH_WRITE_ENABLED=true          # uniquement si autorisé métier
CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT     # ne pas étendre sans autorisation
CLINIC_BEACH_INBOUND_ENABLED=true
CLINIC_BEACH_EXCLUDE_CODES=TES
CLINIC_BEACH_DELETE_ENABLED=false
CLINIC_CH_WRITE_ENABLED=false
```

## Commandes de mise à jour

```powershell
cd c:\Users\HP\Videos\STSDEV\cmkerp
git pull
# Rebuild shared-kernel → platform → gateway ; redémarrer gateway avec .env

cd c:\Users\HP\Videos\STSDEV\consolenotification
git pull
```

## Prochaine action automatique

1. Importer sélectivement Services / Imputations / Locations (CLINIQUE → ERP) via UI.
2. Fournir instance SQL Server de recette isolée pour lever le blocage « Validé recette ».
3. Lots 3–6 sur le même moteur ; écritures Beach uniquement après autorisation par entité.
4. Brancher le dashboard financier après données prestations/médicaments.
