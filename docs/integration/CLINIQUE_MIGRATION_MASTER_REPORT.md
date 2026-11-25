# CLINIQUE Migration — Rapport maître

Date : **2026-03-25**  
Écritures Beach : **`WRITE_ENTITIES=TCONTRAT` uniquement**. CH : aucune écriture.

## Synthèse

| Lot | Thème | Importé | Écriture Beach |
| --- | --- | --- | --- |
| 0–1 | Infra / Contrats | Oui (TCC) | TCONTRAT — **recette isolée restante** |
| 2 | Services, Imputations, Locations | 43+49+3 | Non |
| 3 | Sociétés, Classes, Tarifs | 484+24+1977 | Non |
| 4 | Médecins, Ayants droit, Utilisateurs | 140+30412+18 | Non |
| 5 | Pharmacies, Produits, Stock, Médicaments | 7+5177+…+2000* | Non |
| 6 | Prestations | **2000*** / ~981k | Non (import seul) |
| Autonome | Normalisation relationnelle V32 | **Oui** | Non (migration MySQL only) |

\* Volumes historiques massifs : preuve **sélective** opérationnelle.

## Architecture relationnelle (2026-03-25)

- Standard permanent : `.cursor/rules/erp-relational-data-standard.mdc` (+ AGENTS.md → six standards).
- Migration `V32__erp_facturation_relational_tables.sql` appliquée sur `cmkerp-v24prod`.
- Données migrées hors `erp_clinic_referentials` vers tables typées + FK (voir `CLINIQUE_RELATIONAL_SCHEMA_REPORT.md`).
- API list/get/actif : lecture prioritaire tables `erp_*` avec libellés joints.
- Import sélectif : miroir idempotent vers tables typées **sans outbox**.
- UI : libellés société/contrat/service/imputation ; sélecteurs métier (plus de codes seuls en saisie).

## Docs associées

- `CLINIQUE_AUTONOMOUS_EXECUTION_TRACKER.md`
- `CLINIQUE_RELATIONAL_SCHEMA_REPORT.md`
- `CLINIQUE_FOREIGN_KEY_MAPPING_REPORT.md`
- `CLINIQUE_PRODUCTION_READINESS_REPORT.md`

## Clôture officielle Lots 0–1

Bloquée tant que `CLINIQUE_ISOLATED_SQLSERVER_RECIPE.md` n’est pas exécutée.

## Suite autonome restante

PatientPicker Mediline, mapping UI users/pharmacies, formulaires prestations/médicaments transactionnels, dashboard financier agrégé — **sans élargir WRITE_ENTITIES**.
