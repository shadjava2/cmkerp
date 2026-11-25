# Inventaire RBAC CMK ERP

État : **Phase 2 — catalogue + enforcement API** (2026-11-25).  
À enrichir à chaque livraison fonctionnelle (DoD RBAC).

Légende : `OK` | `PARTIEL` | `MANQUANT`

## 1. Modules / zones portail (`consolenotification`)

| Module | Emplacement | Route | Permission | Parent | État |
| --- | --- | --- | --- | --- | --- |
| Administration | `accessZones.ts` | `/administration` | `MODULE_ADMINISTRATION` | 0 | OK |
| Pilotage stock | `accessZones.ts` | `/portail/pilotage` | `MODULE_PILOTAGE_STOCK` | 0 | OK |
| Stock & mouvements | `accessZones.ts` | `/portail/stock-mouvements` | `MODULE_STOCK` | 0 | OK |
| GMAO | `accessZones.ts` | `/gmao` | `MODULE_GMAO` | 0 | OK |
| Dossier patient (DMI) | `accessZones.ts` | `/dossier-patient` | `MODULE_DMI` | 0 | OK |

## 2. Sidebar DMI

| Menu | Permission | Parent | État |
| --- | --- | --- | --- |
| Accueil | `SIDEBAR_ACCUEIL` | MODULE_DMI | OK |
| Réception | `SIDEBAR_RECEPTION` | MODULE_DMI | OK |
| Cardex | `SIDEBAR_CARDEX` | MODULE_DMI | OK |
| Triage | `SIDEBAR_TRIAGE` | MODULE_DMI | OK |
| Cabinet | `SIDEBAR_CABINET` | MODULE_DMI | OK |
| Urgences | `SIDEBAR_URGENCES` | MODULE_DMI | OK |
| Hospitalisation | `SIDEBAR_HOSPITALISATION` | MODULE_DMI | OK |
| Soins rapprochés | `SIDEBAR_SOINSRAPPROCHE` | MODULE_DMI | OK |
| Prélèvement | `SIDEBAR_PRELEVEMENT` | MODULE_DMI | OK |
| Pharmacie | `SIDEBAR_PHARMACIE` | MODULE_DMI | OK |
| Facturation | `SIDEBAR_FACTURATION` | MODULE_DMI | OK |

## 3. Actions métier DMI

| Action | Permission | Backend | Frontend | État |
| --- | --- | --- | --- | --- |
| Consulter patients | `DMI_RECEPTION_PATIENT_VIEW` | Filtre + Mediline | PARTIEL | PARTIEL |
| Créer patient | `DMI_RECEPTION_PATIENT_CREATE` | Filtre path | PARTIEL | PARTIEL |
| Modifier patient | `DMI_RECEPTION_PATIENT_UPDATE` | Filtre path | PARTIEL | PARTIEL |
| Créer consultation | `DMI_CABINET_CONSULTATION_CREATE` | Filtre consultations | PARTIEL | PARTIEL |
| Prescrire | `DMI_CABINET_PRESCRIPTION_CREATE` | Catalogue | PARTIEL | PARTIEL |
| Annuler consultation | `DMI_CABINET_CONSULTATION_CANCEL` | Catalogue | PARTIEL | PARTIEL |

## 4. Stock (menus + API)

| Capacité | Permission | FE menu | API catalogue | État |
| --- | --- | --- | --- | --- |
| Produits | `STOCK_PRODUIT_*` | OK | OK | OK |
| Fusion CLINIQUE | `STOCK_PRODUIT_FUSION` | OK | OK | OK |
| Prix / marges | `STOCK_PRIX_AJUSTER` | OK | OK | OK |
| Sync facturation | `STOCK_FACTURATION_SYNC` | OK | OK | OK |
| Approvisionnements | `STOCK_APPROV_*` | OK | OK | OK |
| Transferts | `STOCK_TRANSFERT_*` | OK | OK | OK |
| Inventaires | `STOCK_INVENTAIRE_*` | OK | OK | OK |
| Ventes / sorties | `STOCK_VENTE_*` | OK | OK | OK |
| Reports | `STOCK_REPORT_EXPORT` | OK | OK | OK |

## 5. Administration (console)

| Capacité | Permission | Endpoint / route | État |
| --- | --- | --- | --- |
| Utilisateurs ERP | `ADMIN_USERS_*` | `/administration/utilisateurs` | OK |
| Rôles & permissions | `ADMIN_ROLES_*` / `ADMIN_ROLES_MANAGE` | `/administration/roles` | OK |
| Catalogue permissions | `ADMIN_PERMISSIONS_CATALOG_VIEW` | `/administration/permissions-catalog` | OK |
| Audit des accès | `ADMIN_RBAC_AUDIT_VIEW` | `/administration/rbac-audit` | OK |
| Intégrité RBAC | `ADMIN_RBAC_INTEGRITY_*` | `/administration/rbac-integrity` | OK |
| Autorisations opérations | `STOCK_AUTORISATION_DECIDE` | `/administration/autorisations` | OK (distinct du RBAC) |
| Usage events | `ADMIN_USAGE_VIEW` | `/administration/usage` | OK |

## 6. Hardcodes (éliminés)

| Zone | État |
| --- | --- |
| GMAO initializer | OK (supprimé) |
| AppUsageEventService | OK (permission) |
| MedilinePatientController | OK (PermissionAuthorizationPort) |
| Compliance script | PASSED |

## 7. Tests

| Scénario | État |
| --- | --- |
| Arbre 3 états FE (`rbacTree`) | Présent |
| Protection ADMIN domain | 6/6 PASS |
| ApiPermissionCatalog | PASS |
| Révocation E2E 2 sessions | Staging requis |
| Triggers MySQL live | Recette SQL recommandée |
