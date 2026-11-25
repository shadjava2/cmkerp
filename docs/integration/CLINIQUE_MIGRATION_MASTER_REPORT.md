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
| 5 | Pharmacies, Produits, Stock, Médicaments | 7+5177+4649+2000* | Non |
| 6 | Prestations | **2000*** / 980890 | Non (import seul) |

\* Volumes historiques massifs : preuve **sélective** opérationnelle (fenêtrage + recherche UI). Pas de dump intégral forcé.

## Lots 5–6 — modes

| Entité | Mode | ERP |
| --- | --- | --- |
| STOCK | Lecture seule | 4649 |
| MEDICAMENTS | Import seul | 2000 |
| PRESTATIONS | Import seul | 2000 |

## Clôture officielle Lots 0–1

Bloquée tant que `CLINIQUE_ISOLATED_SQLSERVER_RECIPE.md` n’est pas exécutée.

## Prochaine étape

Finalisation : **dashboard financier** (prestations / médicaments / société-contrat-service-site, évolution temporelle) — sans élargir `WRITE_ENTITIES`.
