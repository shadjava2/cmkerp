# Protection CLINIQUE Beach / CH

## Politique

Les bases SQL Server CLINIQUE sont des systèmes historiques de production. Le schéma est **immuable** pour CMK ERP.

| Opération | MySQL ERP | SQL Server CLINIQUE |
| --- | --- | --- |
| SELECT | Oui | Périmètre approuvé |
| INSERT/UPDATE | Oui | Réplication contrôlée + flags |
| DELETE | Contrôlé | Interdit par défaut |
| DDL | Migrations versionnées | **Interdit** |

## Feature flags (défaut sûr)

```yaml
clinic.beach.sync-enabled: false
clinic.beach.write-enabled: false
clinic.beach.delete-enabled: false
clinic.ch.write-enabled: false
```

Variables d’environnement : `CLINIC_BEACH_SYNC_ENABLED`, `CLINIC_BEACH_WRITE_ENABLED`, `CLINIC_BEACH_DELETE_ENABLED`, `CLINIC_CH_WRITE_ENABLED`.

## Protections techniques

1. DataSources séparés (`cliniqueJdbcTemplate`, `cliniqueChJdbcTemplate`).
2. `ClinicLegacyWriteGuard` — refus applicatif si flags off / CH.
3. Aucun Flyway sur DataSource CLINIQUE.
4. Outbox MySQL `erp_sync_outbox` avant toute réplication.
5. Règle Cursor `clinic-legacy-protection.mdc` (alwaysApply).
6. Script `scripts/clinic-legacy-compliance-check.sh`.

## Secrets

Ne jamais importer `TUTILISATEUR.MOTDEPASSE`. Colonnes explicites uniquement. Pas de `SELECT *` sur utilisateurs historiques.
