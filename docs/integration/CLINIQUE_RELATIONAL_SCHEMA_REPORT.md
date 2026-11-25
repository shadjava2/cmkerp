# CLINIQUE — Rapport schéma relationnel ERP

Date : 2026-03-25

## Décision

`erp_clinic_referentials` (JSON) n’est plus la source de vérité métier. Les tables `erp_*` typées portent les données, FK et libellés. La table générique reste un pont d’import / contrôle.

## Tables créées (V32)

| Table | Rôle |
| --- | --- |
| `erp_services` | TSERVICE |
| `erp_imputations` | TIMPUTATION |
| `erp_locations` | TLOCATION (+ `fk_site`) |
| `erp_medecins` | TMEDECIN |
| `erp_societes` | TSOCIETE → `fk_contrat` |
| `erp_classes` | TCLASSE → `fk_service` |
| `erp_tarifs` | TARIF → service / classe / imputation |
| `erp_ayants_droit` | TAYANTDROIT → société / contrat / titulaire / location |
| `erp_user_external_mappings` | TUTILISATEUR (sans mot de passe) |
| `erp_pharmacy_external_mappings` | TPHARMACIE → pharmacies ERP |
| `erp_product_external_mappings` | TPRODUIT → produits ERP |
| `erp_stock_clinique` | Projection stock normalisée |
| `erp_facturation_medicaments` | Structure transactionnelle |
| `erp_facturation_prestations` | Structure transactionnelle |

`erp_contrats` / `erp_sync_outbox` inchangés (V30).

## Volumes migrés (cmkerp-v24prod)

| Entité | Source générique | Table dédiée | FK rapprochées |
| --- | ---: | ---: | --- |
| Services | 43 | 43 | — |
| Imputations | 49 | 49 | — |
| Locations | 3 | 3 | — |
| Médecins | 140 | 140 | — |
| Sociétés | 484 | 484 | 484/484 → contrats |
| Classes | 24 | 24 | rapprochement service texte/code |
| Tarifs | 1977 | 1977 | service 1977 ; imputation 1936 |
| Ayants droit | 30412 | 30412 | société 30411 ; contrat 30408 |
| Users map | 18 | 18 | UNMATCHED |
| Pharmacies map | 7 | 7 | UNMATCHED |
| Produits map | 5177 | 5177 | MATCHED 1966 |

Aucune intention Outbox générée par cette migration.
WRITE Beach reste `TCONTRAT` uniquement.
