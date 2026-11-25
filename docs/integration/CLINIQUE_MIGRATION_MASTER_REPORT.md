# CLINIQUE Migration — Rapport maître

Date : **2026-11-25**  
Périmètre : coexistence MySQL `erp_*` / `erp_clinic_referentials` ↔ CLINIQUE Beach (SQL Server).  
Écritures Beach : **allow-list** `WRITE_ENTITIES=TCONTRAT` uniquement. CH : **aucune écriture**.

## Synthèse exécutive

| Lot | Thème | Implémenté | Testé | Importé | Validé recette | Autorisé prod écriture Beach |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | Infrastructure / sécurité / outbox | Oui | Partiel | N/A | **Non** (recette isolée manquante) | N/A |
| 1 | Contrats TCONTRAT | Oui | Live partiel (TCC SYNCED) | Oui | **Non** | TCONTRAT seulement |
| 2 | Services, Imputations, Locations | Oui | API live + unit tests | **Oui** 43+49+3 | N/A (import MySQL) | **Non** |
| 3 | Sociétés, Classes, Tarifs | Oui | API live + unit tests | **Oui** 484+24+1977 | N/A (import MySQL) | **Non** |
| 4–6 | Suite référentiels | Préparé (moteur commun) | Non | Non | Non | Non |

**Lots 0 et 1 ne sont pas officiellement clos** tant que la recette SQL Server isolée n’est pas exécutée (`docs/integration/CLINIQUE_ISOLATED_SQLSERVER_RECIPE.md`).

## Frontière transactionnelle

| Règle | État |
| --- | --- |
| Métier MySQL + outbox même TX `REQUIRED` | Validé architecture |
| `REQUIRES_NEW` worker uniquement | Validé architecture |
| Flush Beach afterCommit | Validé architecture |
| Document | `CLINIQUE_OUTBOX_TRANSACTION_BOUNDARY.md` |

## Lot 2 — Services / Imputations / Locations

Volumes Beach → ERP :

| Entité | CLINIQUE | ERP | Mode |
| --- | --- | --- | --- |
| SERVICES | 43 | 43 | `CLINIQUE_TO_ERP` |
| IMPUTATIONS | 49 | 49 | idem |
| LOCATIONS | 3 | 3 | idem + `siteId` ERP |

## Lot 3 — Sociétés / Classes / Tarifs

| Capacité | État |
| --- | --- |
| Formulaires enrichis (champs métier + pagination) | Implémenté |
| Import sélectif multi-pages / lots 500 | Implémenté |
| Centre sync compteurs réels | Implémenté |
| RBAC SOCIETES / CLASSES / TARIFS | Présent |
| Écriture Beach | **Interdite** (hors `WRITE_ENTITIES`) |

Volumes Beach → ERP (preuve live 2026-11-25) :

| Entité | CLINIQUE | ERP après import | matched |
| --- | --- | --- | --- |
| SOCIETES | 484 | 484 | 484 |
| CLASSES | 24 | 24 | 24 |
| TARIFS | 1977 | 1977 | 1977 |

## Configuration production sûre

```
CLINIC_BEACH_SYNC_ENABLED=true
CLINIC_BEACH_WRITE_ENABLED=true
CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT
CLINIC_BEACH_INBOUND_ENABLED=true
CLINIC_BEACH_EXCLUDE_CODES=TES
CLINIC_BEACH_DELETE_ENABLED=false
CLINIC_CH_WRITE_ENABLED=false
```

## Commandes PowerShell — mise à jour

```powershell
cd c:\Users\HP\Videos\STSDEV\cmkerp
git pull
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
mvn -pl cmkerp-shared-kernel,cmkerp-platform,cmkerp-gateway -am clean package -DskipTests
# Redémarrer le JAR gateway sur :8999 avec .env

cd c:\Users\HP\Videos\STSDEV\consolenotification
git pull
```

## Prochaine étape

Lot 4 : Médecins, Ayants droit, Utilisateurs — même moteur, **sans** étendre `WRITE_ENTITIES` sans accord explicite.
