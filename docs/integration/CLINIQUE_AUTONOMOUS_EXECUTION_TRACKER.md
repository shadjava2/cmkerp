# CLINIQUE — Autonomous Execution Tracker

Dernière mise à jour : **2026-11-25** (FINAL RELEASE AUDIT)

| ID | Lot | Description | État | Résultat / blocage |
| --- | --- | --- | --- | --- |
| A0–A6 | 0–4 | Audit, V32, libellés, sélecteurs | DONE | Commits antérieurs |
| A5b | 4 | PatientPicker | DONE | À recetter UI (0 LINKED live) |
| R1 | rel | SoT erp_* + V33 patient_link | DONE | Confirmé MySQL live |
| R2 | 4 | Mapping utilisateurs | DONE | `erp_user_external_mappings` (18) |
| R3 | 5 | Mapping pharmacies | DONE | `erp_pharmacy_external_mappings` (7) |
| R4 | 6 | Poste prestations | DONE | **Plus LOCAL_ONLY** → `BLOCKED_*`/`SUSPENDED` + outbox |
| R5 | 5 | Poste médicaments | DONE | Idem + stockMovement NONE |
| R6 | dash | Dashboard relationnel | DONE | À recetter UI |
| R7 | rbac | CREATE + ApiPermissionCatalog | DONE | |
| R8 | docs | MASTER réécrit + UAT + cartographie règles | DONE | `CLINIQUE_MIGRATION_MASTER_REPORT.md` |
| F2 | sync | Correction politique LOCAL_ONLY facturation | DONE | Audit final |
| BLK | 0/1 | Recette SQL Server isolée | BLOCKED | Pas d’env isolé |
| BLK | beach | WRITE hors TCONTRAT | BLOCKED | Autorisation métier |
| OPEN | stock | Mapping STOCK1…n | OPEN | Non inventé |
| OPEN | idmvt | IDMVT / IDM / tarifs histo. | OPEN | Voir cartographie |

## Volumes live (cmkerp-v24prod)

Services 43 · Imputations 49 · Locations 3 · Médecins 140 · Sociétés 484 · Classes 24 · Tarifs 1977 · Ayants 30412 (LINKED 0) · Contrats 16 · User map 18 · Pharma map 7 · Prod map 5177 · Prestations 0 · Médicaments tx 0 · Referentials 46983 · Outbox 2

## V32 / V33

Appliquées **effectivement** en MySQL (tables + colonnes `patient_link_status` / `mediline_ipp`), indépendamment de Flyway history.

## Recette utilisateur

`docs/integration/CLINIQUE_FINAL_USER_ACCEPTANCE.md` — score personnel **0/19**
