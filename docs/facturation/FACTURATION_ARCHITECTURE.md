# Facturation V2 — Architecture métier

**État** : structure module + dashboard médicaments opérationnel (2026-11-25).  
**Racine RBAC** : `MODULE_FACTURATION` (zone portail uniquement — refuse by default pour le reste).

## Objectif

Espace de travail hospitalier dédié à la facturation, distinct du DMI (soins) et du Stock (mouvements), sans dupliquer les services métier ni le moteur RBAC.

## Navigation officielle

| Section | Menu | Route | Permission | État |
| --- | --- | --- | --- | --- |
| — | Tableau de bord | `/facturation` | `SIDEBAR_FACTURATION_ACCUEIL` | Opérationnel (KPI médicaments) |
| Contrats & bénéficiaires | Contrats | `/facturation/contrats-gestion` | `SIDEBAR_FACTURATION_CONTRATS` | Structuré |
| | Ayants droit | `/facturation/ayants-droit` | `SIDEBAR_FACTURATION_AYANTSDROIT` | Structuré |
| | Sociétés | `/facturation/societes` | `SIDEBAR_FACTURATION_SOCIETES` | Structuré |
| Opérations | Prestations | `/facturation/prestations` | `SIDEBAR_FACTURATION_PRESTATIONS` | Structuré |
| | Médicaments | `/facturation/medicaments` | `SIDEBAR_FACTURATION_MEDICAMENTS` | Opérationnel |
| Référentiels | Tarifs | `/facturation/tarifs` | `SIDEBAR_FACTURATION_TARIFS` | Opérationnel (sync) |
| | Imputations | `/facturation/imputations` | `SIDEBAR_FACTURATION_IMPUTATIONS` | Structuré |
| | Classes | `/facturation/classes` | `SIDEBAR_FACTURATION_CLASSES` | Structuré |
| | Médecins | `/facturation/medecins` | `SIDEBAR_FACTURATION_MEDECINS` | Structuré |
| | Services | `/facturation/services` | `SIDEBAR_FACTURATION_SERVICES` | Structuré |
| Pharmacie & stock | Pharmacies | `/facturation/pharmacies` | `SIDEBAR_FACTURATION_PHARMACIES` | Structuré |
| | Produits | `/facturation/produits` | `SIDEBAR_FACTURATION_PRODUITS` | Opérationnel (catalogue Stock) |
| | Stock | `/facturation/stock` | `SIDEBAR_FACTURATION_STOCK` | Structuré (lecture seule prévue) |
| Administration | Utilisateurs | `/facturation/utilisateurs` | `SIDEBAR_FACTURATION_UTILISATEURS` | Structuré |

## Réutilisation métier

| Capacité | Service / UI réutilisé | Permission API |
| --- | --- | --- |
| Ventes médicaments contrat / privé | `VentesListPage` / `VentePosWizardPage` | `STOCK_VENTE_VIEW` / `STOCK_VENTE_CREATE` |
| Sync tarifs Beach ↔ CH | `FacturationSyncPage` | `STOCK_FACTURATION_SYNC` |
| Catalogue produits | `ProduitsStockPage` | `STOCK_PRODUIT_VIEW` / `STOCK_VIEW` |
| KPI dashboard | `consommation-services` (stock-intelligence) | permissions dashboard Facturation + API Pilotage existante |

## Séparation DMI

`SIDEBAR_FACTURATION` (parent `MODULE_DMI`) reste le menu **Facturation des soins** clinique.  
Aucune collision de code avec `SIDEBAR_FACTURATION_*` du module autonome.

## Principes

1. `MODULE_FACTURATION` ouvre la zone — pas les menus ni les KPI.
2. Chaque menu exige sa `SIDEBAR_*` exclusive (pas de `anyOf` avec le module).
3. Les actions partagées Stock / DMI réutilisent les permissions métier existantes.
4. Pas de chiffres fictifs ; « Donnée indisponible » si la source manque.
5. Prestations ≠ médicaments (circuits distincts, supervision commune future).
