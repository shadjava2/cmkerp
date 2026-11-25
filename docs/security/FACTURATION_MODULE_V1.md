# Module Facturation — v1 (intégration)

## Objectif

Espace de travail autonome **Facturation** (`MODULE_FACTURATION`), séparé du DMI, sans inventer de moteurs financiers.

## Réutilisé (opérationnel)

| Capacité | UI module | API | Permission métier |
| --- | --- | --- | --- |
| Facturation contrats | `/facturation/contrats` | `/api/v1/ventes` | `STOCK_VENTE_VIEW` / `CREATE` |
| Facturation privées | `/facturation/privees` | `/api/v1/ventes` | `STOCK_VENTE_VIEW` / `CREATE` |
| Sync tarifs Beach ↔ CH | `/facturation/sync` | `/api/v1/stocks/facturation-sync` | `STOCK_FACTURATION_SYNC` |

Les mêmes écrans restent accessibles sous Stock (`/stock-mouvements/facturation-*`).

## Conservé dans le DMI

- Menu **Facturation des soins** (`SIDEBAR_FACTURATION`) — parcours clinique, non supprimé.
- Intégration future : une facture issue du parcours de soins devra être retrouvable dans le module Facturation via le même référentiel ventes / factures.

## Permissions créées (v1)

- `MODULE_FACTURATION` — accès portail uniquement (deny-by-default sur les circuits)
- `SIDEBAR_FACTURATION_ACCUEIL` — menu Accueil

Aucune attribution automatique hors ADMIN (trigger / `syncAdmin`).

## Backlog (non opérationnel)

Tableau de bord, conventions / organismes payeurs, créances, remboursements dédiés, caisse hospitalière, rapports financiers natifs, paramètres de facturation — chantier métier ultérieur avec enrichissement hiérarchique RBAC.
