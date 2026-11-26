# Chantier 01 — Ayants droit (rapport ciblé)

Date : 2026-11-26  
Périmètre : formulaire métier + persistance `erp_ayants_droit` + intention Outbox `TAYANTDROIT`.

## Formulaire

- Page dédiée : `FacturationAyantsDroitPage.tsx` (remplace le formulaire générique).
- Étape 1 : `PatientPicker` (recherche Mediline / cardex) — pas de création patient.
- Étape 2 : carte identité + affiliation (société, contrat, relation, titulaire, location, source, assurance, COVID).
- Liste paginée serveur avec filtres (statut, liaison Mediline, sync).

## MySQL

Table réutilisée : `erp_ayants_droit` (+ V35 `titulaire_ipp`).

## Mapping TAYANTDROIT (14 colonnes)

| Beach | Origine ERP |
| --- | --- |
| MATRICULE | IPP ≤10 ou matricule historique compatible (sinon 422) |
| NOM | Identité Mediline |
| DATENAIS | Mediline |
| SEXE | Mediline → M/F |
| NATIONALITE | C/E |
| RELATION | A/C/E/P/O (codes cliniques observés) |
| CODEFAM | Matricule titulaire (A → soi-même) |
| CODESOC / CONTRAT | Codes ERP société / contrat |
| DATSAIS | Serveur |
| SOURCE | CM (valeur dominante Beach) |
| LOCATION | Code location ERP |
| ASSURANCE / COVID | O/N |

## Outbox

- Création/MAJ : `erp_ayants_droit` + `erp_sync_outbox` (même TX).
- Statut : `SUSPENDED` tant que `WRITE_ENTITIES` n’inclut pas `TAYANTDROIT`.
- Worker : branche `TAYANTDROIT` + relecture Beach avant `SYNCED`.
- **Activation recette contrôlée (non active)** : `CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT,TAYANTDROIT`

## RBAC

Endpoints `/api/v1/facturation/ayants/**` dans `ApiPermissionCatalog` — permissions `FACTURATION_AYANTSDROIT_*` existantes.

## Blocages

- IPP Mediline > 10 caractères sans matricule historique → création bloquée (pas de troncature).
- Écriture Beach non activée → intention `SUSPENDED` (pas SYNCED).
- Sync inverse : import sélectif existant via `ErpImportDialog` / référentiels.

## Commandes

```powershell
cd C:\Users\HP\Videos\STSDEV\cmkerp
git pull
mvn -pl cmkerp-shared-kernel,cmkerp-gateway -am package -DskipTests
.\run-gateway.ps1 -SkipBuild -Background -KillPort

cd C:\Users\HP\Videos\STSDEV\consolenotification
git pull
npx tsc --noEmit
npm run build
npm run dev
```
