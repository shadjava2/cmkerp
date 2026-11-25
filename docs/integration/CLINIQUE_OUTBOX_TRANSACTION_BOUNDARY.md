# Frontière transactionnelle Outbox CLINIQUE

## Règle non négociable

| Étape | Transaction | Propagation |
| --- | --- | --- |
| INSERT/UPDATE fiche ERP MySQL | TX métier | `REQUIRED` (défaut `@Transactional`) |
| INSERT intention `erp_sync_outbox` | **même TX métier** | join |
| Commit MySQL | — | atomique métier + intention |
| Écriture SQL Server Beach | hors TX MySQL | datasource distincte |
| Claim / SYNCED / FAILED / UNCERTAIN | TX worker | `REQUIRES_NEW` |
| Flush worker après create/update/rapprocher | **après** `afterCommit` | pas dans la TX initiale |

## Conséquence

Si la création ERP échoue ou est annulée, **aucune** intention outbox ne subsiste.

`REQUIRES_NEW` ne doit **pas** être utilisé pour l’enregistrement initial de l’intention.

## Preuve live (2026-11-25)

- Contrat `TCC` : outbox `SYNCED`, `attempt_count=1`, présent dans Beach `TCONTRAT` (`TEST2`).
- Cause du bug « PENDING perpétuel » : mises à jour de statut worker annulées par rollback de TX ambiante → corrigé via `REQUIRES_NEW` **uniquement** côté worker.
