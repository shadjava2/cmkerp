# CMK ERP — Recette utilisateur finale Facturation V1

Date : **2026-11-26**  
Objectif : checklist **écran par écran** pour validation personnelle (0/19).  
Référence technique : `CLINIQUE_MIGRATION_MASTER_REPORT.md`

Données pré-chargées pour la recette : **29 100** ayants liés Mediline · **2 500** prestations importées · **1 500** médicaments importés · **7 951** lignes stock compartiments. Import exhaustif Beach encore progressif via API.

### Légende risque données

| Marque | Signification |
| --- | --- |
| 🟢 Sûr prod | Lecture / navigation / filtres |
| 🟡 Contrôlé | Écriture MySQL ERP (réversible ou isolée métier) — **pas** Beach hors contrats |
| 🔴 Isolé uniquement | Écriture Beach / scénarios destructifs — **environnement de recette** |

---

## Score personnel

# **0 / 19**

Cocher après validation réelle.

---

## 1. Contrats — Création, modification et réplication

| | |
| --- | --- |
| Fonctionnalités | CRUD, import sélectif, outbox, sync Beach whitelist |
| Parcours | Créer / modifier → vérifier file → (isolé) réplication |
| Attendu | Intention `PENDING`/`SUSPENDED` ; jamais perte d’intention type TES |
| Relations | CODE + DESIGNATION réplicables |
| Sync | Live démontré sur TCONTRAT |
| Anomalies connues | Écritures hors CODE/DESIGNATION hors scope |
| Risque | 🟡 ERP ; 🔴 Beach hors recette isolée |

- [ ] Validé

## 2. Sociétés — Relation avec le contrat

| | |
| --- | --- |
| Fonctionnalités | Liste libellés, FK contrat, import |
| Parcours | Ouvrir fiche → sélectionner contrat → enregistrer |
| Attendu | Libellé contrat affiché ; pas de saisie libre d’id |
| Sync | ERP ; Beach non whitelisté |
| Risque | 🟡 |

- [ ] Validé

## 3. Ayants droit — Société, contrat, patient Mediline et IPP

| | |
| --- | --- |
| Fonctionnalités | CRUD relationnel, PatientPicker, `patient_link_status`, IPP |
| Parcours | Recherche Mediline → sélection → enregistrer → rouvrir |
| Attendu | IPP persisté ; état LINKED/UNLINKED/REVIEW ; ambiguïté gérée |
| Restrictions | Aucune écriture Mediline ; volume live LINKED = 0 avant recette |
| Risque | 🟡 |

- [ ] Validé

## 4. Services — Consultation et gestion

| | |
| --- | --- |
| Volume live | 43 |
| Attendu | Libellés, actif, import sélectif |
| Risque | 🟢 lecture / 🟡 écriture ERP |

- [ ] Validé

## 5. Imputations — Libellés et sélection

| | |
| --- | --- |
| Volume live | 49 |
| Attendu | Utilisable dans tarifs / prestations |
| Risque | 🟢 / 🟡 |

- [ ] Validé

## 6. Classes — Relations métier

| | |
| --- | --- |
| Volume live | 24 |
| Attendu | FK service résolue en libellé |
| Risque | 🟢 / 🟡 |

- [ ] Validé

## 7. Tarifs — Service, classe, imputation et montants

| | |
| --- | --- |
| Volume live | 1 977 |
| Attendu | ComboBox parents ; montants DECIMAL ; pas de code technique seul |
| Anomalies | Formules Beach historiques non figées |
| Risque | 🟢 / 🟡 |

- [ ] Validé

## 8. Locations — Correspondance avec les sites ERP

| | |
| --- | --- |
| Volume live | 3 |
| Attendu | Lien site ERP si mappé ; orphelin signalé |
| Risque | 🟢 / 🟡 |

- [ ] Validé

## 9. Médecins — Recherche et sélection

| | |
| --- | --- |
| Volume live | 140 |
| Attendu | Recherche + sélection dans postes facturation |
| Risque | 🟢 / 🟡 |

- [ ] Validé

## 10. Utilisateurs — Mapping et signature CLINIQUE

| | |
| --- | --- |
| Volume live | 18 mappings |
| Parcours | Associer ERP ↔ legacy → signature backend |
| Attendu | Pas de lecture `MOTDEPASSE` ; désactivation possible |
| Sync | Requis pour débloquer `BLOCKED_MAPPING` sur facturation |
| Risque | 🟡 |

- [ ] Validé

## 11. Pharmacies — Mapping et habilitations

| | |
| --- | --- |
| Volume live | 7 mappings |
| Attendu | Association + site ; usage dans médicaments |
| Risque | 🟡 |

- [ ] Validé

## 12. Produits — Recherche et correspondances

| | |
| --- | --- |
| Volume live | 5 177 mappings externes |
| Attendu | Recherche serveur ; libellés ; pas de saisie libre FK |
| Risque | 🟢 / 🟡 |

- [ ] Validé

## 13. Stock — Quantités et rapprochement

| | |
| --- | --- |
| Volume live | `erp_stock_clinique` = 0 |
| Attendu | Consultation / import projection ; **pas** d’écriture Beach |
| Anomalies | `STOCK1`…`n` sans mapping confirmé |
| Risque | 🟢 |

- [ ] Validé

## 14. Médicaments — Formulaire, calcul et enregistrement

| | |
| --- | --- |
| Fonctionnalités | Poste ERP, produit, patient, montant |
| Attendu | Enregistrement MySQL ; sync_status `BLOCKED_*` ou `SUSPENDED` ; **outbox intention** ; pas de sortie stock Beach |
| Sync | Intention préparée ; réplication **non autorisée** |
| Risque | 🟡 |

- [ ] Validé

## 15. Prestations — Formulaire, patient, tarifs et enregistrement

| | |
| --- | --- |
| Fonctionnalités | Tarif / montant, PatientPicker, médecin, service |
| Attendu | Même politique sync que médicaments (plus de `LOCAL_ONLY` silencieux) |
| Anomalies | IDMVT / règles tarifaires historiques ouvertes |
| Risque | 🟡 |

- [ ] Validé

## 16. Synchronisation — Outbox, états, rapprochement

| | |
| --- | --- |
| Fonctionnalités | Centre sync, KPIs, prepare intentions, import |
| Attendu | Contrats réplicables ; autres entités visibles / suspendues |
| Risque | 🟢 supervision ; 🔴 push Beach hors TCONTRAT |

- [ ] Validé

## 17. Dashboard — Indicateurs, filtres et graphiques

| | |
| --- | --- |
| Source | Tables relationnelles MySQL |
| Attendu | Montants prestations/médicaments, filtres dates ; **pas** d’encaissement inventé |
| Risque | 🟢 |

- [ ] Validé

## 18. RBAC — Utilisateur non ADMIN

| | |
| --- | --- |
| Parcours | Compte restreint : menus/boutons/API 403 |
| Attendu | DENY BY DEFAULT ; 403 ≠ liste vide |
| Risque | 🟢 |

- [ ] Validé

## 19. Ergonomie — Cohérence globale et responsive

| | |
| --- | --- |
| Attendu | Shell navy, accent facturation, pas de `window.confirm`, pas de texte technique |
| Risque | 🟢 |

- [ ] Validé

---

## Opérations sûres vs isolées

**Sans risque production (🟢)** : navigation, recherches, dashboard, supervision outbox en lecture, tests RBAC lecture.

**ERP contrôlé (🟡)** : CRUD référentiels MySQL, mappings, enregistrement prestations/médicaments ERP (intentions suspendues, **pas** d’écriture Beach hors TCONTRAT).

**Recette isolée uniquement (🔴)** : activation WRITE Beach étendue, INSERT/UPDATE historiques, scénarios de conflit destructifs, toute manipulation SQL Server hors liste blanche.

---

## Après la recette

1. Noter anomalies UI / workflows hospitaliers.  
2. Décider activation progressive réplications **entité par entité**.  
3. Ne pas étendre `WRITE_ENTITIES` sans preuve IDMVT/IDM/STOCK.
