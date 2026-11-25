# CLINIQUE Migration — Rapport d’exécution (Lot 0 / Lot 1)

Date : **2026-11-25 (soir)**  
Périmètre de ce sprint : **moteur TCONTRAT** — pas de clôture Lots 2–6.

## Distinction obligatoire

| Niveau | Signification |
| --- | --- |
| **Implémenté** | Code présent dans le dépôt |
| **Testé** | Preuve unitaire / manuelle exécutée |
| **Validé recette** | Démontré sur SQL Server **isolé** |
| **Autorisé production** | Flag write + autorisation métier |

## État réel des lots (nomenclature officielle)

| Lot | Thème | État |
| --- | --- | --- |
| 0 | Infrastructure / protection / moteur commun | **À consolider** — implémenté, pas validé E2E écriture |
| 1 | Contrats TCONTRAT | **Partiel** — CRUD + import + outbox + inbound + UI rapprochement |
| 2 | Services, Imputations, Locations | **Préparé (moteur générique)** — **hors sprint** jusqu’à validation Lot 0/1 |
| 3 | Sociétés, Classes, Tarifs | Préparé — non validé |
| 4 | Médecins, Ayants droit, Utilisateurs | Préparé — non validé |
| 5 | Pharmacies, Produits, Stock, Médicaments | Préparé — non validé |
| 6 | Prestations | Préparé (import-only) — non validé |

## Checklist sprint (0→12)

| # | Capacité | État précédent | Travail réalisé | Testé | Validé recette | Prod |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Popup bidirectionnel | Partiel (import seul) | `ErpSynchronizationDialog` 3 onglets | UI code | Non | Non |
| 2 | Sync entrante automatique | Confondue avec import | `ClinicContratInboundWorker` + empreinte | Unitaire empreinte | Non (pas d’instance isolée) | Non |
| 3 | Rattrapage intentions | Incluait TES | `exclude-codes=TES` + prepare | Logique code | Non | Non |
| 4 | Worker Outbox | Sans verify | Verify SQL Server avant SYNCED, UNCERTAIN, stuck, backoff | Code | Non | Non |
| 5 | Reprise après incident | Partiel | reclaim IN_PROGRESS → UNCERTAIN, requeue FAILED | Code | Non | Non |
| 6 | Résultats incertains | Absent | `markOutboxUncertain` + reconciliation | Code | Non | Non |
| 7 | Conflits | Partiel | snapshot + resolve KEEP_ERP/KEEP_CLINIQUE | Code | Non | Non |
| 8 | Historique | Absent | GET `/clinic-sync/history` + onglet | Code | Non | Non |
| 9 | Configuration | Absent | GET `/clinic-sync/config` (desired/server/effective) | Code | Non | Non |
| 10 | Tests SQL Server isolés | Absent | **BLOQUÉ** — pas d’instance de test dédiée | — | Non | — |
| 11 | RBAC / CLINIQUE compliance | OK | Nouveaux endpoints catalogués | PASSED scripts | — | — |
| 12 | Rapport + commit/push | — | Ce document | — | — | — |

## Import sélectif ≠ sync entrante

- **Import sélectif** : manuel, candidats, validation explicite.
- **Sync entrante auto** : comparaison périodique CODE + empreinte `SHA-256(CODE|DESIGNATION)` ; MAJ ERP sans outbox retour (anti-boucle) ; nouveaux codes détectés (compteur) ; conflits ERP bloqués.

## Protections écriture

```
CLINIC_BEACH_WRITE_ENABLED=false          # défaut / local actuel
CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT      # allow-list quand write=true
CLINIC_BEACH_INBOUND_ENABLED=true
CLINIC_BEACH_SYNC_ENABLED=true
CLINIC_BEACH_EXCLUDE_CODES=TES
CLINIC_CH_WRITE_ENABLED=false
CLINIC_BEACH_DELETE_ENABLED=false
```

L’UI **ne peut pas** activer les écritures. ADMIN ne contourne pas le WriteGuard.

## Blocage recette écriture

**Pas d’instance SQL Server isolée** disponible dans cet environnement.  
Les DML de test **n’ont pas** été rejoués sur une copie dédiée dans ce sprint.  
→ Ne pas déclarer « worker écriture PASS ».  
→ Prêt pour activation limitée TCONTRAT **après** votre autorisation + cible de recette isolée.

Note : un INSERT `TES` avait été effectué antérieurement sur Beach ; `TES` est désormais exclu du rattrapage automatique.

## Fichiers principaux

Backend : `ClinicContratSyncEngine`, `ClinicContratInboundWorker`, `ClinicBeachOutboxWorker`, `ClinicLegacySyncProperties/WriteGuard`, `FacturationContratController`, `ApiPermissionCatalog`.  
Frontend : `ErpSynchronizationDialog`, `FacturationClinicSyncPage` (Historique + Configuration).

## Commandes de mise à jour

```powershell
cd c:\Users\HP\Videos\STSDEV\cmkerp
git pull
# STS : Maven Update shared-kernel → platform → gateway, redémarrer GatewayApplication
# Vérifier .env : WRITE_ENABLED=false ; INBOUND_ENABLED=true ; EXCLUDE_CODES=TES

cd c:\Users\HP\Videos\STSDEV\consolenotification
git pull
```

## Verdict

**Lot 0 / Lot 1 : NON CLOS.**  
Moteur TCONTRAT **implémenté** et **partiellement testé** (empreinte, compliance).  
**Manque** : validation E2E sur SQL Server isolé + autorisation production limitée TCONTRAT.  
**Lots 2+** : ne pas démarrer la recette métier tant que ce moteur n’est pas démontré.
