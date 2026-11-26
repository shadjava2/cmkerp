# Rapport — Récapitulatif premium & prévention des doublons patients

Date : 2026-11-26

## Architecture du moteur anti-doublons

| Couche | Composant | Rôle |
| --- | --- | --- |
| Similarité | `PatientIdentityMatcher` | Normalisation, permutations, token sort/set, Levenshtein, Jaro-Winkler, scoring multicritère |
| Détection | `PatientDuplicateDetectionService` | Phase 1 candidats Mediline (+ inscriptions ERP) · Phase 2 scoring |
| API | `POST /api/v1/dmi/registrations/duplicates` · `/preflight` | Analyse + contrôles admin |
| Garde création | `PatientRegistrationOrchestrator` | Vérification fingerprint + décision avant écriture Mediline |
| Audit | `erp_patient_duplicate_decision` (V38) | Décision, justification, candidats, utilisateur |
| UI | `PatientRegistrationConfirmStep` | Récap métier, doublons, contrôles, comparaison |

## Algorithmes retenus

- Normalisation NFD (accents), espaces, apostrophes, tirets.
- Permutations nom / post-nom / prénom (inversions).
- Token Sort Ratio, Token Set Ratio (Jaccard/coverage).
- Distance de Levenshtein (formes compactes, max 64).
- Similarité Jaro-Winkler.
- Contenance courte plafonnée (anti faux positifs noms fréquents).
- Date de naissance : identité, inversion jour/mois plausible, ±1 an.
- Téléphone normalisé (9 derniers chiffres) = indice complémentaire uniquement.

## Pondération initiale

| Critère | Poids |
| --- | --- |
| Identité complète | 50 % |
| Date de naissance | 25 % |
| Téléphone | 10 % |
| Sexe | 5 % |
| Lieu de naissance | 5 % |
| E-mail | 5 % |

Les critères absents d’un côté sont exclus (renormalisation). Une information absente n’est jamais considérée comme identique.

## Seuils

| Score | Bande | Effet UI / serveur |
| --- | --- | --- |
| ≥ 95 | IDENTICAL_LIKELY | Blocage + confirmation justifiée (`DMI_PATIENT_DUPLICATE_OVERRIDE`) |
| ≥ 85 | VERY_PROBABLE | Blocage + confirmation justifiée |
| ≥ 70 | POSSIBLE | Présentation des candidats |
| &lt; 70 | WEAK | Non bloquant |

## Performances

- Phase 1 limitée (~40 lignes / stratégie, pool ≤ 120).
- Scoring uniquement sur le pool.
- Affichage ≤ 12 candidats, sans masquer les scores ≥ 85.
- Tests unitaires matcher : 10/10 en ~0,1 s (hors I/O Mediline).

## Faux positifs / négatifs (calibration)

- Téléphone familial partagé : pénalité si identité &lt; 60 (test unitaire).
- Contenance courte sur noms fréquents : plafonnée.
- Homonymes / jumeaux : non fusionnés ; décision utilisateur + justification.
- Calibration terrain CMK encore à mener sur base de test isolée (seuils provisoires).

## Corrections du récapitulatif

- Sexe : Masculin / Féminin.
- Contrat : désignation métier.
- Relation : Assuré / Titulaire, Conjoint, etc.
- PEC, catégorie, compte Mediline, location, nationalité, assurance, COVID.
- Boutons Modifier (identité / prise en charge).
- Indicateur d’étapes + footer stable.

## Cause exacte de « Catégorie ou compte inconnu »

`MedilinePatientWriteService.validateReference` (et équivalent Cardex) levait un message unique lorsque :

1. le code était absent de `t_categorie_patient` / `t_compte`, ou
2. la catégorie valait `-1`.

Cause métier typique en création DMI : mapping société→compte manquant, ou code catégorie contrat non présent / invalide en Mediline.

**Correction** : messages distincts (catégorie introuvable, code `-1`, compte introuvable) + erreurs `AffiliationCoverageService` préfixées (`SOCIETE_MEDILINE_UNMAPPED`, `CATEGORIE_*`, `COMPTE_*`, `MEDILINE_UNAVAILABLE`).

## RBAC

- `DMI_PATIENT_DUPLICATE_DETECT`
- `DMI_PATIENT_DUPLICATE_COMPARE`
- `DMI_PATIENT_DUPLICATE_USE_EXISTING`
- `DMI_PATIENT_DUPLICATE_OVERRIDE`
- `DMI_PATIENT_DUPLICATE_AUDIT_VIEW`

## Tests exécutés

- `PatientIdentityMatcherTest` — 10 PASS
- `npx tsc --noEmit` — PASS
- `npm run build` — PASS
- `mvn package` shared-kernel + gateway — PASS (après libération du JAR)
- `rbac-compliance-check.sh` — PASS
- `clinic-legacy-compliance-check.sh` — FAIL préexistant (`ClinicLegacyWriteGuard non référencé dans facturation gateway` vs présence confirmée ailleurs) — hors périmètre de ce chantier

## Standard permanent

- `.cursor/rules/patient-identity-integrity.mdc` (`alwaysApply`)
- Entrée dans `AGENTS.md`
- Inventaire RBAC mis à jour

## Blocages / suites

1. Attribuer `DMI_PATIENT_DUPLICATE_*` aux rôles métier (ADMIN sync auto via catalogue).
2. Appliquer Flyway V38 (ou exécuter le SQL) sur la base gateway cible.
3. Recette E2E sur **base de test isolée** uniquement (identités fictives).
4. Calibrer seuils/pondérations sur cas CMK représentatifs.
5. Redémarrer le gateway avec le JAR fraîchement construit.
