# CLINIQUE Migration — Rapport maître (réécriture intégrale)

Date d’audit final : **2026-11-25**  
Base MySQL cible : **`cmkerp-v24prod`** (port Docker `32768`, utilisateur `shad`)  
Écritures Beach : **`WRITE_ENTITIES=TCONTRAT` uniquement**  
CH / Mediline : **aucune écriture** dans ce chantier  
Flyway runtime : souvent **désactivé** — l’état réel = tables/colonnes observées en MySQL, pas seulement les fichiers `V32`/`V33`

Ce document **remplace** toute synthèse antérieure. Les mentions « Lots 2–6 non démarrés » sont **obsolètes** et ne doivent plus être citées.

---

## 1. Synthèse exécutive par lot

Légende des colonnes :

| Abrégé | Signification |
| --- | --- |
| Impl. | Code livré dans le dépôt |
| MySQL | Structures + volumes observés live |
| Testé | Tests / compile / SQL exécutés |
| Recette UI | Validation fonctionnelle utilisateur |
| Prod OK | Opérationnel pour usage ERP contrôlé |
| Répl. préparée | Intention outbox / adaptateur prêt |
| Répl. autorisée | Liste blanche Beach active |
| Bloqué | Attente métier / env / règle historique |

| Lot | Thème | Impl. | MySQL | Testé | Recette UI | Prod OK | Répl. préparée | Répl. autorisée | Bloqué |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | Outbox, flags, WriteGuard, compliance | Oui | Oui | Oui | Partiel | Oui (socle) | Oui | N/A | Recette SQL Server isolée |
| 1 | Contrats CRUD / import / réplication | Oui | 16 | Oui + live | **Live démontré** | Oui | Oui | **TCONTRAT** | Ext. hors CODE/DESIGNATION |
| 2 | Services, Imputations, Locations | Oui | 43 / 49 / 3 | SQL + compile | À faire | Oui (ERP) | Import + outbox générique | Non | Writes Beach |
| 3 | Sociétés, Classes, Tarifs | Oui | 484 / 24 / 1977 | SQL + FK | À faire | Oui (ERP) | Import | Non | Writes Beach |
| 4 | Médecins, Ayants, Users, Mediline | Oui | 140 / 30412 / 18 map | SQL + compile | À faire | Oui (ERP) | Mapping users | Non | Liaison Mediline 0 LINKED |
| 5 | Pharmacies, Produits, Stock, Médicaments | Partiel | 7 map / 5177 prod map / stock 0 / médic. tx 0 | Compile | À faire | ERP partiel | Intention médic. **SUSPENDED** | Non | STOCK1…n, IDM, PAU |
| 6 | Prestations | Oui (ERP) | 0 lignes tx | Compile | À faire | ERP local + intention | **Outbox SUSPENDED** | Non | IDMVT, tarifs histo. |
| Dash | Dashboard financier relationnel | Oui | Agrégats erp_* | Compile | À faire | Oui (lecture) | N/A | N/A | Interprétation encaissement |

---

## 2. Architecture retenue

| Couche | Rôle |
| --- | --- |
| Tables métier `erp_*` | **Source de vérité** CMK ERP (listes, CRUD, FK, dashboard, postes facturation) |
| `erp_clinic_referentials` | **Pont d’import temporaire** / miroir technique — **pas** une SoT concurrente |
| `erp_sync_outbox` | Intentions ERP → Beach (`PENDING` / `SUSPENDED` / …) |
| CLINIQUE Beach | Historique compatible ; DML gated ; **DELETE/DDL interdits** |
| CLINIQUE CH | Lecture éventuelle ; **write=false** |
| Mediline | Recherche patient **lecture seule** |

### Cycle de vie de `erp_clinic_referentials`

1. **Aujourd’hui** : cible des imports sélectifs CLINIQUE → MySQL ; alimente encore certains écrans de sync.  
2. **Migration** : V32 a peuplé les tables typées depuis ce pont (services, sociétés, tarifs, ayants…).  
3. **Cible** : lectures métier 100 % `erp_*` ; le pont ne sert plus qu’à l’import / fingerprint.  
4. **Retrait** : uniquement après preuve que plus aucun écran/API métier ne lit le JSON bag — **aucune suppression de données production** dans cet audit.

---

## 3. Inventaire MySQL live (`cmkerp-v24prod`)

### 3.1 Tables `erp_*` présentes

`erp_ayants_droit`, `erp_classes`, `erp_clinic_referentials`, `erp_contrats`, `erp_facturation_medicaments`, `erp_facturation_prestations`, `erp_imputations`, `erp_locations`, `erp_medecins`, `erp_pharmacy_external_mappings`, `erp_product_external_mappings`, `erp_services`, `erp_societes`, `erp_stock_clinique`, `erp_sync_outbox`, `erp_tarifs`, `erp_user_external_mappings`

### 3.2 Volumes (requêtes live 2026-11-25)

| Entité | Volume |
| --- | ---: |
| Services | 43 |
| Imputations | 49 |
| Locations | 3 |
| Médecins | 140 |
| Sociétés | 484 |
| Classes | 24 |
| Tarifs | 1 977 |
| Ayants droit | 30 412 |
| Ayants `patient_link_status=LINKED` | **0** |
| Contrats | 16 |
| User mappings | 18 |
| Pharmacy mappings | 7 |
| Product external mappings | 5 177 |
| Stock clinique | 0 |
| Prestations ERP | 0 |
| Médicaments ERP (tx) | 0 |
| Referentials (pont) | 46 983 |
| Outbox | 2 |

### 3.3 Migrations V32 / V33 — état effectif

| Migration | Fichier dépôt | Appliquée live ? | Preuve |
| --- | --- | --- | --- |
| V32 tables relationnelles | `V32__erp_facturation_relational_tables.sql` (+ scripts manuels) | **Oui** | Tables listées + volumes + FK |
| V33 patient link | `V33__erp_ayants_patient_link_status.sql` | **Oui** | Colonnes `patient_link_status`, `mediline_ipp`, `mediline_patient_id` sur `erp_ayants_droit` |

**Ne pas** assumer Flyway history : en environnement actuel, appliquer via SQL non destructif (`CREATE IF NOT EXISTS` / `ADD COLUMN` gardés) si une base satellite n’a pas encore les objets.

### 3.4 Clés étrangères MySQL confirmées

- `erp_societes.fk_contrat` → `erp_contrats`  
- `erp_classes.fk_service` → `erp_services`  
- `erp_tarifs` → service / classe / imputation  
- `erp_ayants_droit` → société, contrat, location, titulaire  

---

## 4. Politique de synchronisation (correction LOCAL_ONLY)

| Situation | Statut attendu | Implémentation actuelle |
| --- | --- | --- |
| Donnée exclusivement ERP | `LOCAL_ONLY` | Référentiels non destinés à Beach |
| Opération destinée à CLINIQUE, écriture interdite | `SUSPENDED` (+ outbox) | **Contrats** ; **prestations / médicaments** (audit final) |
| Mapping utilisateur manquant | `BLOCKED_MAPPING` | Postes facturation |
| Règles historiques IDMVT/IDM non résolues | `BLOCKED_RULES` | Postes facturation (intention outbox quand même en `SUSPENDED`) |
| Whitelist + règles OK | `PENDING` | Uniquement si entité dans `WRITE_ENTITIES` |
| Répliqué vérifié | `SYNCED` | Contrats (worker TCONTRAT) |
| Divergence | `CONFLICT` | Centre de sync |

**Correction audit final :** les créations prestations / médicaments **ne restent plus en `LOCAL_ONLY` silencieux**. Elles journalisent une intention `erp_sync_outbox` (`SUSPENDED`). Le worker **n’exécute que `TCONTRAT`** — aucune extension automatique de la liste blanche.

---

## 5. Formulaires et relations

| Écran | Sélecteurs / libellés | État |
| --- | --- | --- |
| Contrats | CRUD + import + sync | Opérationnel (live) |
| Sociétés / Classes / Tarifs / Services / Imputations / Locations / Médecins / Ayants | Libellés FK + ComboBox / pickers | Implémenté — à recetter |
| PatientPicker (Mediline) | IPP / nom ; persistance IPP + `patient_link_status` | Implémenté — **0 LINKED live** à valider |
| Mapping utilisateurs / pharmacies | API + UI ; pas de `MOTDEPASSE` | Implémenté — à recetter |
| Prestations / Médicaments | Postes ERP + PatientPicker + montants | Implémenté — intention SUSPENDED |
| Dashboard | Agrégats `erp_*` | Implémenté — à recetter |
| Sync centre | Outbox + referentials | Opérationnel |

---

## 6. Adaptateurs Beach

| Entité | INSERT/UPDATE Beach | Whitelist prod |
| --- | --- | --- |
| `TCONTRAT` | Oui (`ClinicBeachOutboxWorker`) | **Oui** |
| Autres (`TPRESTATION`, `TMEDICAMENT`, …) | **Non exécutés** ; intentions éventuelles suspendues | **Non** |

---

## 7. Blocages métier restants (légitimes)

Voir `CLINIQUE_HISTORICAL_RULES_CARTOGRAPHY.md` :

1. Correspondance `STOCK1`…`STOCKn` ↔ pharmacies / dépôts / locations — **non déterminée** (pas inventée).  
2. Génération `IDMVT` / `IDM`.  
3. Tarification contractuelle prestations (K, SERIE, AFACT, CODECMK…).  
4. Valorisation médicaments (PAU, QTE, MONTANT, UNITGEST).  
5. Activation progressive WRITE hors `TCONTRAT`.  
6. Recette SQL Server isolée — **ouverte / bloquée** faute d’environnement adapté.

---

## 8. Sécurité non négociable (état effectif)

```
CLINIC_BEACH_WRITE_ENTITIES=TCONTRAT
CLINIC_BEACH_DELETE_ENABLED=false
CLINIC_CH_WRITE_ENABLED=false
```

- Aucun DDL Beach/CH.  
- Mediline lecture seule.  
- MySQL : pas de migration destructive dans cet audit.

---

## 9. Commandes de mise à jour (sans déploiement auto)

```powershell
cd C:\Users\HP\Videos\STSDEV\cmkerp
git pull
# Si le JAR gateway est verrouillé : arrêter proprement le process, puis :
mvn -pl cmkerp-shared-kernel,cmkerp-gateway -am -DskipTests package
# Redémarrer GatewayApplication (STS / service)

cd C:\Users\HP\Videos\STSDEV\consolenotification
git pull
npm run build   # ou npm run dev
```

Vérification SQL non destructive V33 (si colonne absente sur une autre base) :

```sql
-- Voir fichier V33__erp_ayants_patient_link_status.sql
-- CREATE/ALTER IF NOT EXISTS uniquement ; aucune DROP.
```

---

## 10. Recette utilisateur

Checklist écran par écran :  
[`CLINIQUE_FINAL_USER_ACCEPTANCE.md`](./CLINIQUE_FINAL_USER_ACCEPTANCE.md)

Tracker autonome :  
`cmkerp/docs/integration/CLINIQUE_AUTONOMOUS_EXECUTION_TRACKER.md`

---

## 11. Verdict de clôture technique

| Question | Réponse |
| --- | --- |
| Socle relationnel livré ? | **Oui** (MySQL live + code) |
| Clôture définitive Facturation V1 ? | **Non** — recette utilisateur + règles historiques + WRITE hors contrats |
| Prêt pour prise en main recette ? | **Oui** pour parcours ERP (contrats, référentiels, postes, dashboard, mappings) |
| Prêt pour étendre Beach ? | **Non** sans décision métier explicite |
