# Architecture RBAC CMK ERP

## Principe

Le code déclare et contrôle les permissions nécessaires.  
La base MySQL (`permissions` + `roles_permissions`) est la **source de vérité** des autorisations, administrables à 100 % depuis l'interface (hors rôle système ADMIN).

## Tables

| Table | Rôle |
| --- | --- |
| `permissions` | Catalogue hiérarchique (`parent`, `nom` unique) |
| `roles` | Rôles métier + ADMIN (id=5) |
| `roles_permissions` | Attributions explicites |
| `utilisateurs.fkRole` | Association utilisateur → rôle |

## Hiérarchie

- Racine : `parent = 0` (`MODULE_*`)
- Enfants : `parent = id` du parent (`SIDEBAR_*`, puis actions `DMI_*` / `ADMIN_*`)
- Cascade UI = aide à la sélection ; **pas** d'héritage implicite à l'autorisation

## Moteur backend

- Port : `PermissionAuthorizationPort`
- Implémentation : `PermissionAuthorizationService` (cache + invalidation)
- Déclaratif : `@RequirePermission`
- ADMIN : `SystemRoleConstants.ADMIN_ROLE_ID = 5`
- Triggers MySQL : sync insert permission → ADMIN ; protection delete associations ADMIN
- Réparation : `POST /api/v1/rbac/admin/repair-permissions`

## Frontend

- `usePermissions()` / `PermissionGate`
- Zones portail : `accessZones.ts`
- Menu DMI filtré par `SIDEBAR_*`
- UI admin : `/administration/roles` (arbre 3 états)

## Révocation session active

1. Admin retire une permission de rôle
2. `invalidateRolePermissions(roleId)` purge le cache
3. Prochaine requête API : refus 403 (moteur live)
4. Frontend écoute `cmk:permissions-denied` et affiche le message métier

Les claims JWT ne constituent **pas** une autorité définitive.

## Initialisation legacy

Flyway peut être désactivé (`application-dev.yml`).  
`RbacSchemaInitializer` (Order 55) applique colonnes, catalogue et sync ADMIN au démarrage.
