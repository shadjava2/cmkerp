# CLINIQUE Migration — Rapport maître

Date : **2026-11-25**  
Périmètre : coexistence MySQL `erp_*` / `erp_clinic_referentials` ↔ CLINIQUE Beach (SQL Server).  
Écritures Beach : **allow-list** `WRITE_ENTITIES=TCONTRAT` uniquement. CH : **aucune écriture**.

## Synthèse exécutive

| Lot | Thème | Implémenté | Testé | Importé | Validé recette | Autorisé prod écriture Beach |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | Infrastructure / sécurité / outbox | Oui | Partiel | N/A | **Non** (recette isolée manquante) | N/A |
| 1 | Contrats TCONTRAT | Oui | Live partiel (TCC SYNCED) | Oui | **Non** | TCONTRAT seulement |
| 2 | Services, Imputations, Locations | Oui | API live + unit tests | **Oui** 43+49+3 | N/A (import MySQL) | **Non** — lecture/import seulement |
| 3–6 | Suite référentiels | Préparé (moteur commun) | Non | Non | Non | Non |

**Lots 0 et 1 ne sont pas officiellement clos** tant que la recette SQL Server isolée n’est pas exécutée (`docs/integration/CLINIQUE_ISOLATED_SQLSERVER_RECIPE.md`).

## Frontière transactionnelle

| Règle | État |
| --- | --- |
| Métier MySQL + outbox même TX `REQUIRED` | Validé architecture |
| `REQUIRES_NEW` worker uniquement | Validé architecture |
| Flush Beach afterCommit | Validé architecture |
| Document | `CLINIQUE_OUTBOX_TRANSACTION_BOUNDARY.md` |

## Lot 2 — Services / Imputations / Locations

| Capacité | État |
| --- | --- |
| Moteur commun `erp_clinic_referentials` | Implémenté (pas de tables `erp_services` dupliquées) |
| Formulaires Facturation (sidebar) | Implémenté |
| Import sélectif `ErpImportDialog` | Implémenté |
| Locations ↔ site ERP (`siteId` overlay, ComboBox) | Implémenté |
| Centre sync compteurs (ERP / CLINIQUE / seuls / à jour / divergences / file) | Implémenté |
| RBAC branches + `ApiPermissionCatalog` + `…/erp-sites` | Implémenté |
| Écriture Beach TSERVICE / TIMPUTATION / TLOCATION | **Interdite** (hors allow-list → outbox `SUSPENDED`) |
| DDL SQL Server | Aucun |

Volumes Beach → ERP (preuve live 2026-11-25) :

| Entité | CLINIQUE | ERP après import | Mode sync sortant |
| --- | --- | --- | --- |
| SERVICES | 43 | 43 (`IMPORTED`) | `CLINIQUE_TO_ERP` (hors allow-list) |
| IMPUTATIONS | 49 | 49 | idem |
| LOCATIONS | 3 | 3 | idem + association `siteId` ERP |

Réimport : `available=0` (idempotent). Centre sync : `matched=43/49/3`, `clinicOnly=0`.

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
# Arrêter l’ancien process Java du gateway, puis :
Get-Content .env | ForEach-Object {
  $line = $_.Trim(); if (-not $line -or $line.StartsWith('#')) { return }
  $i = $line.IndexOf('='); if ($i -lt 1) { return }
  $k = $line.Substring(0,$i).Trim(); $v = $line.Substring($i+1).Trim().Trim('"').Trim("'")
  Set-Item "Env:$k" $v
}
$env:CMK_SERVER_PORT = "8999"
& "$env:JAVA_HOME\bin\java.exe" -jar "cmkerp-gateway\target\cmkerp-gateway-4.1.1.jar"

# Import Lot 2 (après redémarrage) :
powershell -File scripts\lot2-import-live.ps1

cd c:\Users\HP\Videos\STSDEV\consolenotification
git pull
# npm run dev déjà actif → HMR
```

## Prochaine étape

Lot 3 : Sociétés, Classes, Tarifs — même moteur, **sans** étendre `WRITE_ENTITIES` sans accord explicite.
