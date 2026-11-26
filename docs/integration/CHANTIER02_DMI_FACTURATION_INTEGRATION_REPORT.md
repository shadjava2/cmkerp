# Chantier 02 — DMI × Facturation (MVP)

Voir également `docs/integration/CHANTIER02_DMI_FACTURATION_INTEGRATION_REPORT.md` à la racine du workspace STSDEV si présent.

## Objectif

Orchestration **création dossier Mediline** + **affiliation ayant droit ERP** avec correspondances société ↔ compte Mediline et reprise idempotente par `request_id`.

## Livrables

| Zone | Contenu |
| --- | --- |
| Migration V36 | Colonnes `mediline_*` sur `erp_societes`, table `erp_patient_registration` |
| API Facturation | `GET/PUT /api/v1/facturation/mappings/societe-comptes`, `GET /api/v1/facturation/lookups/mediline-comptes` |
| API DMI | `POST /api/v1/dmi/registrations` |
| UI (`consolenotification`) | `AffiliationEditor`, correspondances Mediline, parcours Nouveau dossier en 3 étapes |
| RBAC | Catalogue `ApiPermissionCatalog` |

## Règles

1. Affiliation avec société ERP : mapping `MATCHED` obligatoire.
2. Création Mediline : `CardexCreationRequests` — idempotence `request_id`.
3. Jamais de suppression patient Mediline depuis l’orchestrateur.
4. Beach : whitelist inchangée (pas d’activation écritures Beach dans ce chantier).

## Écarts

- `FacturationAyantsDroitPage` : refactor complet sur `AffiliationEditor` à finaliser.
- Preuves SQL/UI live + `RBAC_INVENTORY.md`.
- `mvn package` gateway : repackage bloqué si JAR verrouillé (compile OK).