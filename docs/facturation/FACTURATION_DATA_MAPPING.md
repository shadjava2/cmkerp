# Facturation V2 — Cartographie des données

## Tableau de correspondance

| Menu | Source | Table / API | Service | Endpoint | Permission navigation | État |
| --- | --- | --- | --- | --- | --- | --- |
| Tableau de bord | CLINIQUE / ventes sorties | agrégats consommation-services | cmkerp-stocks | `GET …/consommation-services/kpis` (+ mensuel, produits) | `SIDEBAR_FACTURATION_ACCUEIL` + `FACTURATION_DASHBOARD_*` | Opérationnel (médicaments) |
| Contrats (conventions) | À confirmer (Beach / CH / ERP) | — | — | — | `SIDEBAR_FACTURATION_CONTRATS` | Structuré |
| Ayants droit | À confirmer vs patient | — | — | — | `SIDEBAR_FACTURATION_AYANTSDROIT` | Structuré |
| Sociétés | À confirmer (entreprises) | — | — | — | `SIDEBAR_FACTURATION_SOCIETES` | Structuré |
| Prestations | DMI / Mediline / CLINIQUE | parcours soins | — | — | `SIDEBAR_FACTURATION_PRESTATIONS` | Structuré |
| Médicaments | Moteur ventes Stock | ventes / lignes | cmkerp-stocks | API ventes existante | `SIDEBAR_FACTURATION_MEDICAMENTS` + `STOCK_VENTE_*` | Opérationnel |
| Tarifs | Sync Beach ↔ CH | service facturation-sync | cmkerp-stocks | sync existant | `SIDEBAR_FACTURATION_TARIFS` + `STOCK_FACTURATION_SYNC` | Opérationnel |
| Imputations | Schéma métier à confirmer | — | — | — | `SIDEBAR_FACTURATION_IMPUTATIONS` | Structuré |
| Classes | Référentiel existant à confirmer | — | — | — | `SIDEBAR_FACTURATION_CLASSES` | Structuré |
| Médecins | Référentiel DMI / CLINIQUE | — | — | — | `SIDEBAR_FACTURATION_MEDECINS` | Structuré |
| Services | Référentiel hospitalier | — | — | — | `SIDEBAR_FACTURATION_SERVICES` | Structuré |
| Pharmacies | Stock / portal pharmacie | — | — | — | `SIDEBAR_FACTURATION_PHARMACIES` | Structuré |
| Produits | Catalogue Stock | produits | cmkerp-stocks | API produits | `SIDEBAR_FACTURATION_PRODUITS` + `STOCK_PRODUIT_VIEW` | Opérationnel |
| Stock | Module Stock (lecture) | stocks / mouvements | — | — | `SIDEBAR_FACTURATION_STOCK` | Structuré |
| Utilisateurs | ERP users + RBAC | users / roles_permissions | gateway | admin users | `SIDEBAR_FACTURATION_UTILISATEURS` | Structuré |

## Bases

| Domaine | Base typique | Notes |
| --- | --- | --- |
| Auth / RBAC / users | MySQL primaire ERP (`.env` CMK_PRIMARY) | Catalogue `permissions` |
| Ventes / sorties / produits | Bases CLINIQUE Beach / CH via stocks | Ne pas double-compter |
| Patients / soins | Mediline / DMI | Prestations distinctes |
| Conventions / sociétés | Non unifié REST à ce jour | Audit préalable obligatoire |

## Interdits

- Assimiler `FACTUREE` (usage sortie) à un encaissement réel.
- Additionner USD et CDF sans conversion documentée.
- Calculer les KPI dashboard côté navigateur sur des milliers de lignes brutes.
