# CLINIQUE — Autonomous Execution Tracker

Dernière mise à jour : 2026-03-25

| ID | Lot | Description | Dépendances | État | Fichiers | Tests | Résultat | Anomalies / blocages |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A0 | 0/1 | Audit `erp_clinic_referentials` + volumes live | — | DONE | MySQL `cmkerp-v24prod` | SQL counts | SERVICES 43, IMPUTATIONS 117, LOCATIONS 3, SOCIETES 620, CLASSES 33, TARIFS 1977, MEDECINS 140, AYANTS 30412, USERS 83, PHARMA 16, PRODUITS 5765, STOCK 5765, MEDICAMENTS 2000, PRESTATIONS 2000 | Seules tables `erp_*` métier avant V32 : `erp_contrats`, `erp_sync_outbox`, `erp_clinic_referentials` |
| A1 | std | Standard `erp-relational-data-standard.mdc` + AGENTS | — | DONE | `.cursor/rules/erp-relational-data-standard.mdc`, `AGENTS.md` | — | alwaysApply | — |
| A2 | 2–4 | Migration V32 tables référentielles dédiées | A0 | DONE | `V32__erp_facturation_relational_tables.sql` | SQL apply | Tables + FK OK | Apply manuel (Flyway history vide) |
| A3 | 2–4 | Migration données JSON → tables typées (sans outbox) | A2 | DONE | V32 DML | counts | 30412 ayants, 484 sociétés… | Aucun outbox |
| A4 | 3–4 | API list/actif relationnelle + miroir import | A3 | DONE | `ErpFacturationRelationalStore`, `ClinicReferentialService` | unit tests | Compile + tests OK | create/update complets à durcir |
| A5 | 4 | UI libellés + sélecteurs Sociétés/Ayants/Tarifs | A4 | DONE | `FacturationReferentialPage.tsx` | manuel | — | — |
| A5b | 4 | PatientPicker Mediline sur Ayants | A5 | DONE | `PatientPicker.tsx` | — | push `1fdb813` | Persist `mediline_ipp` relationnel à durcir |
| A6 | 2–3 | API/UI Services… via store relationnel | A3 | DONE | list/actif relational | unit | OK | Formulaires create/update mixtes |
| A7 | 4 | Mapping users / pharmacies / produits | A2 | PARTIAL | tables mappings | SQL | 1966 produits MATCHED | UI mapping manuelle restante |
| A8 | 5–6 | Tables transactionnelles médicaments / prestations | A2 | PENDING | — | — | — | WRITE Beach non étendu |
| A9 | final | Docs schema/FK/readiness + commit/push | A4+ | PENDING | docs/integration/* | compliance | — | Recette SQL Server isolée Lots 0/1 ouverte |
| BLK | 0/1 | Recette écriture Beach isolée | — | BLOCKED | Testcontainers/Docker | — | — | Pas d’env SQL Server isolé ; prod WRITE=TCONTRAT only |

## Volumes source (avant normalisation)

| referential_key | count |
| --- | ---: |
| SERVICES | 43 |
| IMPUTATIONS | 117 |
| LOCATIONS | 3 |
| SOCIETES | 620 |
| CLASSES | 33 |
| TARIFS | 1977 |
| MEDECINS | 140 |
| AYANTS_DROIT | 30412 |
| UTILISATEURS | 83 |
| PHARMACIES | 16 |
| PRODUITS | 5765 |
| STOCK | 5765 |
| MEDICAMENTS | 2000 (échantillon) |
| PRESTATIONS | 2000 (échantillon) |
