# Recette coexistence — jalon TCONTRAT

| Critère | Résultat |
| --- | --- |
| Règle `clinic-legacy-protection.mdc` | Présente |
| Flags write/sync/delete/CH défaut false | Oui (`application-dev.yml`) |
| Tables `erp_contrats` + `erp_sync_outbox` | Migration V30 |
| Import Dry Run + Import MySQL | API + UI |
| CRUD ERP contrats | UI Contrats |
| `actif` non répliqué | `setActif` sans outbox |
| Catalogue API facturation | `ApiPermissionCatalog` |
| Permissions CONTRATS_* / SYNC_* | V30 + seed initializer |
| Écriture Beach production | **Non activée** |
| DDL Beach | Aucun |

Prochaine preuve : Services / Imputations / Locations (Lot 1 suite).
