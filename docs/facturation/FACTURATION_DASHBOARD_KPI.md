# Facturation V2 — Dictionnaire des indicateurs dashboard

Source agrégation actuelle : `GET /api/v1/stock-intelligence/pilotage/consommation-services/*`  
Périmètre : sorties / ventes médicaments (usage `VENTE PRIVEE`, `FACTUREE`, `USAGE`).

## Indicateurs opérationnels (branchés)

| Code permission | Libellé UI | Champ(s) source | Inclus | Exclus | Date | Devise | Méthode |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `FACTURATION_DASHBOARD_KPI_MEDICAMENTS_VIEW` | Médicaments facturés | `montant_vente_privee` + `montant_facturee` | usages privé + facturée | `montant_usage` (usage interne) | filtre `from`/`to` API | montants source (affichés USD côté UI actuelle) | Somme backend KPI |
| `FACTURATION_DASHBOARD_KPI_PRIVES_VIEW` | Patients privés | `montant_vente_privee` | VENTE PRIVEE | FACTUREE, USAGE | idem | idem | KPI backend |
| `FACTURATION_DASHBOARD_KPI_CONTRATS_USAGE_VIEW` | Sous contrat | `montant_facturee` | FACTUREE | VENTE PRIVEE, USAGE | idem | idem | KPI backend |
| `FACTURATION_DASHBOARD_CHART_EVOLUTION_VIEW` | Évolution | `stats/mensuel` : privé + facturée par `periode` | idem | USAGE | mois de période | idem | séries mensuelles backend |
| `FACTURATION_DASHBOARD_TABLE_PRODUITS_VIEW` | Top produits | `stats/produits` | privé + facturée | USAGE | filtre période | idem | top N backend |

**Annulations / remboursements** : non isolés dans ces agrégats ; le détail ventes conserve les statuts `ANNULEE` / `ANNULEE-REMBOURSE` au niveau transactionnel.

**FACTUREE ≠ encaissement** : le champ `montant_facturee` mesure des sorties valorisées sous usage contrat, pas un paiement reçu.

## Indicateurs non disponibles (affichage explicite)

| Libellé | Raison |
| --- | --- |
| Total encaissé | Pas de source de règlement fiable branchée |
| Solde à recevoir / créances | Idem |
| Prestations médicales | Circuit distinct non agrégé |
| Contrats actifs / ayants droit | Référentiel conventions non exposé |
| Taux d'encaissement | Dépend des encaissements |

Ne jamais afficher `0` à la place de « Donnée indisponible » pour ces indicateurs.

## Filtres UI actuels

- Période : mois / trimestre / année / personnalisée → `from` / `to`
- Pharmacie : contexte portail (`pharmacieId`) si sélectionné

Filtres société / contrat / médecin / classe : reportés dès exposition des dimensions côté API.

## Évolutions prévues

1. Endpoint Facturation dédié (agrégation multi-sites, multi-devises documentée).
2. Permissions KPI encaissement / créances / prestations dès sources fiables.
3. Drill-down RBAC vers listes médicaments / prestations.
