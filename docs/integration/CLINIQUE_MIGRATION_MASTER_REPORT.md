# CLINIQUE Migration — Rapport maître

Date : **2026-11-25**  
Périmètre : coexistence MySQL `erp_*` ↔ CLINIQUE Beach (SQL Server).  
Écritures Beach / CH : **désactivées** par défaut (`clinic.beach.write-enabled=false`, `clinic.ch.write-enabled=false`).

## Synthèse exécutive

| Lot | Thème | État |
| --- | --- | --- |
| 0 | Socle flags, outbox, protections | **FAIT** |
| 1 | TCONTRAT / `erp_contrats` | **PARTIEL — moteur industrialisé, écritures Beach non autorisées en prod** |
| 2–6 | Autres référentiels / transactions | **NON DÉMARRÉ** (préparé dans le centre de sync comme « Non disponible ») |

**Cause du contrat `TES` absent de CLINIQUE :** création ERP avec écritures Beach off → historiquement `LOCAL_ONLY` **sans** ligne outbox. **Corrigé :** journalisation systématique `SUSPENDED` + action « Préparer les intentions ».

**Worker de réplication :** implémenté (`ClinicBeachOutboxWorker`), chargé seulement si `clinic.beach.sync-enabled=true`, et **n’écrit pas** tant que `write-enabled=false`.

## Lot 0 — Socle

| Élément | État | Preuve |
| --- | --- | --- |
| Feature flags Beach/CH | FAIT | `application-dev.yml` + `ClinicLegacySyncProperties` |
| `ClinicLegacyWriteGuard` | FAIT | platform |
| Table `erp_sync_outbox` | FAIT | V30 |
| Compliance CLINIQUE | FAIT | `clinic-legacy-compliance-check.sh` |
| Docs protection / protocole | FAIT | `docs/integration/CLINIQUE_*.md` |
| Rule `clinic-legacy-protection.mdc` | FAIT | alwaysApply |
| Rule `clinic-bidirectional-sync.mdc` | FAIT | alwaysApply |

## Lot 1 — TCONTRAT

| Capacité | État | Notes |
| --- | --- | --- |
| Migration MySQL `erp_contrats` | FAIT | V30 |
| Formulaire Gestion des contrats | FAIT | UI production |
| API CRUD | FAIT | `/api/v1/facturation/contrats` |
| Import sélectif CLINIQUE → ERP | FAIT | candidates + selected + `ErpImportDialog` |
| Synchronisation entrante | FAIT | import MySQL only |
| Intentions ERP → CLINIQUE (outbox) | FAIT | `PENDING` / `SUSPENDED` même si writes off |
| Worker réplication sortante | PRÉPARÉ | Code présent ; inactif tant que flags off |
| Rapprochement manuel bidirectionnel (popup 3 onglets ERP↔CLINIQUE) | PARTIEL | Import sélectif + onglet divergences ; push ERP→CLINIQUE via file / prepare |
| Reprise auto / backoff | PRÉPARÉ | `next_attempt_at`, FAILED retry formula ; non démontré en écriture réelle |
| Centre de synchronisation | FAIT (v1 industrielle) | KPIs métier + outbox + référentiels + onglets |
| RBAC | FAIT | catalogue + permissions FACTURATION_* |
| Tests isolation + écriture Beach | BLOQUÉ | Attente environnement / autorisation writes |
| Activation production TCONTRAT | BLOQUÉ | Autorisation explicite requise |

### Checklist clôture TCONTRAT (0→11)

| # | Tâche | État |
| --- | --- | --- |
| 1 | Auditer travaux antérieurs | FAIT (ce rapport) |
| 2 | Popup import sélectif | FAIT |
| 3 | Génération événements Outbox | FAIT (correction SUSPENDED) |
| 4 | Réplication ERP → CLINIQUE | PRÉPARÉ (worker, flags off) |
| 5 | Rapprochement CLINIQUE → ERP | FAIT (import sélectif) |
| 6 | Reprise auto après incident | PRÉPARÉ |
| 7 | Rattrapage manuel bidirectionnel | PARTIEL (prepare + import) |
| 8 | Centre de synchronisation | FAIT v1 |
| 9 | Validation environnement isolé + writes | À FAIRE |
| 10 | Autoriser writes TCONTRAT prod | À FAIRE (autorisation) |
| 11 | Poursuivre Services / Imputations / Locations | BLOQUÉ jusqu’à validation moteur |

## Lots 2–6 — Non démarrés

Services, Imputations, Locations, Sociétés, Classes, Tarifs, Médecins, Ayants droit, Utilisateurs, Pharmacies, Produits, Stock, Prestations, Médicaments : **pas** de tables `erp_*` dédiées ni sync dans ce chantier. Affichés « Non disponible » dans le centre.

## Preuves techniques à retenir

1. **DENY BY DEFAULT RBAC** + catalogue `ApiPermissionCatalog` pour facturation/clinic-sync.
2. **Import sélectif** : aucune écriture avant validation ; codes seuls envoyés au backend ; relecture CLINIQUE.
3. **Outbox** : intentions persistées même écritures suspendues.
4. **Worker** : `ClinicBeachOutboxWorker` — DML paramétré `TCONTRAT (CODE, DESIGNATION)` uniquement si write+sync enabled.

## Plan d’exécution suivant

1. Recette isolée : préparer intentions pour `TES` → vérifier file `SUSPENDED`.
2. Sur environnement non-prod autorisé : activer `CLINIC_BEACH_SYNC_ENABLED=true` + `CLINIC_BEACH_WRITE_ENABLED=true` temporairement → vérifier INSERT Beach + statut SYNCED.
3. Désactiver writes ; confirmer nouvelles créations restent `SUSPENDED`.
4. Clôturer checklist TCONTRAT items 9–10.
5. Enchaîner Lot 2 (Services / Imputations / Locations) en réutilisant `ErpImportDialog` + outbox.

## Commandes de mise à jour

```powershell
cd c:\Users\HP\Videos\STSDEV\cmkerp
git pull
# STS : Maven Update Project (shared-kernel, platform, gateway) puis redémarrer GatewayApplication

cd c:\Users\HP\Videos\STSDEV\consolenotification
git pull
```
