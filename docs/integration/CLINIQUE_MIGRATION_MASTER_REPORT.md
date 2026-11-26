# CLINIQUE Migration — Rapport maître

Date : **2026-11-26**  
Base MySQL : **`cmkerp-v24prod`**  
CH / Mediline structure : **aucune modification**  
DELETE Beach : **interdit**

---

## 0. Directive permanente (2026-11-26)

**Autorisation métier globale** INSERT/UPDATE Beach pour les 15 entités métier à équivalent historique.

**Activation technique** = allow-list `CLINIC_BEACH_WRITE_ENTITIES` (jamais `*`) + adaptateur conforme + tests.

Référence Cursor : `.cursor/rules/erp-mandatory-legacy-replication.mdc` (alwaysApply).

Ne pas confondre :

| Notion | Signification |
| --- | --- |
| Autorisation métier | Accordée pour les 15 entités |
| Adaptateur implémenté | Code INSERT/UPDATE + outbox présents |
| Adaptateur testé | Preuve technique (isolée / recette) |
| Activation technique | Présence dans `write-entities` |
| Réplication réelle vérifiée | Ligne lue dans Beach + statut `SYNCED` |
| Recette utilisateur | Validation métier humaine |

---

## 1. Matrice des 15 entités

| Entité CLINIQUE | Table ERP | Autorisation métier | Adaptateur | Testé | Activation technique | Réplication vérifiée | Blocage / note |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TCONTRAT | `erp_contrats` | Oui | Oui | Oui (live) | **Oui** | Oui (historique) | — |
| TSOCIETE | `erp_societes` | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | Mapping Mediline requis (Chantier 02) |
| TAYANTDROIT | `erp_ayants_droit` | Oui | Oui (worker + verify) | À recetter | **Oui** (env) | À confirmer | IPP ≤ 10 / matricule ; pas de reprise mass SUSPENDED |
| TSERVICE | `erp_services` | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TCLASSE | `erp_classes` | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TIMPUTATION | `erp_imputations` | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TARIF | `erp_tarifs` | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TLOCATION | `erp_locations` | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TMEDECIN | `erp_medecins` | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TUTILISATEUR | mapping users | Oui | Writer (sans MDP) | Partiel | **Oui** (env) | À confirmer | Jamais `MOTDEPASSE` |
| TPHARMACIE | mapping pharmacies | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TPRODUIT | mapping produits | Oui | Writer générique | Partiel | **Oui** (env) | À confirmer | — |
| TSTOCK | `erp_stock_clinique` | Oui | Non final | Non | **Non** | Non | STOCK1…n / IDM / concurrence |
| TMEDICAMENT | `erp_facturation_medicaments` | Oui | Non final | Non | **Non** | Non | IDMVT / tarification / double mouvement |
| TPRESTATION | `erp_facturation_prestations` | Oui | Non final | Non | **Non** | Non | IDMVT / signature / double facturation |

Allow-list technique actuelle (recette locale) :

```
TCONTRAT,TSOCIETE,TAYANTDROIT,TSERVICE,TCLASSE,TIMPUTATION,TARIF,TLOCATION,TMEDECIN,TUTILISATEUR,TPHARMACIE,TPRODUIT
```

---

## 2. Lots 0–6 (rappel volumes)

| Lot | État | Notes |
| --- | --- | --- |
| 0 Infrastructure | Implémenté | Recette SQL Server isolée encore ouverte |
| 1 Contrats | Opérationnel live | Réplication Beach démontrée |
| 2–3 Référentiels | Prêt UAT | Services / sociétés / classes / tarifs / locations |
| 4 Acteurs / Mediline | Prêt UAT + rapprochement | ~29 100 LINKED |
| 5 Pharmacie / stock | Partiel | Compartiments ; TSTOCK non activé |
| 6 Prestations / médicaments | Import histo MySQL | TPRESTATION / TMEDICAMENT non activés en écriture Beach |
| Chantier 02 | MVP livré | DMI → affiliation → Mediline → ERP → TAYANTDROIT (à recetter) |

---

## 3. Protections non négociables

- Aucun DDL CLINIQUE / Mediline  
- Aucun DELETE Beach  
- Aucune écriture CH  
- Outbox transactionnelle MySQL  
- Idempotence + relecture avant `SYNCED`  
- Pas de rattrapage massif des `SUSPENDED`  

---

## 4. Priorité immédiate

1. Mapping société ERP ↔ compte Mediline (au moins une société de test)  
2. Premier dossier DMI justifié + ayant ERP  
3. Vérifier intention Outbox + réplication `TAYANTDROIT` après activation  
4. Puis formulaire suivant, un par un  

---

## 5. Commandes de mise à jour

```powershell
cd C:\Users\HP\Videos\STSDEV\cmkerp
git pull
# Arrêter le gateway si JAR verrouillé
mvn -pl cmkerp-shared-kernel,cmkerp-gateway -am -DskipTests package
powershell -File .\run-gateway.ps1 -Background -SkipBuild -KillPort

cd C:\Users\HP\Videos\STSDEV\consolenotification
git pull
npm run build
```

UAT utilisateur : `CLINIQUE_FINAL_USER_ACCEPTANCE.md`.
