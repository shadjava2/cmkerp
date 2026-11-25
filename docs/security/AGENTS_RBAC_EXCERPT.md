# AGENTS.md — CMK ERP

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
3. Migrer / seeder le catalogue
4. Protéger backend + frontend
5. Mettre à jour l'inventaire

**Interdit** : utiliser `MODULE_*` (ou un parent) comme `anyOf` pour autoriser un menu ou une action enfant. La hiérarchie sert au formulaire d'administration, pas à l'autorisation runtime. DENY BY DEFAULT.

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

**Build PASS ≠ disponibilité.** Distinguer tests statiques et vérifications d'intégration (SQL + session + UI).

Livrer sans couverture RBAC = violation du standard architectural.

### Contrôle CI bloquant

`cmkerp/scripts/rbac-compliance-check.sh` doit rester vert. Toute livraison sans couverture RBAC complète est rejetée.

