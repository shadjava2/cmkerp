# Cartographie données CLINIQUE Beach → MySQL ERP

| Table Beach | Cible MySQL | Statut |
| --- | --- | --- |
| `TCONTRAT` | `erp_contrats` | Opérationnel (import + CRUD ERP) |
| `TSOCIETE` | `erp_clinic_referentials` / `SOCIETES` | Moteur V31 — CODESOC, NOMSOC, ADRESSE, N_IMPOT, NRC, ID_NAT, CONTRAT |
| `TSERVICE` | `erp_clinic_referentials` / `SERVICES` | Moteur V31 — CODE(2), LIBELLE |
| `TCLASSE` | `erp_clinic_referentials` / `CLASSES` | Moteur V31 — SERVICE = texte |
| `TIMPUTATION` | `erp_clinic_referentials` / `IMPUTATIONS` | Moteur V31 — CODE(7), DESIGNATION |
| `TARIF` | `erp_clinic_referentials` / `TARIFS` | Moteur V31 — CODEPRES, LIBPRES, MONTANT, MONTPRIVE |
| `TLOCATION` | `erp_clinic_referentials` / `LOCATIONS` | Moteur V31 — CODE entier, LIBELLE, ADRESSE |
| `TUTILISATEUR` | `erp_clinic_referentials` / `UTILISATEURS` | Moteur V31 — CODE, NOM, CATEGORIE, LOCATION. Mot de passe jamais lu |
| `TPHARMACIE` | `erp_clinic_referentials` / `PHARMACIES` | Moteur V31 — CODE, LIBELLE |
| `TPRODUIT` | `erp_clinic_referentials` / `PRODUITS` | Moteur V31 — fiche produit. Écriture Beach distincte du sync stock existant |
| `TSTOCK` | `erp_clinic_referentials` / `STOCK` | Projection en lecture. Aucune écriture sortante |
| `TMEDICAMENT` | `erp_clinic_referentials` / `MEDICAMENTS` | Import sélectif seul. Pas d'écriture sortante |
| `TPRESTATION` | `erp_clinic_referentials` / `PRESTATIONS` | Import sélectif seul. Pas d'écriture sortante |
| `TMEDECIN` | `erp_clinic_referentials` / `MEDECINS` | Moteur V31 — CODE, NOM, SPECIALITE, CODECMK, NBREPATIENT |
| `TAYANTDROIT` | `erp_clinic_referentials` / `AYANTS_DROIT` | Moteur V31 — MATRICULE et colonnes d'identité |

## TCONTRAT (confirmé)

- CODE nchar(3) PK → `erp_contrats.code` VARCHAR(3)
- DESIGNATION nvarchar(50) → `designation`
- `actif` = **ERP seulement** (jamais en SQL Server)
