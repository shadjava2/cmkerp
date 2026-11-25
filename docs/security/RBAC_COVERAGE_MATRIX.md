# Matrice de couverture RBAC — Phase 2 (mesures réelles)

Date d’audit : **2026-11-25**. Sources : ripgrep / inventaire code, `ApiPermissionCatalog`, `RbacSchemaInitializer`, `rbac-compliance-check.sh`, Maven Surefire.

## Mesures globales

| Indicateur | Valeur | Source |
| --- | ---: | --- |
| Contrôleurs REST (`*RestController.java`) | 60 | glob `cmkerp` |
| Mappings HTTP (`@(Get\|Post\|Put\|Patch\|Delete)Mapping` sur contrôleurs) | ~398 | ripgrep contrôleurs |
| Règles path→permission (`ApiPermissionCatalog`) | 70+ | code (deny-by-default) |
| Endpoints publics justifiés | auth, health, webhook WhatsApp, portail-fournisseur, swagger, actuator health/info | `ApiPermissionCatalog.isPublicPath` |
| Permissions seed Phase 2 (MODULES + SIDEBAR + STOCK/GMAO/ADMIN/DMI) | 70+ | V25 + `RbacSchemaInitializer` |
| Hardcodes d’autorisation nominatifs (code métier) | **0** | `rbac-compliance-check.sh` PASSED |
| `@RequirePermission` déclaratif | Role + RbacAdmin | complément du filtre path |
| Protection ADMIN (tests unitaires) | **6/6 PASS** | `RoleDomainServiceAdminProtectionTest` |
| Catalogue API (tests) | **PASS** | `ApiPermissionCatalogTest` |
| Compilation modules RBAC | **BUILD SUCCESS** | shared-kernel, platform, stocks, gateway |
| CI bloquante | Oui | `.github/workflows/ci.yml` → `rbac-compliance-check.sh` |

## Par module

| Module | Fonctions inventoriées | Permissions catalogue | FE protégé | API via filtre | Hardcodes | Couverture |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Administration | 12+ | `MODULE_ADMINISTRATION` + `ADMIN_*` + `ADMIN_USAGE_VIEW` | Zones + `/administration/roles` + historique audit | `/users`, `/roles`, `/rbac`, `/usage-events`, `/admin` | 0 | Élevée |
| Pilotage stock | 15+ | `MODULE_PILOTAGE_STOCK` + `PILOTAGE_*` | Zone portail | `/stock-intelligence/**` | 0 | Élevée |
| Stock | 40+ | `MODULE_STOCK` + `STOCK_*` + `CMD_*` | Menus `stockMouvementsMenu` + permissions liens | `/stocks/**`, approv, inventaires, ventes, commandes | 0 | Élevée |
| GMAO | 12+ | `MODULE_GMAO` + `GMAO_*` | Zone + layout | `/gmao/**` | 0 | Élevée |
| DMI / Cardex / Consultation | 25+ | `MODULE_DMI` + `SIDEBAR_*` + `DMI_*` | Sidebar + shell | `/cardex`, `/consultations`, `/mediline` | 0 | Élevée (actions bouton PARTIEL) |
| POS / Caisse | 10+ | `OUVRIR_MODULE_CAISSE` / `MODULE_STOCK` | cmkerp-frontend (compat) | `/pos/**`, `/ventes` | 0 | Moyenne→Élevée |
| Facturation (Contrats + Sync CLINIQUE) | 8+ | `FACTURATION_CONTRATS_*` / `FACTURATION_SYNC_*` | Contrats + ClinicSync | `/api/v1/facturation/**` | 0 | Élevée (Lot 1) |

## Endpoints Facturation Lot 1 (catalogue obligatoire)

| Méthode | Path | Permission(s) anyOf |
| --- | --- | --- |
| GET | `/api/v1/facturation/contrats` (+ `/{id}`) | `FACTURATION_CONTRATS_VIEW`, `SIDEBAR_FACTURATION_CONTRATS` |
| POST | `/api/v1/facturation/contrats` | `FACTURATION_CONTRATS_CREATE` |
| PUT | `/api/v1/facturation/contrats/**` | `FACTURATION_CONTRATS_UPDATE`, `FACTURATION_CONTRATS_STATUS_UPDATE` |
| DELETE | `/api/v1/facturation/contrats/**` | `FACTURATION_CONTRATS_DELETE` |
| POST | `/api/v1/facturation/contrats/import/dry-run` | `FACTURATION_CONTRATS_IMPORT`, `FACTURATION_SYNC_DRY_RUN` |
| POST | `/api/v1/facturation/contrats/import` | `FACTURATION_CONTRATS_IMPORT`, `FACTURATION_SYNC_IMPORT` |
| GET | `/api/v1/facturation/clinic-sync/**` | `FACTURATION_SYNC_VIEW`, `SIDEBAR_FACTURATION_SYNCHRONISATION`, `FACTURATION_SYNC_HISTORY_VIEW` |
| POST | `/api/v1/facturation/clinic-sync/**` | `FACTURATION_SYNC_RETRY`, `FACTURATION_SYNC_CONFLICT_RESOLVE`, `FACTURATION_SYNC_IMPORT` |
| * | `/api/v1/stocks/facturation-sync` | `STOCK_FACTURATION_SYNC`, `MODULE_STOCK` |

Rebuild requis après modification : `mvn -pl cmkerp-gateway -am install` puis redémarrage gateway (shared-kernel embarqué dans le fat JAR).

## Endpoints publics (exceptions documentées)

| Path | Justification |
| --- | --- |
| `/api/v1/auth/**` | Authentification / session / refresh permissions |
| `/api/v1/health` | Healthcheck |
| `/api/v1/portail-fournisseur/**` | Accès tokenisé fournisseurs |
| `/api/v1/stock-intelligence/whatsapp/webhook` | Webhook externe |
| `/actuator/health`, `/actuator/info` | Ops |
| `/swagger-ui/**`, `/v3/api-docs/**` | Documentation (à restreindre en prod si besoin) |

## Mécanismes d’enforcement

1. `JwtAuthenticationFilter` — authentifie
2. `RbacEnforcementFilter` — autorise via `PermissionAuthorizationService` (DB/cache live, deny-by-default)
3. `@RequirePermission` — contrôles déclaratifs complémentaires
4. `GET /api/v1/auth/permissions` + `PermissionsDeniedListener` — rafraîchissement FE après 403
5. FK `ON DELETE CASCADE` sur `roles_permissions.fkPermission` — nettoyage ADMIN sans conflit avec trigger anti-retrait

## Critères de clôture vs objectifs 100 %

| Critère | État mesuré |
| --- | --- |
| 0 hardcode autorisation nominatif | **ATTEINT** |
| Catalogue permissions ↔ fonctionnalités principales | **ATTEINT** (V25 + initializer) |
| Endpoints métier sous contrôle | **ATTEINT** (filtre deny-by-default + catalogue) |
| Sync ADMIN + intégrité | **ATTEINT** |
| Révocation session (API live + refresh FE) | **ATTEINT** (mécanisme) — E2E 2 sessions staging à confirmer |
| Historique admin `audit_events` + UI | **ATTEINT** (`GET /roles/{id}/permission-audit`) |
| CI bloquante | **ATTEINT** |
| 100 % boutons FE individuels (chaque wizard stock/DMI) | **PARTIEL** — menus/zones/actions critiques OK |
| Tests Playwright révocation | **NON EXÉCUTÉS** en staging (environnement requis) |
| Tests MySQL triggers live | **À exécuter** sur instance MySQL de recette |

## Écarts résiduels assumés (non bloquants pour le socle)

| Écart | Impact | Plan |
| --- | --- | --- |
| Boutons individuels stock/DMI non encore sous `PermissionGate` partout | Moyen | Continuer au fil des écrans ; API déjà refusante |
| Dual `OUVRIR_*` + `MODULE_*` | Faible | Compat volontaire jusqu’à bascule prod |
| E2E Playwright révocation 2 sessions | Moyen | Staging |
| cmkerp-frontend hors dépôt git local | Faible | Alignement aliases déjà présent dans le code local |
