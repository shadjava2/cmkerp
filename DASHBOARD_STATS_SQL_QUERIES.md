# 📊 REQUÊTES SQL - DASHBOARD STATS

## Vue d'ensemble
Ce document contient toutes les requêtes SQL utilisées pour calculer les statistiques du dashboard Stock.

**Fichier source** : `cmkerp-platform/src/main/java/cd/shad/erp/cmk/cmkerp/platform/inventory/infrastructure/persistence/InventoryDashboardRepository.java`

---

## 1️⃣ RUPTURE DE STOCK

**Méthode** : `countRuptureStock(Long pharmacieId)`

**Critères** :
- `st.operationnel = TRUE`
- `st.qte <= p.qtcritique OR st.qte <= 0`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT st.id)
FROM stock_produits st
INNER JOIN produits p ON st.fkProduits = p.id
WHERE st.operationnel = TRUE
  AND (st.qte <= p.qtcritique OR st.qte <= 0)
  -- Optionnel : AND st.fkPharmacies = :pharmacieId
```

**Optimisations possibles** :
- Index sur `stock_produits(operationnel, qte, fkPharmacies)`
- Index sur `produits(qtcritique)`
- Index composite sur `stock_produits(fkPharmacies, operationnel, qte)`

---

## 2️⃣ PÉRIMÉ DANS 3 MOIS

**Méthode** : `countPerimeDans3Mois(Long pharmacieId)`

**Critères** :
- `pas.notifactif = TRUE`
- `pas.dateperemtion BETWEEN CURDATE() AND :dans3Mois`
- `st.operationnel = TRUE`
- `st.qte > 0`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT pas.fkStock)
FROM perimable_alerte_stock pas
INNER JOIN stock_produits st ON pas.fkStock = st.id
WHERE pas.notifactif = TRUE
  AND pas.dateperemtion BETWEEN CURDATE() AND :dans3Mois
  AND st.operationnel = TRUE
  AND st.qte > 0
  -- Optionnel : AND st.fkPharmacies = :pharmacieId
```

**Paramètres** :
- `:dans3Mois = CURDATE() + INTERVAL 3 MONTH`

**Optimisations possibles** :
- Index sur `perimable_alerte_stock(notifactif, dateperemtion, fkStock)`
- Index sur `stock_produits(id, operationnel, qte, fkPharmacies)`

---

## 3️⃣ PÉRIMÉ DANS 1 MOIS

**Méthode** : `countPerimeDans1Mois(Long pharmacieId)`

**Critères** :
- `pas.notifactif = TRUE`
- `pas.dateperemtion BETWEEN CURDATE() AND :dans1Mois`
- `st.operationnel = TRUE`
- `st.qte > 0`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT pas.fkStock)
FROM perimable_alerte_stock pas
INNER JOIN stock_produits st ON pas.fkStock = st.id
WHERE pas.notifactif = TRUE
  AND pas.dateperemtion BETWEEN CURDATE() AND :dans1Mois
  AND st.operationnel = TRUE
  AND st.qte > 0
  -- Optionnel : AND st.fkPharmacies = :pharmacieId
```

**Paramètres** :
- `:dans1Mois = CURDATE() + INTERVAL 1 MONTH`

**Optimisations possibles** :
- Même que "Périmé dans 3 mois"
- Index composite sur `perimable_alerte_stock(notifactif, dateperemtion, fkStock)`

---

## 4️⃣ ACHAT RISQUÉ

**Méthode** : `countAchatRisque(Long pharmacieId)`

**Critères** :
- `pas.notifactif = TRUE`
- `pas.fkAprov IS NOT NULL AND pas.fkAprov != 0`
- `DATE(pas.datecreate) >= :ilYUnAn`
- `DATEDIFF(pas.dateperemtion, pas.datecreate) <= 365`
- `st.operationnel = TRUE`
- `st.qte > 0`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT pas.fkStock)
FROM perimable_alerte_stock pas
INNER JOIN stock_produits st ON pas.fkStock = st.id
WHERE pas.notifactif = TRUE
  AND pas.fkAprov IS NOT NULL
  AND pas.fkAprov != 0
  AND DATE(pas.datecreate) >= :ilYUnAn
  AND DATEDIFF(pas.dateperemtion, pas.datecreate) <= 365
  AND st.operationnel = TRUE
  AND st.qte > 0
  -- Optionnel : AND st.fkPharmacies = :pharmacieId
```

**Paramètres** :
- `:ilYUnAn = CURDATE() - INTERVAL 1 YEAR`

**Optimisations possibles** :
- Index sur `perimable_alerte_stock(notifactif, fkAprov, datecreate, dateperemtion, fkStock)`
- Calculer `DATEDIFF` dans une colonne calculée ou vue matérialisée

---

## 5️⃣ STOCK DORMANT

**Méthode** : `countStockDormant(Long pharmacieId)`

**Critères** :
- `st.operationnel = TRUE`
- `st.qte > 0`
- `DATE(st.dateupdate) < :ilYA6Mois`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT st.id)
FROM stock_produits st
WHERE st.operationnel = TRUE
  AND st.qte > 0
  AND DATE(st.dateupdate) < :ilYA6Mois
  -- Optionnel : AND st.fkPharmacies = :pharmacieId
```

**Paramètres** :
- `:ilYA6Mois = CURDATE() - INTERVAL 6 MONTH`

**Optimisations possibles** :
- Index sur `stock_produits(operationnel, qte, dateupdate, fkPharmacies)`
- Index composite pour éviter le scan complet

---

## 6️⃣ STOCKS LES PLUS MOUVEMENTÉS

**Méthode** : `countStockPlusMouvementes(Long pharmacieId)`

**Critères** :
- `st.operationnel = TRUE`
- `st.qte > 0`
- `DATE(st.dateupdate) >= :ilYA7Jours`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT st.id)
FROM stock_produits st
WHERE st.operationnel = TRUE
  AND st.qte > 0
  AND DATE(st.dateupdate) >= :ilYA7Jours
  -- Optionnel : AND st.fkPharmacies = :pharmacieId
```

**Paramètres** :
- `:ilYA7Jours = CURDATE() - INTERVAL 7 DAY`

**Optimisations possibles** :
- Index sur `stock_produits(operationnel, qte, dateupdate, fkPharmacies)`
- Index sur `dateupdate` pour accès rapide aux stocks récents

---

## 7️⃣ STOCKS LES MOINS MOUVEMENTÉS

**Méthode** : `countStockMoinsMouvementes(Long pharmacieId)`

**Critères** :
- `st.operationnel = TRUE`
- `st.qte > 0`
- `DATE(st.dateupdate) < :ilYA3Mois`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT st.id)
FROM stock_produits st
WHERE st.operationnel = TRUE
  AND st.qte > 0
  AND DATE(st.dateupdate) < :ilYA3Mois
  -- Optionnel : AND st.fkPharmacies = :pharmacieId
```

**Paramètres** :
- `:ilYA3Mois = CURDATE() - INTERVAL 3 MONTH`

**Optimisations possibles** :
- Index sur `stock_produits(operationnel, qte, dateupdate, fkPharmacies)`

---

## 8️⃣ FOURNISSEURS

**Méthode** : `countFournisseurs()`

**Requête SQL** :
```sql
SELECT COUNT(*) FROM fournisseurs
```

**Optimisations possibles** :
- Index sur `fournisseurs(id)` (généralement déjà présent en PK)

---

## 9️⃣ DEMANDES EN ATTENTE

**Méthode** : `countDemandesEnAttente(Long pharmacieId)`

**Critères** :
- `statut = 'EN ATTENTE'`

**Requête SQL** :
```sql
SELECT COUNT(*)
FROM requisitions
WHERE statut = 'EN ATTENTE'
  -- Optionnel : AND fkPharmacie = :pharmacieId
```

**Optimisations possibles** :
- Index sur `requisitions(statut, fkPharmacie)`
- Index composite pour filtrage rapide

---

## 🔟 RÉCEPTIONS EN ATTENTE

**Méthode** : `countReceptionEnAttente(Long pharmacieId)`

**Critères** :
- `rs.statut = 'EN ATTENTE'`
- Jointure via `reception_stock -> transferts_stock -> requisitions`

**Requête SQL** :
```sql
SELECT COUNT(DISTINCT rs.id)
FROM reception_stock rs
INNER JOIN transferts_stock ts ON rs.fkTransfert = ts.id
INNER JOIN requisitions r ON ts.fkRequisition = r.id
WHERE rs.statut = 'EN ATTENTE'
  -- Optionnel : AND r.fkPharmacie = :pharmacieId
```

**Optimisations possibles** :
- Index sur `reception_stock(statut, fkTransfert)`
- Index sur `transferts_stock(fkRequisition, id)`
- Index sur `requisitions(fkPharmacie, id)`
- Index composite pour optimiser les JOINs

---

## 📈 OPTIMISATIONS GLOBALES RECOMMANDÉES

### Index suggérés

```sql
-- Stock produits
CREATE INDEX idx_stock_operationnel_qte_pharmacie
ON stock_produits(operationnel, qte, fkPharmacies);

CREATE INDEX idx_stock_dateupdate_pharmacie
ON stock_produits(dateupdate, fkPharmacies, operationnel, qte);

-- Périmable alerte stock
CREATE INDEX idx_perimable_notifactif_date
ON perimable_alerte_stock(notifactif, dateperemtion, fkStock);

CREATE INDEX idx_perimable_achat_risque
ON perimable_alerte_stock(notifactif, fkAprov, datecreate, dateperemtion, fkStock);

-- Requisitions
CREATE INDEX idx_requisitions_statut_pharmacie
ON requisitions(statut, fkPharmacie);

-- Reception stock
CREATE INDEX idx_reception_statut_transfert
ON reception_stock(statut, fkTransfert);

-- Transferts stock
CREATE INDEX idx_transferts_requisition
ON transferts_stock(fkRequisition, id);
```

### Vues matérialisées (optionnel)

Pour des performances encore meilleures, créer des vues matérialisées rafraîchies périodiquement :

```sql
CREATE MATERIALIZED VIEW mv_dashboard_stats AS
SELECT
  -- Calculer toutes les stats en une seule requête
  ...
;

-- Rafraîchir toutes les heures
CREATE EVENT refresh_dashboard_stats
ON SCHEDULE EVERY 1 HOUR
DO
  REFRESH MATERIALIZED VIEW mv_dashboard_stats;
```

---

## 🔍 NOTES IMPORTANTES

1. **Toutes les requêtes utilisent `COUNT(DISTINCT ...)`** pour éviter les doublons
2. **Le filtre `pharmacieId` est optionnel** - si `null`, toutes les pharmacies sont incluses
3. **Les dates sont calculées dynamiquement** dans le code Java (pas dans SQL)
4. **Les JOINs sont optimisés** avec des index appropriés
5. **Les requêtes de rapport** (find*) sont plus complexes avec GROUP_CONCAT pour les péremptions

---

**Dernière mise à jour** : 2025-01-XX
**Version** : CMK-ERP 4.1.1

