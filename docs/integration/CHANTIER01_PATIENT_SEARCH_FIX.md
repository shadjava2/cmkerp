# Chantier 01 — Correction recherche patient DMI / Facturation

Date : 2026-11-26

## Cause racine

Le `PatientPicker` Facturation envoyait **à la fois** `nom` et `identite` avec la même phrase (`"MPAKA MABI"`).

Le backend applique un `AND` multiterme sur **chaque** champ renseigné :

- `identite` → chaque terme dans `CONCAT_WS(NOM, POSTNOM, PRENOM)` — OK
- `nom` → chaque terme dans `p.NOM` seul — **échoue** si `MABI` n’est pas dans le nom de famille

Le DMI n’envoie que `{ ...EMPTY_SEARCH, identite }` → 3 patients.
Facturation (avant correctif) → 0 patient.

Preuve API live (même JWT ADMIN) :

| Critères | Résultats |
| --- | --- |
| `identite=MPAKA MABI` | 3 |
| `identite=MABI MPAKA` | 3 |
| `nom=MPAKA MABI` + `identite=MPAKA MABI` | 0 |
| `identite=mpaka mabi` | 3 |
| `identite=MPAKA SCHAD` | 1 |

## Correctif

- `buildPatientQuickSearchCriteria` : `identite` multiterme **ou** `nIpp` exact (token unique avec chiffre)
- `PatientPicker` mode SELECTION : même endpoint cardex, panneau avancé DMI, états d’erreur différenciés
- Ayants droit : étape 1 = sélecteur plein écran (plus de double modal)
- RBAC : `POST /api/v1/cardex/patients/recherche` accepte `FACTURATION_AYANTSDROIT_VIEW|CREATE` (sans `MODULE_DMI`)
- Contrôleur : `searchDatabase()` distinct du Cardex médical complet
- Standard permanent : `.cursor/rules/unified-patient-search.mdc`

## Commits

- Backend : `5fc505d` (cmkerp)
- Frontend : `6c6d431` (consolenotification)

## Mise à jour runtime

```powershell
# Frontend déjà rebuildable via Vite
cd consolenotification; npm run build

# Gateway : rebuild shared-kernel puis gateway, redémarrer le process
# (le JAR fat embarque le kernel — DevTools seul insuffisant pour le catalogue)
cd cmkerp
mvn -pl cmkerp-shared-kernel install -DskipTests
mvn -pl cmkerp-gateway -am package -DskipTests
# puis redémarrer le gateway
```

Le correctif **frontend** suffit pour le cas « MPAKA MABI » avec un compte qui a déjà accès cardex/DMI.
Le rebuild gateway est requis pour les comptes Facturation **sans** droits DMI.
