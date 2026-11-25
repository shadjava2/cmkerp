# Cartographie données CLINIQUE Beach → MySQL ERP

| Table Beach | Cible MySQL | Statut |
| --- | --- | --- |
| `TCONTRAT` | `erp_contrats` | Opérationnel (import + CRUD ERP) |
| `TSOCIETE` | `erp_societes` | Planifié Lot 2 |
| `TSERVICE` | `erp_services` | Planifié Lot 1 |
| `TCLASSE` | `erp_classes` | Planifié Lot 2 — SERVICE = texte, pas code |
| `TIMPUTATION` | `erp_imputations` | Planifié Lot 1 |
| `TARIF` | `erp_tarifs` | Planifié Lot 2 — montants DECIMAL |
| `TLOCATION` | `erp_locations` / mapping `sites` | Menu structuré |
| `TUTILISATEUR` | mapping `utilisateurs` | Sans MOTDEPASSE |
| `TPHARMACIE` | mapping `pharmacies` | Lot 3 |
| `TPRODUIT` | référentiel produit ERP | Lot 3 |
| `TSTOCK` | projection normalisée | Lecture seule Lot 4 |
| `TMEDICAMENT` | `erp_facturation_medicaments` | Lot 5 |
| `TPRESTATION` | `erp_facturation_prestations` | Lot 5 |
| `TMEDECIN` | référentiel / `erp_medecins` | Audit schéma requis |
| `TAYANTDROIT` | `erp_ayants_droit` | Audit schéma requis |

## TCONTRAT (confirmé)

- CODE nchar(3) PK → `erp_contrats.code` VARCHAR(3)
- DESIGNATION nvarchar(50) → `designation`
- `actif` = **ERP seulement** (jamais en SQL Server)
