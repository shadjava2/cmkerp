# Inventaire RBAC CMK ERP

État : **Phase 2 — catalogue + enforcement API** (2026-11-25).  
À enrichir à chaque livraison fonctionnelle (DoD RBAC).

Légende : `OK` | `PARTIEL` | `MANQUANT`

## 1. Modules / zones portail (`consolenotification`)

| Module | Emplacement | Route | Permission | Parent | État |
| --- | --- | --- | --- | --- | --- |
| Administration | `accessZones.ts` | `/administration` | `MODULE_ADMINISTRATION` | 0 | OK |
| Pilotage | `accessZones.ts` | `/portail/pilotage` | `MODULE_PILOTAGE_STOCK` | 0 | OK |
| Stock | `accessZones.ts` | `/portail/stock-mouvements` | `MODULE_STOCK` | 0 | OK |
| GMAO | `accessZones.ts` | `/gmao` | `MODULE_GMAO` | 0 | OK |
| DMI | `accessZones.ts` | `/dossier-patient` | `MODULE_DMI` | 0 | OK |
| Facturation | `accessZones.ts` | `/facturation` | `MODULE_FACTURATION` | 0 | OK |

### Facturation — menus V2 (module autonome)

| Menu / capacité | Permission | Parent / réutilisation | État |
| --- | --- | --- | --- |
| Tableau de bord | `SIDEBAR_FACTURATION_ACCUEIL` | `MODULE_FACTURATION` | OK |
| KPI médicaments / privé / contrat | `FACTURATION_DASHBOARD_KPI_*` | `SIDEBAR_FACTURATION_ACCUEIL` | OK |
| Chart évolution / table produits | `FACTURATION_DASHBOARD_CHART_EVOLUTION_VIEW` / `…_TABLE_PRODUITS_VIEW` | `SIDEBAR_FACTURATION_ACCUEIL` | OK |
| Contrats (types TCONTRAT) | `SIDEBAR_FACTURATION_CONTRATS` + `FACTURATION_CONTRATS_*` | `MODULE_FACTURATION` | OK — API cataloguée `/api/v1/facturation/contrats/**` |
| Synchronisation CLINIQUE | `SIDEBAR_FACTURATION_SYNCHRONISATION` + `FACTURATION_SYNC_*` | `MODULE_FACTURATION` | OK — API cataloguée `/api/v1/facturation/clinic-sync/**` |
| Locations | `SIDEBAR_FACTURATION_LOCATIONS` + `FACTURATION_LOCATIONS_*` | `MODULE_FACTURATION` | API V31 — écran métier non branché |
| Ayants droit | `SIDEBAR_FACTURATION_AYANTSDROIT` + `FACTURATION_AYANTSDROIT_*` | `MODULE_FACTURATION` | API V31 — écran métier non branché |
| Sociétés | `SIDEBAR_FACTURATION_SOCIETES` + `FACTURATION_SOCIETES_*` | `MODULE_FACTURATION` | API V31 — écran métier non branché |
| Prestations | `SIDEBAR_FACTURATION_PRESTATIONS` + `FACTURATION_PRESTATIONS_VIEW/IMPORT/STATUS_UPDATE` | `MODULE_FACTURATION` | API V31 import seul — écran non branché |
| Médicaments | `SIDEBAR_FACTURATION_MEDICAMENTS` + `FACTURATION_MEDICAMENTS_VIEW/IMPORT/STATUS_UPDATE` | `MODULE_FACTURATION` | API V31 import seul — écran non branché |
| Actions ventes médicaments | `STOCK_VENTE_VIEW` / `STOCK_VENTE_CREATE` | `MODULE_STOCK` (API partagée) | OK — réutilisation |
| Tarifs | `SIDEBAR_FACTURATION_TARIFS` + `FACTURATION_TARIFS_*` | `MODULE_FACTURATION` | API V31 — écran métier non branché |
| Sync Beach ↔ CH | `STOCK_FACTURATION_SYNC` | `MODULE_STOCK` (API partagée) | OK — réutilisation |
| Imputations / Classes / Médecins / Services | `SIDEBAR_FACTURATION_*` + `FACTURATION_{IMPUTATIONS,CLASSES,MEDECINS,SERVICES}_*` | `MODULE_FACTURATION` | API V31 — écran métier non branché |
| Pharmacies / Stock lecture | `SIDEBAR_FACTURATION_PHARMACIES` / `_STOCK` + actions `FACTURATION_PHARMACIES_*` / `FACTURATION_STOCK_VIEW/IMPORT/STATUS_UPDATE` | `MODULE_FACTURATION` | API V31 — stock en projection, écran non branché |
| Produits | `SIDEBAR_FACTURATION_PRODUITS` + `FACTURATION_PRODUITS_*` | `MODULE_FACTURATION` | API V31 — écran métier non branché |
| Utilisateurs | `SIDEBAR_FACTURATION_UTILISATEURS` + `FACTURATION_UTILISATEURS_*` | `MODULE_FACTURATION` | API V31 — sans mot de passe, écran non branché |
| Facturation des soins (DMI) | `SIDEBAR_FACTURATION` | `MODULE_DMI` | Conservé (parcours clinique) |
| Encaissements / créances natifs | — | — | Backlog (pas de KPI fictif) |

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
