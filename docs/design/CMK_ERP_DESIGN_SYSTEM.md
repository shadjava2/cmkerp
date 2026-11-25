# CMK ERP — Design System institutionnel

**Principe** : une application, une identité, plusieurs espaces métier.  
**Référence initiale** : module DMI (`DossierPatientShell`, `dossierPatient.css`).  
**Couche partagée émergente** : `consolenotification/src/styles/erpShell.css` + layouts module.

## Tokens institutionnels

| Token | Valeur | Usage |
| --- | --- | --- |
| Navy sidebar | `#112339` | Fond navigation |
| Encre | `#142940` | Texte principal |
| Muet | `#516579` | Labels secondaires |
| Ligne | `#dce5ed` | Bordures |
| Fond workspace | `#f3f6fa` + motif doodle | Zone de contenu |
| Accent DMI | `#096d74` / `#07565c` | Médical |
| Accent Facturation | `#8a5a00` / `#6b4500` | Financier sobre |
| Soft Facturation | `#f5efe4` | Icônes / avatar |

## Shell module

Structure commune (DMI `dp-*` ou ERP `erp-*`) :

1. Sidebar navy 228px (68px repliée)
2. Brand : logo 36×36 + `CMK ERP` + sous-titre module
3. Groupes de navigation + captions uppercase
4. Footer : Retour au portail
5. Topbar blanche 64px : collapse, breadcrumb, GatewayStatus, refresh, portail, profil, logout
6. Workspace avec `MedicalWhatsappBackdrop`
7. Contenu `*-page` max-width 1550px

## Composants partagés (cible)

| Concept | Implémentation actuelle |
| --- | --- |
| Shell | `erp-shell` / `dp-shell` |
| Sidebar | `erp-sidebar` / `dp-sidebar` |
| Header | `erp-topbar` / `dp-topbar` |
| Page | `erp-page` / `dp-page` |
| Card | `erp-card` / `dp-card` |
| KPI | `erp-kpi` |
| Shortcut | `erp-shortcut` / `dp-shortcut` |
| Bouton | `erp-btn` / `dp-btn` |
| Table | `erp-table` / `dp-table` |
| Backdrop | `MedicalWhatsappBackdrop` |
| Gateway | `GatewayStatus` |

## Intégration d’un nouveau module

1. Réutiliser `erp-shell` + classe `erp-accent-<module>`
2. Déclarer menus + permissions SIDEBAR_* (RBAC)
3. Header / session actions identiques — pas de duplication auth
4. Accent documenté dans ce fichier
5. Pas de sidebar / fond / typo divergents
6. États vides professionnels, pas de données fictives
7. Recette visuelle côte à côte avec le DMI (même zoom)

## Responsive

- ≤790px : sidebar icônes 62px, labels masqués
- ≤1120px : métadonnées profil masquées
- Contenu sans scroll horizontal global

## Règle Cursor

`.cursor/rules/cmk-design-system.mdc` (`alwaysApply: true`)
