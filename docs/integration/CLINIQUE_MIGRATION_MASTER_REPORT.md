# CLINIQUE Migration — Rapport maître

Date : **2026-11-25**  
Écritures Beach : **`WRITE_ENTITIES=TCONTRAT` uniquement**. CH : aucune écriture.

## Synthèse

| Lot | Thème | Importé | Écriture Beach |
| --- | --- | --- | --- |
| 0–1 | Infra / Contrats | Oui (TCC) | TCONTRAT — **recette isolée restante** |
| 2 | Services, Imputations, Locations | 43+49+3 | Non |
| 3 | Sociétés, Classes, Tarifs | 484+24+1977 | Non |
| 4 | Médecins, Ayants droit, Utilisateurs | 140+30412+18 | Non |
| 5 | Pharmacies, Produits, Stock, Médicaments | **7+5177+4649+2000*** | Non |
| 6 | Prestations | À faire | Non (import seul) |

\* Médicaments : **preuve sélective** 2000 / 453475 mouvements historiques. Import complet volontairement non forcé — UI recherche + fenêtrage.

## Lot 5 — détail

| Entité | Mode | CLINIQUE | ERP |
| --- | --- | --- | --- |
| PHARMACIES | CLINIQUE→ERP | 7 | 7 |
| PRODUITS | CLINIQUE→ERP | 5177 | 5177 |
| STOCK | Lecture seule | 4649 | 4649 |
| MEDICAMENTS | Import seul | 453475 | 2000 (sélectif) |

Formulaire Produits Facturation branché sur le référentiel CLINIQUE (plus le catalogue stock ERP). Route `/facturation/mouvements-medicaments` pour les mouvements TMEDICAMENT.

## Prochaine étape

Lot 6 : Prestations (import sélectif seul) — puis dashboard financier.
