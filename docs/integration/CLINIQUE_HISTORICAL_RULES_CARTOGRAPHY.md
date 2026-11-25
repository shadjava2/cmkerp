# Cartographie des règles historiques CLINIQUE (lecture seule)

Date : **2026-11-25**  
Méthode : catalogue ERP (`ClinicReferentialCatalog`), schéma SQL Server documenté dans le code, volumes MySQL, **aucune invention** de règle financière.  
Environnement SQL Server isolé d’écriture : **non disponible** pour validation dynamique complète.

Légende : **Confirmé** | **Probable** | **Non déterminé**

---

## 1. Stock (`TSTOCK`)

### Colonnes observées (catalogue)

`CODE`, `DESIGNATION`, `UNITEMB`, `UNITGEST`, `STREP`, `STCRITIQ`, `STINV`, `STOCK1`…`STOCK20`, `PAU`, `DCI`

| Règle | Statut | Preuve / limite |
| --- | --- | --- |
| Quantités par emplacement stockées en colonnes `STOCKn` | **Confirmé** (structure) | Catalogue + schéma historique |
| Mapping `STOCKn` ↔ code pharmacie / dépôt / location | **Non déterminé** | Aucune table de correspondance fiable trouvée dans le code ERP ; **interdit d’inventer** |
| `PAU` = prix d’achat unitaire historique | **Probable** | Nom + usage catalogue médicaments |
| Écriture ERP → `TSTOCK` | **Bloquée** | Hors whitelist ; stock MySQL `erp_stock_clinique` = 0 lignes |

---

## 2. Prestations (`TPRESTATION`)

### Colonnes catalogue

`IDMVT`, `DATEPRES`, `NDATE`, `NOM`, `MATRICULE`, `CODEPRES`, `LIBPRES`, `K`, `MONTANT`, `CODEMED`, `MEDECIN`, `CODESERV`, `CODESOC`, `LIBSOC`, `CONTRASOC`, `CODEFAM`, `RELATION`, `CODECMK`, `SERIE`, `AFACT`, `DATESAIS`, `SOURCE`, `LOCATION`, `UTILISATEUR`, `IMPUTATION`

| Règle | Statut | Notes |
| --- | --- | --- |
| `IDMVT` identifiant mouvement / PK métier | **Confirmé** (rôle clé) | Mode import `IMPORT_ONLY` ; génération auto **Non déterminée** |
| Lien patient via `MATRICULE` / nom | **Confirmé** (colonnes) | ERP utilise `erp_ayants_droit` + Mediline IPP |
| Montant issu d’un tarif contractuel (`erp_tarifs` / tables histo.) | **Probable** | Tarifs ERP migrés (1977) ; formule exacte Beach **Non déterminée** |
| `K`, `SERIE`, `AFACT`, `CODECMK`, `SOURCE` | **Non déterminé** | Présents en colonnes ; sémantique métier non figée en code |
| Signature `UTILISATEUR` | **Confirmé** (besoin) | Mapping `erp_user_external_mappings` (18) ; signature backend |
| INSERT Beach depuis ERP | **Intention seulement** | Outbox `SUSPENDED` ; worker n’exécute pas `TPRESTATION` |

---

## 3. Médicaments (`TMEDICAMENT`)

### Colonnes catalogue

`IDM`, `MATRICULE`, `NOM`, `CODESOC`, `LIBSOC`, `CONTRASOC`, `CODEPROD`, `DESIGNATION`, `QTE`, `UNITGEST`, `PAU`, `MONTANT`, `DATEMVT`, `NDATE`, `AFACT`, `CODEMED`, `MEDECIN`, `CODESERV`, `DATESAIS`, `SOURCE`, `CODEPHARM`, `LOCATION`, `UTILISATEUR`

| Règle | Statut | Notes |
| --- | --- | --- |
| `IDM` identifiant mouvement | **Confirmé** (rôle clé) | Génération **Non déterminée** |
| `MONTANT` ≈ f(`QTE`, `PAU`) | **Probable** | Hypothèse classique ; **ne pas coder en production** sans preuve |
| Lien pharmacie `CODEPHARM` | **Confirmé** (colonne) | Mapping pharmacies ERP 7 lignes |
| Sortie de stock automatique à l’enregistrement ERP | **Interdit actuellement** | Poste médicament : `stockMovement=NONE` ; pas de double stock |
| INSERT Beach | **Intention seulement** | Même politique que prestations |

---

## 4. Décisions explicitement hors scope agent

- Ne pas activer `WRITE_ENTITIES` pour `TPRESTATION` / `TMEDICAMENT` / `TSTOCK`.  
- Ne pas inventer un mapping `STOCKn`.  
- Ne pas transformer une hypothèse PAU×QTE en règle financière livrée.  
- Toute activation = autorisation métier + recette isolée.

---

## 5. Prochaines preuves recommandées (lecture seule)

1. Métadonnées SQL Server : `sys.columns` / commentaires sur `TSTOCK.STOCK*`.  
2. Échantillon de procédures stockées Beach liées à `IDMVT` / `IDM` (SELECT only).  
3. Corrélation location ↔ index `STOCKn` via données historiques réelles.  
4. Comparaison montants ERP `erp_tarifs` vs lignes `TPRESTATION` importées.
