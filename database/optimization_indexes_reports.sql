-- =====================================================
-- OPTIMISATION DES INDEXES POUR LES RAPPORTS
-- =====================================================
-- Ce script améliore les performances des requêtes SQL
-- utilisées pour générer les rapports PDF
-- =====================================================

-- 1. Index pour produits avec stock (requête principale)
-- Optimise les JOINs entre produits et stock_produits
CREATE INDEX IF NOT EXISTS idx_stock_produits_fk ON stock_produits(fkProduits, fkPharmacies, operationnel);
CREATE INDEX IF NOT EXISTS idx_stock_produits_pharmacie ON stock_produits(fkPharmacies, operationnel, qte);

-- 2. Index pour catégories et droits
-- Optimise les JOINs avec droits_categorie
CREATE INDEX IF NOT EXISTS idx_produits_categorie ON produits(fkCategorie);
CREATE INDEX IF NOT EXISTS idx_droits_categorie_composite ON droits_categorie(fkCategorie, fkPharmacie);

-- 3. Index pour recherche de produits
-- Optimise les recherches par nom commercial, nom scientifique, code-barres
CREATE INDEX IF NOT EXISTS idx_produits_nomcommercial ON produits(nomcommercial);
CREATE INDEX IF NOT EXISTS idx_produits_nomscientifique ON produits(nomscientifique);
CREATE INDEX IF NOT EXISTS idx_produits_codebarre ON produits(codebarre);

-- 4. Index pour péremption
-- Optimise les requêtes sur perimable_alerte_stock
CREATE INDEX IF NOT EXISTS idx_perimable_alerte_stock_fk ON perimable_alerte_stock(fkStock, notifactif);
CREATE INDEX IF NOT EXISTS idx_perimable_alerte_stock_date ON perimable_alerte_stock(dateperemtion, notifactif);

-- 5. Index pour références (formes, dosages, conditionnements)
-- Optimise les JOINs avec les tables de référence
CREATE INDEX IF NOT EXISTS idx_produits_forme ON produits(fkForme);
CREATE INDEX IF NOT EXISTS idx_produits_dosage ON produits(fkDosage);
CREATE INDEX IF NOT EXISTS idx_produits_conditionnement ON produits(fkConditionnement);

-- 6. Index composite pour recherche avancée
-- Optimise les recherches combinées
CREATE INDEX IF NOT EXISTS idx_produits_search_composite ON produits(nomcommercial, nomscientifique, codebarre, fkCategorie);

-- 7. Index pour tri et pagination
-- Optimise ORDER BY et LIMIT
CREATE INDEX IF NOT EXISTS idx_produits_id_desc ON produits(id DESC);
CREATE INDEX IF NOT EXISTS idx_produits_nomcommercial_asc ON produits(nomcommercial ASC);

-- =====================================================
-- VÉRIFICATION DES INDEXES
-- =====================================================
-- Exécuter cette requête pour vérifier que les indexes sont créés :
-- SHOW INDEX FROM stock_produits;
-- SHOW INDEX FROM produits;
-- SHOW INDEX FROM droits_categorie;
-- SHOW INDEX FROM perimable_alerte_stock;
-- =====================================================

-- =====================================================
-- ANALYSE DES PERFORMANCES
-- =====================================================
-- Après création des indexes, analyser les tables :
-- ANALYZE TABLE produits;
-- ANALYZE TABLE stock_produits;
-- ANALYZE TABLE droits_categorie;
-- ANALYZE TABLE perimable_alerte_stock;
-- =====================================================

