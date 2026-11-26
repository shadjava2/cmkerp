# AGENTS.md — CMK ERP

## Sept standards architecturaux permanents

| Standard | Fichier | Responsabilité |
| --- | --- | --- |
| RBAC Security | `.cursor/rules/rbac-security.mdc` | Autorisations et sécurité |
| CMK Design System | `.cursor/rules/cmk-design-system.mdc` | Cohérence visuelle et ergonomique |
| CLINIQUE Legacy Protection | `.cursor/rules/clinic-legacy-protection.mdc` | Intégrité des bases historiques SQL Server |
| Production Release | `.cursor/rules/production-release-standard.mdc` | Qualité produit, UI métier, zéro prototypage |
| Bidirectional Synchronization | `.cursor/rules/clinic-bidirectional-sync.mdc` | Outbox, réplication, rapprochement |
| ERP Relational Data | `.cursor/rules/erp-relational-data-standard.mdc` | Tables métier, FK, libellés, sélecteurs |
| Recherche patient unifiée | `../.cursor/rules/unified-patient-search.mdc` | Moteur Mediline commun DMI / Facturation / ERP |

CMK ERP est **une seule application**, plusieurs espaces métier. On change de module, pas d’identité produit.

**Philosophie produit** : complexité technique en arrière-plan, simplicité maximale pour l’utilisateur, sécurité serveur renforcée, interface exclusivement métier. Aucun texte de développement, bouton de simulation ou fonctionnalité fictive dans l’UI.

**Importations** : parcours Importer → Comparer → Sélectionner → Valider → Enregistrer → Actualiser via `ErpImportDialog` (pas de `window.confirm`). SQL Server = lecture seule ; MySQL = écriture contrôlée.

**Recherche patient** : un seul moteur Mediline (`buildPatientQuickSearchCriteria` + `POST /api/v1/cardex/patients/recherche` + `PatientSearchPanel` / `PatientPicker`). Modes NAVIGATION (DMI) et SELECTION (formulaires ERP). Interdit de dupliquer la logique de recherche.

**Chantier 02 (DMI × Facturation)** : `POST /api/v1/dmi/registrations`, registre `erp_patient_registration`, mapping société ↔ compte Mediline (V36). Voir `docs/integration/CHANTIER02_DMI_FACTURATION_INTEGRATION_REPORT.md`.

**Sélecteurs FK** : petit référentiel → ComboBox ; volume moyen → `ErpSearchableCombobox` ; complexe → Picker. Interdit : input de recherche + `<select>` pour la même clé étrangère.

### Migration CLINIQUE (coexistence)

- Nouvelles structures = MySQL `erp_*` uniquement.
- SQL Server Beach/CH = schéma **immuable** ; DML via outbox + feature flags (défaut off).
- Documentation : `docs/integration/CLINIQUE_*.md`
- Contrôle : `cmkerp/scripts/clinic-legacy-compliance-check.sh`

## Standard architectural obligatoire : Design System

Le DMI constitue la référence visuelle initiale. Tout nouveau module réutilise le shell institutionnel (sidebar navy, header blanc, motif de fond, typographie, cartes, boutons).

La différenciation se fait par le **contenu**, les **icônes** et la **couleur d’accent** (Facturation = ambre discret).

Voir :

- `.cursor/rules/cmk-design-system.mdc` (alwaysApply)
- `docs/design/CMK_ERP_DESIGN_SYSTEM.md`
- `consolenotification/src/styles/erpShell.css`

## Standard architectural obligatoire : RBAC dynamique

Le système RBAC n'est pas une fonctionnalité isolée. C'est un **standard de développement permanent**.

Voir :

- `.cursor/rules/rbac-security.mdc` (alwaysApply)
- `docs/security/RBAC_ARCHITECTURE.md`
- `docs/security/RBAC_INVENTORY.md`
- `docs/security/RBAC_DEVELOPMENT_GUIDELINES.md`

### Séparation fondamentale

| Dans le code | Dans la base (administrable) |
| --- | --- |
| Déclaration de la permission requise | Attribution permission ↔ rôle |
| Contrôle API + masquage UI | Composition libre des rôles métier |
| Protection structurelle du rôle ADMIN | — |

### Règle pour tout agent

Avant de livrer une capacité métier (module, menu, bouton, endpoint, export, validation…) :

1. Identifier ou créer le code de permission
2. Brancher parent hiérarchique
3. Migrer / seeder le catalogue MySQL
4. Enregistrer méthode HTTP + chemin dans `ApiPermissionCatalog` (filtre deny-by-default)
5. Protéger backend (`@RequirePermission` / service) + frontend (`PermissionGate` / `can`)
6. Vérifier attribution ADMIN + tests 200/403
7. Rebuild `cmkerp-shared-kernel` puis `cmkerp-gateway` (le JAR fat embarque le kernel)
8. Mettre à jour l'inventaire ; nettoyer les règles obsolètes si endpoint retiré

**Interdit** : utiliser `MODULE_*` (ou un parent) comme `anyOf` pour autoriser un menu ou une action enfant. La hiérarchie sert au formulaire d'administration, pas à l'autorisation runtime. DENY BY DEFAULT.

**Interdit** : livrer un endpoint `/api/v1/**` présent dans un contrôleur mais absent de `ApiPermissionCatalog` — ADMIN recevra aussi « Endpoint non couvert… ».

### UI adaptative et points d'entrée

- Tout point d'entrée (bouton, menu, carte, lien, route, raccourci clavier, API) vers une capacité exige la **même** permission.
- Les raccourcis clavier respectent le RBAC (ex. `Ctrl+K` inactif sans droit de recherche).
- Un conteneur sans enfant autorisé est masqué en entier (titre, grille, séparateurs, marges) — pas de sections vides.
- Aucun accès alternatif ne doit contourner une permission.

### Nouveau module — Definition of Done (intégration réelle)

Un module n'est **pas** livré tant que les preuves suivantes existent sur la **base cible** réellement utilisée par le gateway :

1. Permission `MODULE_*` présente dans `permissions` (requête SQL réelle)
2. ADMIN (id=5) `AUTORISE` sur cette permission (trigger / sync)
3. Carte visible sur le portail après reconnexion ADMIN
4. Route module accessible (layout + sidebar + accueil)
5. Branche visible dans Administration → Rôles & permissions
6. Utilisateur sans `MODULE_*` : carte invisible + accès direct refusé
7. Aucune exception hardcodée ADMIN / rôle métier pour forcer l'affichage

**Build PASS ≠ disponibilité.** Distinguer tests statiques (compliance, compilation) et vérifications d'intégration (SQL + session + UI).

Livrer sans couverture RBAC = violation du standard architectural.

### Contrôle CI bloquant

`cmkerp/scripts/rbac-compliance-check.sh` doit rester vert. Toute livraison sans couverture RBAC complète est rejetée.

