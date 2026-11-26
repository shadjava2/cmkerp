# Chantier 02 — Rapport pré-recette (adaptateurs Beach)

Date : 2026-11-26  
Périmètre : vérification technique avant première création réelle DMI → Mediline → `erp_ayants_droit` → `TAYANTDROIT`.

## Synthèse

| Critère | État |
| --- | --- |
| Whitelist `.env` (12/15) | Configurée et relue live |
| Adaptateurs INSERT/UPDATE catalogue | Présents pour les 12 entités activées |
| Worker outbox multi-entités | `workerExecutes` = 12 (probe live) |
| Mapping société / compte Mediline | UI + API + suggestions (23/40 sur 1ʳᵉ page) |
| MATCHED obligatoire à la création/maj ayant | Oui (`ErpAyantService`) |
| Outbox après commit TAYANTDROIT | Oui (`afterCommit` → `flushPendingBatch`) |
| Reprise manuelle outbox | Oui (`POST …/outbox/{id}/retry` + bouton Relancer) |
| Recette live 10/10 | **0/10** — à valider par l’utilisateur (pas de patient artificiel) |

### Probe live (2026-11-26)

- `GET /adapters/outbound` → `writeEntities` = 12 entités ; `beachWriteEnabled=true`
- `GET /ayants/meta` → `beachWriteEnabled=true`
- `GET /mappings/societe-comptes/suggestions` → `totalUnmatched=484` ; propositions scorées (ex. PREMIUM FOODS → compte `PR`, score 95)

## Matrice adaptateurs

Légende :

- **Implémenté** = code INSERT/UPDATE présent
- **Autorisé** = présent dans `CLINIC_BEACH_WRITE_ENTITIES`
- **Testé live** = écriture Beach confirmée en conditions réelles
- **Opérationnel prouvé** = statut outbox `SYNCED` après relecture SQL Server

| Entité | Implémenté | Autorisé | Testé live | Opérationnel prouvé | Mécanisme |
| --- | --- | --- | --- | --- | --- |
| TCONTRAT | Oui | Oui | Oui (historique) | Oui (historique) | Adaptateur dédié worker |
| TSOCIETE | Oui | Oui | Non | Non | `ClinicReferentialLegacyWriter` |
| TAYANTDROIT | Oui | Oui | Non (attente recette) | Non | Catalogue + verify MATRICULE |
| TSERVICE | Oui | Oui | Non | Non | Catalogue |
| TCLASSE | Oui | Oui | Non | Non | Catalogue |
| TIMPUTATION | Oui | Oui | Non | Non | Catalogue |
| TARIF | Oui | Oui | Non | Non | Catalogue |
| TLOCATION | Oui | Oui | Non | Non | Catalogue |
| TMEDECIN | Oui | Oui | Non | Non | Catalogue |
| TUTILISATEUR | Oui (UPDATE only) | Oui | Non | Non | Catalogue (`outboundInsert=false`) |
| TPHARMACIE | Oui | Oui | Non | Non | Catalogue |
| TPRODUIT | Oui | Oui | Non | Non | Catalogue |
| TSTOCK | Non (projection) | Non | — | — | Différé |
| TMEDICAMENT | Intention only | Non | — | — | Différé |
| TPRESTATION | Intention only | Non | — | — | Différé |

Source runtime : `GET /api/v1/facturation/adapters/outbound`.

## Chaîne Chantier 02 (à valider en recette)

1. Correspondance société ERP ↔ compte Mediline (`MATCHED`, validation humaine ; suggestions disponibles).
2. DMI — Nouveau dossier → orchestrateur (`PatientRegistrationOrchestrator`).
3. Création Mediline (IPP + compte résolu via mapping).
4. Création `erp_ayants_droit` via `ErpAyantService.create` (même service que Facturation).
5. Intention outbox `TAYANTDROIT` `PENDING` puis flush post-commit.
6. INSERT/UPDATE Beach + verify → `SYNCED`.
7. Modification affiliation DMI (Cardex) et Facturation → même `ErpAyantService.update`.

## Idempotence / reprise

- Doublons ERP : conflit sur matricule / IPP Mediline.
- Outbox : replace payload si intention ouverte ; claim atomique PENDING → IN_PROGRESS.
- Échec / timeout → `FAILED` / `UNCERTAIN` visibles au centre de synchronisation.
- Reprise auto (`requeueFailedReady`) + reprise manuelle UI/API.
- Bases indisponibles : intention conservée ; pas d’écrasement silencieux.

## Non-objectifs (inchangés)

- Pas de patient de test artificiel en production.
- Pas de chantier 03.
- Pas d’activation TSTOCK / TMEDICAMENT / TPRESTATION.
- DELETE Beach / DDL / écritures CH toujours interdits.
