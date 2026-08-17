-- ==========================================
-- FLYWAY MIGRATION V7 - Fonctions MySQL Personnalisées
-- ==========================================
-- Fonctions réutilisables pour les calculs métier courants
-- ==========================================

DELIMITER $$

-- ==========================================
-- FONCTION: Calcul de prix avec remise
-- ==========================================
-- Calcule le prix final après application d'une remise
--
-- Paramètres:
--   p_prix_base DECIMAL(10,2) : Prix de base
--   p_remise_percent DECIMAL(5,2) : Pourcentage de remise (ex: 10.00 pour 10%)
--
-- Retourne:
--   DECIMAL(10,2) : Prix final après remise
--
-- Exemple:
--   SELECT FN_CALCULER_PRIX_AVEC_REMISE(100.00, 15.00) AS prix_final;
--   Résultat: 85.00
-- ==========================================
DROP FUNCTION IF EXISTS FN_CALCULER_PRIX_AVEC_REMISE$$
CREATE FUNCTION FN_CALCULER_PRIX_AVEC_REMISE(
    p_prix_base DECIMAL(10,2),
    p_remise_percent DECIMAL(5,2)
) RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_prix_final DECIMAL(10,2);

    -- Validation des paramètres
    IF p_prix_base IS NULL OR p_prix_base < 0 THEN
        RETURN 0.00;
    END IF;

    IF p_remise_percent IS NULL OR p_remise_percent < 0 THEN
        RETURN p_prix_base;
    END IF;

    IF p_remise_percent > 100.00 THEN
        SET p_remise_percent = 100.00;
    END IF;

    -- Calcul: prix_base * (1 - remise_percent / 100)
    SET v_prix_final = p_prix_base * (1 - p_remise_percent / 100.00);

    -- Arrondir à 2 décimales
    RETURN ROUND(v_prix_final, 2);
END$$

-- ==========================================
-- FONCTION: Vérification de péremption
-- ==========================================
-- Vérifie si un produit est périmé ou proche de la péremption
--
-- Paramètres:
--   p_date_peremption DATE : Date de péremption du produit
--   p_jours_alerte INT : Nombre de jours avant péremption pour alerter (défaut: 30)
--
-- Retourne:
--   VARCHAR(20) : Statut de péremption
--     - 'PERIME' : Produit périmé
--     - 'ALERTE' : Produit proche de la péremption
--     - 'OK' : Produit valide
--
-- Exemple:
--   SELECT FN_VERIFIER_PEREMPTION('2025-02-01', 30) AS statut;
--   Résultat: 'ALERTE' (si on est en janvier 2025)
-- ==========================================
DROP FUNCTION IF EXISTS FN_VERIFIER_PEREMPTION$$
CREATE FUNCTION FN_VERIFIER_PEREMPTION(
    p_date_peremption DATE,
    p_jours_alerte INT
) RETURNS VARCHAR(20)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_jours_restants INT;
    DECLARE v_jours_alerte INT DEFAULT 30;

    -- Validation des paramètres
    IF p_date_peremption IS NULL THEN
        RETURN 'OK';
    END IF;

    IF p_jours_alerte IS NOT NULL AND p_jours_alerte > 0 THEN
        SET v_jours_alerte = p_jours_alerte;
    END IF;

    -- Calculer les jours restants jusqu'à la péremption
    SET v_jours_restants = DATEDIFF(p_date_peremption, CURDATE());

    -- Déterminer le statut
    IF v_jours_restants < 0 THEN
        RETURN 'PERIME';
    ELSEIF v_jours_restants <= v_jours_alerte THEN
        RETURN 'ALERTE';
    ELSE
        RETURN 'OK';
    END IF;
END$$

-- ==========================================
-- FONCTION: Calcul de montant total avec TVA
-- ==========================================
-- Calcule le montant total TTC (TVA incluse)
--
-- Paramètres:
--   p_montant_ht DECIMAL(10,2) : Montant hors taxes
--   p_taux_tva DECIMAL(5,2) : Taux de TVA en pourcentage (ex: 18.00 pour 18%)
--
-- Retourne:
--   DECIMAL(10,2) : Montant TTC
--
-- Exemple:
--   SELECT FN_CALCULER_MONTANT_TTC(100.00, 18.00) AS montant_ttc;
--   Résultat: 118.00
-- ==========================================
DROP FUNCTION IF EXISTS FN_CALCULER_MONTANT_TTC$$
CREATE FUNCTION FN_CALCULER_MONTANT_TTC(
    p_montant_ht DECIMAL(10,2),
    p_taux_tva DECIMAL(5,2)
) RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_montant_ttc DECIMAL(10,2);

    -- Validation des paramètres
    IF p_montant_ht IS NULL OR p_montant_ht < 0 THEN
        RETURN 0.00;
    END IF;

    IF p_taux_tva IS NULL OR p_taux_tva < 0 THEN
        RETURN p_montant_ht;
    END IF;

    -- Calcul: montant_ht * (1 + taux_tva / 100)
    SET v_montant_ttc = p_montant_ht * (1 + p_taux_tva / 100.00);

    -- Arrondir à 2 décimales
    RETURN ROUND(v_montant_ttc, 2);
END$$

-- ==========================================
-- FONCTION: Calcul de quantité disponible
-- ==========================================
-- Calcule la quantité disponible d'un produit en tenant compte des réservations
--
-- Paramètres:
--   p_stock_id BIGINT : ID du stock
--
-- Retourne:
--   DECIMAL(10,2) : Quantité disponible (stock - réservations)
--
-- Exemple:
--   SELECT FN_CALCULER_QTE_DISPONIBLE(1) AS qte_disponible;
-- ==========================================
DROP FUNCTION IF EXISTS FN_CALCULER_QTE_DISPONIBLE$$
CREATE FUNCTION FN_CALCULER_QTE_DISPONIBLE(
    p_stock_id BIGINT
) RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_qte_stock DECIMAL(10,2);
    DECLARE v_qte_reservee DECIMAL(10,2) DEFAULT 0.00;
    DECLARE v_qte_disponible DECIMAL(10,2);

    -- Récupérer la quantité en stock
    SELECT COALESCE(qte, 0.00) INTO v_qte_stock
    FROM stock_produits
    WHERE id = p_stock_id;

    IF v_qte_stock IS NULL THEN
        RETURN 0.00;
    END IF;

    -- Calculer la quantité réservée (dans les ventes en attente)
    SELECT COALESCE(SUM(lv.qt), 0.00) INTO v_qte_reservee
    FROM lignes_vente lv
    INNER JOIN ventes v ON lv.fkVente = v.id
    WHERE lv.fkStock = p_stock_id
    AND v.statut = 'EN ATTENTE';

    -- Calculer la quantité disponible
    SET v_qte_disponible = v_qte_stock - COALESCE(v_qte_reservee, 0.00);

    -- Retourner 0 si négatif
    IF v_qte_disponible < 0 THEN
        RETURN 0.00;
    END IF;

    RETURN ROUND(v_qte_disponible, 2);
END$$

DELIMITER ;

-- ==========================================
-- VÉRIFICATION DES FONCTIONS CRÉÉES
-- ==========================================
-- Vérifier que les fonctions ont été créées correctement
SELECT
    ROUTINE_NAME AS 'Fonction',
    ROUTINE_TYPE AS 'Type',
    DATA_TYPE AS 'Type Retour',
    CREATED AS 'Date Création'
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = DATABASE()
AND ROUTINE_NAME IN (
    'FN_CALCULER_PRIX_AVEC_REMISE',
    'FN_VERIFIER_PEREMPTION',
    'FN_CALCULER_MONTANT_TTC',
    'FN_CALCULER_QTE_DISPONIBLE'
)
ORDER BY ROUTINE_NAME;
