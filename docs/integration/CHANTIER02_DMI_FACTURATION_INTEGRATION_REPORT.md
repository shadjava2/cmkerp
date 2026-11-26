# Chantier 02 — DMI × Facturation (MVP)

## Objectif

Orchestration **création dossier Mediline** + **affiliation ayant droit ERP** avec correspondances société ↔ compte Mediline et reprise idempotente par `request_id`.

## Livrables

| Zone | Contenu |
| --- | --- |
| Migration V36 | Colonnes `mediline_*` sur `erp_societes`, table `erp_patient_registration` |
| API Facturation | `GET/PUT /api/v1/facturation/mappings/societe-comptes`, `GET /api/v1/facturation/lookups/mediline-comptes` |
| API DMI | `POST /api/v1/dmi/registrations` |
| UI | `AffiliationEditor`, correspondances Mediline, parcours Nouveau dossier en 3 étapes |
| RBAC | Catalogue `ApiPermissionCatalog` (FACTURATION_SOCIETES_*, DMI_ACCUEIL_NOUVEAU_DOSSIER, FACTURATION_AYANTSDROIT_CREATE) |

## Règles d’intégration

1. **Affiliation avec société ERP** : mapping `MATCHED` obligatoire (`mediline_compte_code` renseigné).
2. **Création Mediline** : série IPP via `CardexCreationRequests` — reprise même `request_id` sans double INSERT.
3. **Aucune suppression** patient Mediline depuis l’orchestrateur.
4. **Beach** : écritures TAYANTDROIT restent soumises à la whitelist existante (défaut hors TCONTRAT).
5. **CLINIQUE** : lookups `t_compte` en SELECT Mediline uniquement.

## Écarts / suite

- `FacturationAyantsDroitPage` branché sur `AffiliationEditor` (modes facturation / edit).
- Tests 200/403 live + preuve SQL ADMIN + Flyway V36 sur la base cible.
- Rebuild gateway obligatoire après mise à jour `cmkerp-shared-kernel` (JAR fat).
- Première recette réelle : nouveau dossier justifié → Mediline + `erp_ayants_droit` + Outbox ; Beach après autorisation ciblée.
- Modification d’affiliation depuis le Cardex DMI (hors création) : parcours à brancher sur le même service `ErpAyantService.update`.