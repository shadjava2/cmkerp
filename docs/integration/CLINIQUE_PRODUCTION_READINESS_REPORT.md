# CLINIQUE / Facturation — Production readiness

Date : 2026-03-25

## Autorisé en production aujourd’hui

| Capacité | État |
| --- | --- |
| Lecture Beach / import sélectif référentiels | Oui |
| CRUD Contrats ERP + sync TCONTRAT | Oui (gated) |
| Tables relationnelles `erp_*` + libellés UI | Oui (MySQL) |
| WRITE Beach hors TCONTRAT | **Non** |
| WRITE CH / Mediline / DDL Beach | **Interdit** |

## Implémenté / testé / validé

| Élément | Implémenté | Testé | Validé prod | Notes |
| --- | --- | --- | --- | --- |
| V32 tables + migration données | Oui | SQL counts | Oui (DB cible) | Sans outbox |
| Listes avec libellés FK | Oui | Compile + SQL | À recetter UI | Sociétés, ayants, tarifs… |
| Sélecteurs contrat/société/service/imputation | Oui | — | À recetter UI | Formulaires |
| PatientPicker Mediline | Non | — | — | Prochaine vague |
| Mapping users/pharmacies manuel UI | Partiel | — | — | Tables créées |
| Facturation prestations formulaire | Structure table | — | Non | Adaptateurs gated |
| Recette SQL Server isolée Lots 0/1 | Non | — | Bloqué | Pas d’env isolé |
| Dashboard financier agrégé | Partiel | — | — | Données réelles referentials |

## Feature flags (attendu)

```
clinic.beach.write-enabled / WRITE_ENTITIES = TCONTRAT
clinic.ch.write.enabled = false
```

## Actions utilisateur pour la prochaine recette

1. Rebuild + restart Gateway après pull.
2. Vérifier Sociétés : colonne Contrat = désignation + code secondaire.
3. Vérifier Ayants droit : Société / Contrat en libellés ; formulaire avec listes.
4. Vérifier Tarifs : service + imputation libellés.
5. Centre sync : compteurs inchangés hors TCONTRAT writes.
