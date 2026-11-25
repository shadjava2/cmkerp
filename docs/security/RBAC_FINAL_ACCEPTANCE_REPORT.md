# Rapport final — Clôture opérationnelle RBAC CMK ERP

**Date** : 2026-11-25  
**Périmètre** : socle transversal permissions dynamiques (catalogue, API, FE DMI/Admin, conformité CI)  
**Décision** : **GO CONDITIONNEL** (socle sécurité critique validé ; recettes E2E multi-sessions à confirmer par l’opérateur)

---

## 1. Checklist de recette (10 points)

| # | Critère | Résultat | Preuve |
| --- | --- | --- | --- |
| 1 | Recherche globale correctement masquée | **PASS** | `DossierPatientShell` : bouton header rendu seulement si `canSearchPatients()` ; recette visuelle CONTROLEUR antérieure |
| 2 | Raccourci Ctrl+K inaccessible sans permission | **PASS** | Listener enregistré uniquement si `canSearchPatient` ; sinon pas d’ouverture de panneau |
| 3 | Sections vides entièrement masquées | **PASS** | Accès rapides / Filières filtrés avant rendu ; sidebar Admin masque sections sans liens visibles |
| 4 | Attribution individuelle des permissions opérationnelle | **NOT TESTED** (opérateur) | Nécessite 2 sessions ADMIN + métier ; à exécuter sur la grille progressive (§2 prompt utilisateur) |
| 5 | Retrait individuel des permissions opérationnel | **NOT TESTED** (opérateur) | Idem + invalidation cache rôle (`RoleApplicationService` → `invalidateRolePermissions`) |
| 6 | Accès direct aux URL interdites bloqué | **PASS** (code) | Guard de route DMI dans `DossierPatientShell` (redirect si `SIDEBAR_*` absent) |
| 7 | API non autorisées → HTTP 403 | **PASS** (mécanisme + tests) | `RbacEnforcementFilter` écrit `PERMISSION_DENIED` 403 ; `ApiPermissionCatalogTest` ; deny-by-default si path non catalogué |
| 8 | Révocation effective pendant une session active | **PASS** (mécanisme) / **NOT TESTED** (E2E live) | Cache user/role invalidé ; FE `cmk:permissions-denied` → `refreshPermissions()` ; gateway local non joignable pour preuve HTTP live |
| 9 | ADMIN conserve toutes ses permissions | **PASS** (mécanisme) / **NOT TESTED** (SQL live) | Sync ADMIN + `GET /api/v1/rbac/admin/integrity` ; MySQL local non authentifié depuis ce poste (mot de passe env. différent) |
| 10 | Même comportement Admin / Stock / Pilotage / GMAO | **PASS PARTIEL** | API cataloguée pour les 4 domaines ; FE Admin durci (menus explicites) ; FE Stock/GMAO encore surtout au niveau module/zone (écarts résiduels) |

**Score mesuré hors recettes manuelles opérateur** : **7/10 PASS** + **2 NOT TESTED** + **1 PASS PARTIEL**.

---

## 2. Contrôles automatisés exécutés

| Contrôle | Résultat |
| --- | --- |
| `git pull` console + cmkerp | Already up to date (`d981514` / `93794e9` puis correctifs de clôture) |
| `scripts/rbac-compliance-check.sh` | **PASSED** (EXIT 0) |
| `ApiPermissionCatalogTest` (+ assertions anti-`MODULE_DMI` sur cardex/mediline) | **PASS** |
| Gateway HTTP local (8080/8090) | **NON DISPONIBLE** — pas de preuve curl 403 live |
| Vite desktop | **UP** `http://127.0.0.1:3940/` |
| MySQL `127.0.0.1:32768` | Port ouvert ; login `cmkroot` refusé depuis ce poste → intégrité SQL **NOT TESTED** |

---

## 3. Architecture permanente retenue (confirmée)

| Responsabilité | Mécanisme | État |
| --- | --- | --- |
| Catalogue | `permissions` | Opérationnel |
| Hiérarchie admin UI | `permissions.parent` | Opérationnel (non héritée au runtime) |
| Attributions | `roles_permissions` | Opérationnel |
| Sync ADMIN | triggers / initializer | Mécanisme en place |
| Sécurité backend | `PermissionAuthorizationService` + `RbacEnforcementFilter` | Opérationnel |
| Visibilité FE | `usePermissions` / `PermissionGate` / `canSeeDmiMenuItem` | Opérationnel (DMI + Admin) |
| Standard agents | `.cursor/rules/rbac-security.mdc` + `AGENTS.md` | À jour (UI adaptative + raccourcis) |
| Conformité CI | `rbac-compliance-check.sh` | Vert |

---

## 4. Correctifs appliqués pendant la clôture

1. **AdministrationSidebar** — retrait de `MODULE_ADMINISTRATION` / `OUVRIR_ADMINISTRATION` comme `anyOf` des menus enfants ; permissions fonctionnelles explicites ; sections vides masquées.
2. **ApiPermissionCatalogTest** — garde-fous : `/api/v1/cardex` et `/api/v1/mediline` n’acceptent plus `MODULE_DMI` dans le `anyOf`.

---

## 5. Écarts résiduels (non bloquants pour le socle, à traiter au fil métier)

| Écart | Impact | Plan |
| --- | --- | --- |
| Stock/GMAO : `MODULE_STOCK` / `MODULE_GMAO` encore présents en `anyOf` sur beaucoup d’API mutantes | Moyen | Retirer progressivement les parents module des règles mutantes (comme DMI) |
| Boutons individuels Stock/GMAO sans `PermissionGate` systématique | Moyen | UI adaptative au fil des écrans ; API déjà refuse |
| Recette E2E 2 sessions (grant/revoke live) | Moyen | Opérateur — grille progressive CONTROLEUR |
| Preuve SQL intégrité ADMIN depuis ce poste | Faible | Exécuter `GET /api/v1/rbac/admin/integrity` via session ADMIN |
| Liens Communication Admin rattachés à `ADMIN_USAGE_VIEW` / `ADMIN_ROLES_MANAGE` (proxy) | Faible | Créer permissions dédiées si besoin métier |

---

## 6. Décision GO

### GO sur le socle (autorisé)

- Deny-by-default API + catalogue path.
- DMI : menus, recherche, raccourcis, sections vides, sans héritage `MODULE_DMI`.
- Règles permanentes + script CI bloquant verts.
- Mécanismes de révocation cache + refresh FE présents.

### Conditions avant GO absolu métier

1. Exécuter la grille d’attribution / retrait progressive (ADMIN + CONTROLEUR) et cocher les items 4–5.
2. Sur une session métier limitée, appeler une API stock/DMI interdite et constater **HTTP 403** + rafraîchissement UI.
3. Vérifier l’écran **Intégrité RBAC** (ADMIN) : 0 permission manquante pour le rôle système.

### Interdit désormais

Toute nouvelle capacité sans permission catalogue + contrôle API + point d’entrée FE aligné + inventaire — voir `AGENTS.md` et `rbac-security.mdc`.

---

## 7. Commandes de reprise opérateur

```powershell
cd C:\Users\HP\Videos\STSDEV\consolenotification
git pull
# Ctrl+R dans l'app

cd C:\Users\HP\Videos\STSDEV\cmkerp
git pull
bash scripts/rbac-compliance-check.sh
```

Puis : 2 navigateurs (ADMIN + CONTROLEUR) → Administration → Rôles & permissions → cocher/décocher une à une les permissions de la grille progressive.
