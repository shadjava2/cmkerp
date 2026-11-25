# CLINIQUE Migration — Rapport maître consolidé

Date : **2026-03-25** (reprise autonome post-V32)  
Écritures Beach : **`WRITE_ENTITIES=TCONTRAT` uniquement**. CH / Mediline écriture : **interdites**.

## Architecture — source de vérité

| Couche | Rôle |
| --- | --- |
| Tables `erp_*` typées | **Source de vérité métier** CMK ERP (CRUD, listes, dashboard, facturation) |
| `erp_clinic_referentials` | Pont d’import / compatibilité temporaire — synchronisé en miroir, plus seule vérité |
| CLINIQUE Beach | Système historique compatible ; WRITE gated `TCONTRAT` |
| Mediline | Référentiel patient (lecture) |

## Volumes live (cmkerp-v24prod)

| Entité | Count |
| --- | ---: |
| Ayants droit | 30 412 |
| Sociétés | 484 |
| Tarifs | 1 977 |
| Services | 43 |
| User mappings | 18 |
| Pharmacy mappings | 7 |
| Product MATCHED | 1 966 |
| Prestations ERP locales | 0 → croissant via poste de travail |
| Médicaments ERP locaux | 0 → croissant via poste de travail |
| Ayants avec IPP Mediline | via PatientPicker + `patient_link_status` |

## Livré dans cette reprise

1. V33 `patient_link_status` + persistance Mediline sur update/create ayants  
2. CREATE/UPDATE relationnels (IDs `erp_*` = vérité)  
3. Mapping utilisateurs + pharmacies (API + UI)  
4. Postes de facturation prestations & médicaments (ERP LOCAL_ONLY)  
5. ProduitPicker / recherche produits  
6. Dashboard agrégé relationnel  
7. RBAC : `FACTURATION_PRESTATIONS_CREATE`, `FACTURATION_MEDICAMENTS_CREATE` + catalogue API  
8. PatientPicker branché + colonne état liaison Mediline  

## Commits

- Backend : chaîne `e1a8b20` → `101f879` + commit de reprise  
- Frontend : `a284f41` → `1fdb813` + commit de reprise  

## Distinctions

| Capacité | Implémenté | Testé | Validé prod | Autorisé Beach |
| --- | --- | --- | --- | --- |
| Tables relationnelles V32/V33 | Oui | SQL + compile | Oui (DB) | N/A |
| Libellés FK | Oui | Compile | À recetter UI | N/A |
| PatientPicker + IPP | Oui | Compile | À recetter UI | N/A |
| Mapping users/pharmacies | Oui | Compile | À recetter UI | N/A |
| Facturation prestations ERP | Oui | Compile | À recetter | **Non** (LOCAL_ONLY) |
| Facturation médicaments ERP | Oui | Compile | À recetter | **Non** + pas de stock double |
| Adaptateurs Beach INSERT autres | Préparé / non | Isolé manquant | Non | **Non** |
| Recette SQL Server isolée Lots 0/1 | Non | — | — | **Bloqué** (pas d’env isolé) |

## Commandes de mise à jour

```powershell
cd C:\Users\HP\Videos\STSDEV\cmkerp
git pull
# Arrêter le gateway si JAR verrouillé, puis :
mvn -pl cmkerp-shared-kernel,cmkerp-gateway -am -DskipTests package
# Redémarrer GatewayApplication

cd C:\Users\HP\Videos\STSDEV\consolenotification
git pull
npm run dev
```

## Décisions métier encore nécessaires

- Activation WRITE Beach hors TCONTRAT (entité par entité)  
- Règles de calcul historique TMEDICAMENT / IDMVT concurrence  
- Correspondances STOCK1…n ↔ pharmacies/dépôts  
- Rapprochement automatique ayants ↔ Mediline (interdit sur nom seul)  
