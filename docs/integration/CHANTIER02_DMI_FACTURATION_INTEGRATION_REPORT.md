# Chantier 02 — DMI × Facturation (MVP)

## Objectif

Orchestration **création dossier Mediline** + **affiliation ayant droit ERP** avec correspondances société ↔ compte Mediline et reprise idempotente par `request_id`.

## Livrables

| Zone | Contenu |
| --- | --- |
| Migration V36 | Colonnes `mediline_*` sur `erp_societes`, table `erp_patient_registration` |
| Migration V37 | `erp_contrats.libelle_beneficiaire` + `mediline_categorie_code` (ERP only) |
| API Facturation | `GET/PUT /api/v1/facturation/mappings/societe-comptes`, `GET /lookups/mediline-comptes`, `GET /lookups/mediline-categories`, `POST /ayants/coverage/preview` |
| API DMI | `POST /api/v1/dmi/registrations` (PEC + catégorie résolues côté serveur depuis l’affiliation) |
| UI | `AffiliationEditor` (PEC/catégorie lecture seule), Contrats (classification bénéficiaires), Nouveau dossier étape 1 sans PEC/catégorie |
| RBAC | Catalogue `ApiPermissionCatalog` (FACTURATION_SOCIETES_*, CONTRATS_*, DMI_*_NOUVEAU_DOSSIER, FACTURATION_AYANTSDROIT_*) |

## Règles d’intégration

1. **Affiliation avec société ERP** : mapping `MATCHED` obligatoire (`mediline_compte_code` renseigné).
2. **PEC** : `SOCIÉTÉ` (titulaire) ou `SOCIÉTÉ/NOM_TITULAIRE` (dépendant) — calcul backend obligatoire ; UI = prévisualisation.
3. **Catégorie bénéficiaire** : issue de `erp_contrats.mediline_categorie_code` (codes réels Mediline `AFF` / `ATS` / `CASH` / `CONV`) — jamais inventée ni égale au code contrat CLINIQUE.
4. **Création Mediline** : série IPP via `CardexCreationRequests` — reprise même `request_id` sans double INSERT ; `MAIL`←PEC, `FK_CATEGORIE`←catégorie contrat, `FK_COMPTE_A_FACTURER`←mapping société.
5. **Aucune suppression** patient Mediline depuis l’orchestrateur.
6. **Beach** : écritures TAYANTDROIT restent soumises à la whitelist existante ; PEC / libellé bénéficiaire **non** répliqués vers `TAYANTDROIT` / `TCONTRAT`.
7. **CLINIQUE** : lookups `t_compte` / `t_categorie_patient` en SELECT Mediline uniquement.

## Écarts / suite

- `FacturationAyantsDroitPage` branché sur `AffiliationEditor` (modes facturation / edit).
- Tests 200/403 live + preuve SQL ADMIN + Flyway V36 sur la base cible.
- Rebuild gateway obligatoire après mise à jour `cmkerp-shared-kernel` (JAR fat).
- Première recette réelle : nouveau dossier justifié → Mediline + `erp_ayants_droit` + Outbox ; Beach après autorisation ciblée.
- Modification d’affiliation depuis le Cardex DMI (hors création) : parcours à brancher sur le même service `ErpAyantService.update`.