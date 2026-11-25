# CLINIQUE Migration — Rapport maître (CLOTURE)

Date : **2026-11-25**  
Statut global : **CLOS — lots 0 à 6 livrés sur le moteur centralisé**

## Décision d’autorisation (2026-11-25)

| Flag | Valeur | Effet |
| --- | --- | --- |
| `CLINIC_BEACH_SYNC_ENABLED` | `true` | Worker outbox actif |
| `CLINIC_BEACH_WRITE_ENABLED` | `true` | INSERT / UPDATE Beach autorisés |
| `CLINIC_BEACH_DELETE_ENABLED` | `false` | DELETE interdit |
| `CLINIC_CH_WRITE_ENABLED` | `false` | CLINIQUE CH / Mediline : aucune écriture |

`actif` reste exclusivement ERP. Aucun DDL SQL Server. Mot de passe `TUTILISATEUR` jamais lu.

## Preuve TCONTRAT

| Contrôle | Résultat |
| --- | --- |
| Contrat ERP `TES` | Présent (`OPERATION_ERP`) |
| Intention outbox | Journalisée puis **SYNCED** |
| INSERT Beach `TCONTRAT (CODE, DESIGNATION)` | **Réussi** — `TES` présent dans CLINIQUE |
| Écritures CH | Toujours interdites |

## Synthèse des lots

| Lot | Périmètre | État |
| --- | --- | --- |
| 0 | Flags, outbox, WriteGuard, rules | **CLOS** |
| 1 | TCONTRAT / `erp_contrats` | **CLOS** |
| 2 | Services, Imputations, Locations | **CLOS** |
| 3 | Sociétés, Classes, Tarifs | **CLOS** |
| 4 | Médecins, Ayants droit, Utilisateurs | **CLOS** |
| 5 | Pharmacies, Produits (stock UI), Stock projection | **CLOS** |
| 6 | Prestations, Médicaments (mouvements) import-only | **CLOS** |

## Matrice par entité

| Entité | MySQL | Formulaire | Import sélectif | Outbound Beach | Notes |
| --- | --- | --- | --- | --- | --- |
| Contrats | `erp_contrats` | Oui | Oui | INSERT/UPDATE CODE+DESIGNATION | Validé |
| Services | `erp_clinic_referentials` | Oui | Oui | CODE+LIBELLE | Moteur générique |
| Imputations | idem | Oui | Oui | CODE+DESIGNATION | |
| Locations | idem | Oui | Oui | CODE+LIBELLE+ADRESSE | |
| Sociétés | idem | Oui | Oui | colonnes déclarées | |
| Classes | idem | Oui | Oui | CODE+LIBELLE+SERVICE | |
| Tarifs | idem | Oui | Oui | CODEPRES+montants | |
| Médecins | idem | Oui | Oui | CODE+NOM+… | |
| Ayants droit | idem | Oui | Oui | MATRICULE+… | |
| Utilisateurs | idem | Oui (import/maj) | Oui | UPDATE sans MOTDEPASSE ; pas d’INSERT compte | |
| Pharmacies | idem | Oui | Oui | CODE+LIBELLE | |
| Produits | stock UI + moteur | Stock existant | Moteur prêt | Bidirectionnel gated | |
| Stock | projection | Oui | Oui | **Lecture seule** | |
| Prestations | idem | Oui | Oui (fenêtré) | **Import only** | Volumes élevés |
| Médicaments | idem | Oui | Oui (fenêtré) | **Import only** | Volumes élevés |

## Architecture livrée

- Outbox unique `erp_sync_outbox` (intentions persistées même si writes off → `SUSPENDED`).
- Worker `ClinicBeachOutboxWorker` (TCONTRAT + référentiels catalogue).
- API générique `/api/v1/facturation/referentials/{key}/**`.
- Centre de synchronisation : KPIs métier + file + référentiels + import mutualisé.
- Popup `ErpImportDialog` (bouton Importer toujours visible ; accent corrigé hors shell).
- RBAC : catalogue `ApiPermissionCatalog` + seed V31 + attribution ADMIN.

## Limites assumées (non-bloquantes)

1. Prestations / médicaments : import sélectif paginé (pas de dump mémoire complet).
2. Utilisateurs : pas de création Beach (évite compte sans secret).
3. Produits catalogue facturation : UI stock historique conservée ; sync clinique via API générique.
4. Redémarrage STS obligatoire pour prendre flags + JAR kernel.

## Mise à jour

```powershell
cd c:\Users\HP\Videos\STSDEV\cmkerp
git pull
# Appliquer V31 si besoin (déjà appliqué sur cmkerp-v24prod)
# STS : Maven Update shared-kernel → platform → gateway, redémarrer GatewayApplication
# Vérifier .env : CLINIC_BEACH_SYNC_ENABLED=true et CLINIC_BEACH_WRITE_ENABLED=true

cd c:\Users\HP\Videos\STSDEV\consolenotification
git pull
```

## Verdict

**OK CLOS** — le moteur centralisé de synchronisation CLINIQUE Beach ↔ CMK ERP est opérationnel pour TCONTRAT et industrialisé pour l’ensemble des référentiels des lots 0–6, avec protections DELETE/CH maintenues.
