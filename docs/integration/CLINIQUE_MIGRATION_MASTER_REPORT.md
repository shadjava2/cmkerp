# CLINIQUE Migration — Rapport maître

Date : **2026-11-25**  
Périmètre : coexistence MySQL `erp_*` / `erp_clinic_referentials` ↔ CLINIQUE Beach (SQL Server).  
Écritures Beach : **allow-list** `WRITE_ENTITIES=TCONTRAT` uniquement. CH : **aucune écriture**.

## Synthèse exécutive

| Lot | Thème | Implémenté | Testé | Importé | Validé recette | Autorisé prod écriture Beach |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | Infrastructure / sécurité / outbox | Oui | Partiel | N/A | **Non** | N/A |
| 1 | Contrats TCONTRAT | Oui | Live partiel (TCC SYNCED) | Oui | **Non** | TCONTRAT seulement |
| 2 | Services, Imputations, Locations | Oui | Live + tests | **Oui** 43+49+3 | N/A | **Non** |
| 3 | Sociétés, Classes, Tarifs | Oui | Live + tests | **Oui** 484+24+1977 | N/A | **Non** |
| 4 | Médecins, Ayants droit, Utilisateurs | Oui | Live + tests | **Oui** 140+30412+18 | N/A | **Non** |
| 5–6 | Suite | Préparé | Non | Non | Non | Non |

**Lots 0 et 1 non clos** sans recette SQL Server isolée (`CLINIQUE_ISOLATED_SQLSERVER_RECIPE.md`).

## Lot 4 — preuve live

| Entité | CLINIQUE | ERP | Notes |
| --- | --- | --- | --- |
| MEDECINS | 140 | 140 | matched 140 |
| AYANTS_DROIT | 30413 | 30412 | 1 doublon de matricule côté Beach ; import fenêtré |
| UTILISATEURS | 18 | 18 | sans `MOTDEPASSE` ; création ERP désactivée |

Import fenêtré pour volumes > 2500 (évite un scan Beach complet à chaque page). Centre sync : rapprochement détaillé sauté au-delà de 2500 lignes. UI import : recherche obligatoire sur gros volumes (`partialCandidates`).

`WRITE_ENTITIES=TCONTRAT` inchangé.

## Configuration sûre

```
CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT
CLINIC_CH_WRITE_ENABLED=false
CLINIC_BEACH_DELETE_ENABLED=false
```

## Prochaine étape

Lot 5 : Pharmacies, Produits, Stock, Médicaments — même moteur, sans étendre les écritures Beach.
