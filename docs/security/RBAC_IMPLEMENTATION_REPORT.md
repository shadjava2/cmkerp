# Rapport d’implémentation RBAC — CMK ERP Phase 2

Date : **2026-11-25**  
Statut : **Socle + enforcement enterprise livrés** — couverture fine des boutons individuels en poursuite continue ; critères 100 % absolus documentés dans la matrice.

## Livrables Phase 2

| Livrable | Emplacement |
| --- | --- |
| Migration V24 | `cmkerp-*/src/main/resources/db/migration/V24__rbac_hierarchical_standard.sql` |
| Migration V25 | `V25__rbac_phase2_full_catalog.sql` (catalogue granulaire + FK CASCADE) |
| Initializer | `gateway/.../RbacSchemaInitializer.java` (seed + CASCADE si Flyway off) |
| Moteur live | `PermissionAuthorizationService` + `PermissionAuthorizationPort` |
| Filtre deny-by-default | `RbacEnforcementFilter` + `ApiPermissionCatalog` |
| Annotation | `@RequirePermission` + `RequirePermissionAspect` |
| Protection ADMIN | Domain service, triggers, elevation bloquée, endpoints integrity/repair |
| Audit rôles | `AuditEvent` → `audit_events` + `GET /api/v1/roles/{id}/permission-audit` |
| UI arbre + historique | `RolesPermissionsPage.tsx` |
| Révocation session | invalidation cache + `GET /auth/permissions` + `PermissionsDeniedListener` |
| CI | `scripts/rbac-compliance-check.sh` (bloquant) |
| Matrice | `docs/security/RBAC_COVERAGE_MATRIX.md` |

## Hardcodes éliminés

| Zone | Avant | Après |
| --- | --- | --- |
| `GmaoSchemaInitializer` | Attribution nominative Schadrack | Seed permissions catalogue / rôles |
| `AppUsageEventService` | Filtre username schadrack | Permission `ADMIN_USAGE_VIEW` |
| `MedilinePatientController` | Bypass utilisateur | `PermissionAuthorizationPort` |
| Frontends | Listes statiques / noms | `hasPermission` / zones / menus |

## Nouvelles permissions (extrait V25 / seed)

`STOCK_*` (produits, approv, transferts, inventaires, ventes, reports…), `PILOTAGE_*`, `GMAO_*`, `ADMIN_USAGE_VIEW`, alias `OUVRIR_MODULE_GMAO`, plus catalogue Phase 1 `MODULE_*` / `SIDEBAR_*` / `DMI_*` / `ADMIN_*`.

## Tests exécutés (réels)

| Suite | Résultat |
| --- | --- |
| `RoleDomainServiceAdminProtectionTest` (6) | PASS |
| `ApiPermissionCatalogTest` | PASS |
| `mvn install` shared-kernel, platform, stocks, gateway (filtres tests ci-dessus) | BUILD SUCCESS |
| `rbac-compliance-check.sh` | PASSED |

## Anomalies / écarts résiduels

1. Couverture `PermissionGate` bouton-par-bouton sur wizards stock/DMI encore partielle (API refuse déjà).
2. E2E Playwright révocation 2 sessions : à valider en staging.
3. Triggers MySQL : validés par design + CASCADE ; recette SQL directe recommandée avant prod.
4. Compat `OUVRIR_*` maintenue volontairement.

## Commandes environnement

```bash
# Intégrité ADMIN
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/rbac/admin/integrity

# Réparation ADMIN
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/rbac/admin/repair-permissions

# Permissions session (révocation live)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/auth/permissions

# Historique permissions d’un rôle
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/roles/{id}/permission-audit?limit=30"

# Conformité CI locale
bash cmkerp/scripts/rbac-compliance-check.sh
```

## Déploiement (sans autorisation prod)

1. Appliquer V24/V25 (Flyway) **ou** démarrer le gateway (initializer).
2. Vérifier `GET /api/v1/rbac/admin/integrity` → `healthy: true`.
3. Composer les rôles métier dans `/administration/roles`.
4. Confirmer qu’un 403 déclenche le message de révocation côté console.
