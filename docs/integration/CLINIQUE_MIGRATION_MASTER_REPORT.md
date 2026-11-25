# CLINIQUE Migration — Rapport maître (consolidation pré-UAT)

Date : **2026-11-26**  
Base MySQL : **`cmkerp-v24prod`**  
Écritures Beach : **`WRITE_ENTITIES=TCONTRAT` uniquement**  
CH / Mediline : **aucune écriture**

Ce document remplace les synthèses antérieures. Sprint architectural **clôturé** côté construction ; phase = **recette fonctionnelle + alimentation historique progressive**.

---

## 1. Lots 0–6

| Lot | État | Notes |
| --- | --- | --- |
| 0 Infrastructure | Implémenté | Recette SQL Server isolée encore ouverte |
| 1 Contrats | Opérationnel live | Réplication Beach démontrée |
| 2 Services / Imputations / Locations | Prêt UAT | 43 / 49 / 3 |
| 3 Sociétés / Classes / Tarifs | Prêt UAT | 484 / 24 / 1977 |
| 4 Acteurs / Mediline | Prêt UAT + rapprochement | **29 100 LINKED** / 851 CONFLICT / 3 REVIEW / 458 UNLINKED |
| 5 Pharmacie / stock | Partiel → alimenté | 7 map pharma · 5177 map prod · **7951** lignes stock compartiments |
| 6 Prestations | Formulaire + import histo | **~12 500** tx importées (échantillon progressif ; Beach ~980k) |
| Dashboard | Alimenté | Couverture prestations **2007-11-06 → 2029-12-27** (dates historiques brutes) |

---

## 2. Volumes MySQL live (2026-11-26)

| Indicateur | Volume |
| --- | ---: |
| Ayants droit | 30 412 |
| Ayants LINKED Mediline | **29 100** |
| Prestations ERP (dont import histo) | **12 505** (montant Σ ≈ 318 476) |
| Médicaments ERP (dont import histo) | **11 500** (montant Σ ≈ 128 572) |
| Stock clinique (compartiments) | **7 951** |
| Beach TPRESTATION (source) | ≈ 980 890 |
| Beach TMEDICAMENT (source) | ≈ 453 475 |
| Beach TSTOCK (source) | 4 649 |

Imports historiques : **lecture SQL Server + écriture MySQL**, **aucun outbox**, **aucun recalcul de montants**.

Deux canaux d’alimentation MySQL typée :

1. API progressive `…/history/*/import` (lots par période)
2. **ErpImportDialog** sélectif — `importSelected` miroite désormais `PRESTATIONS` / `MEDICAMENTS` / `STOCK` vers `erp_facturation_*` et `erp_stock_clinique` (auparavant ignorés par `supports()`)

Poursuite possible par lots via API :

- `POST /api/v1/facturation/history/prestations/import`
- `POST /api/v1/facturation/history/medicaments/import`
- `POST /api/v1/facturation/history/stock/import`

---

## 3. Rapprochement Mediline

Stratégie : index Mediline (119 426 IPP) chargé en staging MySQL, puis association **matricule = IPP** (et canal `ANCIEN_DOSSIER`).

| Statut | Traitement |
| --- | --- |
| LINKED | IPP exact unique, identité non contradictoire |
| REVIEW | Plusieurs IPP possibles |
| CONFLICT | IPP unique mais identité contradictoire |
| UNLINKED | Aucune correspondance |

UI exceptions : `/facturation/rapprochement-patients`  
API : `POST /patients/match/run`, `GET /patients/match/exceptions`, `PUT /patients/match/{id}`

---

## 4. Stock STOCK1…STOCK20

Compartiments conservés tels quels dans `erp_stock_clinique`.  
Table `erp_stock_compartment_mappings` (V34) : 20 compartiments **UNMAPPED** par défaut.  
UI : `/facturation/stock-compartiments` — **aucune attribution automatique inventée**.

---

## 5. Adaptateurs sortants

Voir `CLINIQUE_OUTBOUND_ADAPTERS_INVENTORY.md`.  
Seul **TCONTRAT** est exécuté par le worker en production.

---

## 6. Verdict release

| Périmètre | Décision |
| --- | --- |
| Architecture relationnelle | GO recette |
| Référentiels / mappings | GO recette |
| Rapprochement Mediline | GO contrôle (29 100 liés) |
| Dashboard | GO contrôle (données partielles progressives) |
| Prestations / médicaments | Recette contrôlée + import progressif |
| Extension WRITE Beach | **NO-GO** |
| Remplacement CLINIQUE | **NO-GO** |

---

## 7. Commandes de mise à jour

```powershell
cd C:\Users\HP\Videos\STSDEV\cmkerp
git pull
# Arrêter le gateway si JAR verrouillé
mvn -pl cmkerp-shared-kernel,cmkerp-gateway -am -DskipTests package
powershell -File .\run-gateway.ps1 -Background -SkipBuild -KillPort

cd C:\Users\HP\Videos\STSDEV\consolenotification
git pull
```

UAT utilisateur : `CLINIQUE_FINAL_USER_ACCEPTANCE.md` (score personnel **0/19** — à remplir par vous).
