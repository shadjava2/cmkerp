# CLINIQUE — Mapping des clés étrangères ERP

## Règle

- Identifiant interne MySQL = relation applicative.
- Code historique = compatibilité Beach / rapprochement.
- Relation non résolue = `fk_*` NULL + code `legacy_*` conservé + consultable.

## Matrice

| Enfant | Parent | FK | Legacy | Statut live |
| --- | --- | --- | --- | --- |
| `erp_societes` | `erp_contrats` | `fk_contrat` | `legacy_contrat_code` | 484/484 |
| `erp_classes` | `erp_services` | `fk_service` | `legacy_service_text` | partiel (texte historique) |
| `erp_tarifs` | `erp_services` | `fk_service` | `legacy_service_code` | 1977/1977 |
| `erp_tarifs` | `erp_classes` | `fk_classe` | `legacy_classe_code` | selon présence classe |
| `erp_tarifs` | `erp_imputations` | `fk_imputation` | `legacy_imputation_code` | 1936/1977 |
| `erp_ayants_droit` | `erp_societes` | `fk_societe` | `legacy_codesoc` | 30411/30412 |
| `erp_ayants_droit` | `erp_contrats` | `fk_contrat` | `legacy_contrat_code` | 30408/30412 |
| `erp_ayants_droit` | `erp_locations` | `fk_location` | `legacy_location_code` | selon code location |
| `erp_ayants_droit` | `erp_ayants_droit` | `fk_titulaire` | `code_famille` | si matricule distinct |
| `erp_locations` | `sites` | `fk_site` | — | manuel / overlay |
| `erp_product_external_mappings` | `produits` | `erp_product_id` | `legacy_product_code` | 1966 MATCHED |
| `erp_pharmacy_external_mappings` | `pharmacies` | `erp_pharmacy_id` | — | à rapprocher UI |
| `erp_user_external_mappings` | utilisateurs ERP | `erp_user_id` | `legacy_user_code` | UNMATCHED (manuel) |

## Affichage API

Les listes relationnelles exposent `*Designation` / `*Libelle` en plus des codes. L’UI Facturation affiche le libellé en priorité, le code en secondaire.
