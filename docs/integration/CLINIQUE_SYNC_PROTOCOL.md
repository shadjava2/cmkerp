# Protocole de synchronisation

## Outbox

1. Transaction MySQL : écriture `erp_*` + insert `erp_sync_outbox` (PENDING).
2. Worker (activé si `clinic.beach.sync-enabled=true`) lit outbox.
3. Garde `ClinicLegacyWriteGuard` + whitelist table/colonnes.
4. DML paramétré SQL Server.
5. Vérification lecture retour / empreinte.
6. Statut SYNCED | FAILED | CONFLICT | UNCERTAIN.

## Idempotence

`operation_id` unique. Ne pas réinsérer si résultat précédent incertain → CONFLICT / rapprochement admin.

## Origines

`IMPORT_CLINIQUE` | `OPERATION_ERP` | `RECONCILIATION` | `MIGRATION`  
Les imports ne génèrent **pas** d’écriture retour.
