-- ==========================================
-- FLYWAY MIGRATION V5 - Stored Procedures Validation Vente
-- ==========================================
-- Création de stored procedures pour la validation des ventes avec règles métier complètes
--
-- Procédures créées :
-- - SP_VALIDATE_VENTE : Valide une vente avec vérification des stocks et mise à jour
-- - SP_ANNULER_VENTE : Annule une vente avec vérification 24h et remise en stock
-- - SP_ANNULER_VENTE_REMBOURSE : Annule une vente avec remboursement
-- ==========================================

DELIMITER $$

-- ==========================================
-- SP_VALIDATE_VENTE
-- ==========================================
-- Valide une vente avec vérification complète :
-- 1. Vérifie que la vente existe et n'est pas déjà validée/annulée
-- 2. Vérifie que toutes les lignes ont un stock valide
-- 3. Vérifie que les quantités en stock sont suffisantes
-- 4. Met à jour le statut de la vente
-- 5. Décrémente les quantités en stock pour chaque ligne
-- 6. Enregistre les informations de validation
--
-- Paramètres :
--   p_vente_id : ID de la vente à valider
--   p_statut : Statut de validation (SORTIE-USAGE, PAYEE, FACTUREE, etc.)
--   p_user_id : ID de l'utilisateur qui valide
--
-- Retourne :
--   0 : Succès
--   1 : Vente non trouvée
--   2 : Vente déjà validée
--   3 : Vente annulée
--   4 : Aucune ligne de vente
--   5 : Stock insuffisant pour une ligne
--   6 : Stock non trouvé pour une ligne
-- ==========================================
DROP PROCEDURE IF EXISTS SP_VALIDATE_VENTE$$
CREATE PROCEDURE SP_VALIDATE_VENTE(
    IN p_vente_id BIGINT,
    IN p_statut VARCHAR(50),
    IN p_user_id BIGINT,
    OUT p_result_code INT,
    OUT p_result_message VARCHAR(500)
)
BEGIN
    DECLARE v_statut_actuel VARCHAR(50);
    DECLARE v_ligne_count INT DEFAULT 0;
    DECLARE v_stock_qte FLOAT;
    DECLARE v_ligne_qt FLOAT;
    DECLARE v_fk_stock BIGINT;
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_has_error INT DEFAULT 0;

    -- Curseur pour parcourir les lignes de vente
    DECLARE cur_lignes CURSOR FOR
        SELECT fkStock, qt
        FROM lignes_vente
        WHERE fkVente = p_vente_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    -- Initialiser les variables de retour
    SET p_result_code = 0;
    SET p_result_message = 'Validation réussie';

    -- Vérifier que la vente existe
    SELECT statut INTO v_statut_actuel
    FROM ventes
    WHERE id = p_vente_id;

    IF v_statut_actuel IS NULL THEN
        SET p_result_code = 1;
        SET p_result_message = CONCAT('Vente non trouvée: ID=', p_vente_id);
    ELSEIF v_statut_actuel IN ('SORTIE-USAGE', 'PAYEE', 'FACTUREE', 'VALIDEE') THEN
        SET p_result_code = 2;
        SET p_result_message = CONCAT('Vente déjà validée avec le statut: ', v_statut_actuel);
    ELSEIF v_statut_actuel IN ('ANNULEE', 'ANNULEE-REMBOURSE') THEN
        SET p_result_code = 3;
        SET p_result_message = CONCAT('Impossible de valider une vente annulée: ', v_statut_actuel);
    ELSE
        -- Vérifier qu'il y a des lignes
        SELECT COUNT(*) INTO v_ligne_count
        FROM lignes_vente
        WHERE fkVente = p_vente_id;

        IF v_ligne_count = 0 THEN
            SET p_result_code = 4;
            SET p_result_message = 'Aucune ligne de vente trouvée';
        ELSE

    -- Vérifier les stocks et décrémenter
    OPEN cur_lignes;

    read_loop: LOOP
        FETCH cur_lignes INTO v_fk_stock, v_ligne_qt;

        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        -- Vérifier que le stock existe
        IF v_fk_stock IS NULL THEN
            SET p_result_code = 6;
            SET p_result_message = 'Ligne de vente sans stock associé';
            SET v_has_error = 1;
            LEAVE read_loop;
        END IF;

        -- Vérifier la quantité en stock
        SELECT qte INTO v_stock_qte
        FROM stock_produits
        WHERE id = v_fk_stock;

        IF v_stock_qte IS NULL THEN
            SET p_result_code = 6;
            SET p_result_message = CONCAT('Stock non trouvé pour ID=', v_fk_stock);
            SET v_has_error = 1;
            LEAVE read_loop;
        END IF;

        -- Vérifier que la quantité est suffisante
        IF v_stock_qte < v_ligne_qt THEN
            SET p_result_code = 5;
            SET p_result_message = CONCAT('Stock insuffisant: disponible=', v_stock_qte, ', requis=', v_ligne_qt, ' (stock_id=', v_fk_stock, ')');
            SET v_has_error = 1;
            LEAVE read_loop;
        END IF;

        -- Décrémenter le stock
        UPDATE stock_produits
        SET qte = qte - v_ligne_qt,
            dateupdate = NOW()
        WHERE id = v_fk_stock;

    END LOOP;

    CLOSE cur_lignes;

            -- Si erreur détectée, annuler les modifications (rollback implicite via transaction)
            IF v_has_error = 0 THEN
                -- Déterminer le statut final
                IF p_statut IS NULL OR p_statut = '' OR p_statut = 'VALIDEE' THEN
                    SET p_statut = 'SORTIE-USAGE';
                END IF;

                -- Mettre à jour le statut de la vente
                UPDATE ventes
                SET statut = p_statut,
                    dateupdate = NOW(),
                    userupdateid = p_user_id
                WHERE id = p_vente_id;

                SET p_result_message = CONCAT('Vente validée avec succès. Statut: ', p_statut);
            END IF;
        END IF;
    END IF;

END$$

-- ==========================================
-- SP_ANNULER_VENTE
-- ==========================================
-- Annule une vente avec vérification 24h et remise en stock
--
-- Paramètres :
--   p_vente_id : ID de la vente à annuler
--   p_user_id : ID de l'utilisateur qui annule
--
-- Retourne :
--   0 : Succès
--   1 : Vente non trouvée
--   2 : Vente déjà annulée
--   3 : Délai de 24h dépassé
--   4 : Aucune ligne de vente
-- ==========================================
DROP PROCEDURE IF EXISTS SP_ANNULER_VENTE$$
CREATE PROCEDURE SP_ANNULER_VENTE(
    IN p_vente_id BIGINT,
    IN p_user_id BIGINT,
    OUT p_result_code INT,
    OUT p_result_message VARCHAR(500)
)
BEGIN
    DECLARE v_statut_actuel VARCHAR(50);
    DECLARE v_date_update TIMESTAMP;
    DECLARE v_ligne_count INT DEFAULT 0;
    DECLARE v_fk_stock BIGINT;
    DECLARE v_ligne_qt FLOAT;
    DECLARE v_done INT DEFAULT 0;

    -- Curseur pour parcourir les lignes de vente
    DECLARE cur_lignes CURSOR FOR
        SELECT fkStock, qt
        FROM lignes_vente
        WHERE fkVente = p_vente_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    -- Initialiser les variables de retour
    SET p_result_code = 0;
    SET p_result_message = 'Annulation réussie';

    -- Vérifier que la vente existe
    SELECT statut, dateupdate INTO v_statut_actuel, v_date_update
    FROM ventes
    WHERE id = p_vente_id;

    IF v_statut_actuel IS NULL THEN
        SET p_result_code = 1;
        SET p_result_message = CONCAT('Vente non trouvée: ID=', p_vente_id);
    ELSEIF v_statut_actuel IN ('ANNULEE', 'ANNULEE-REMBOURSE') THEN
        SET p_result_code = 2;
        SET p_result_message = CONCAT('Vente déjà annulée avec le statut: ', v_statut_actuel);
    ELSE
        -- Vérifier le délai de 24h si la vente est validée
        IF v_statut_actuel IN ('SORTIE-USAGE', 'PAYEE', 'FACTUREE', 'VALIDEE') THEN
            IF v_date_update IS NOT NULL AND TIMESTAMPDIFF(HOUR, v_date_update, NOW()) > 24 THEN
                SET p_result_code = 3;
                SET p_result_message = 'Impossible d\'annuler une vente validée il y a plus de 24h';
            ELSE
                -- Vérifier qu'il y a des lignes
                SELECT COUNT(*) INTO v_ligne_count
                FROM lignes_vente
                WHERE fkVente = p_vente_id;

                IF v_ligne_count = 0 THEN
                    SET p_result_code = 4;
                    SET p_result_message = 'Aucune ligne de vente trouvée';
                ELSE
                    -- Remettre les stocks en place
                    OPEN cur_lignes;

                    read_loop: LOOP
                        FETCH cur_lignes INTO v_fk_stock, v_ligne_qt;

                        IF v_done = 1 THEN
                            LEAVE read_loop;
                        END IF;

                        -- Remettre le stock
                        IF v_fk_stock IS NOT NULL THEN
                            UPDATE stock_produits
                            SET qte = qte + v_ligne_qt,
                                dateupdate = NOW()
                            WHERE id = v_fk_stock;
                        END IF;

                    END LOOP;

                    CLOSE cur_lignes;

                    -- Mettre à jour le statut de la vente
                    UPDATE ventes
                    SET statut = 'ANNULEE',
                        dateupdate = NOW(),
                        userupdateid = p_user_id
                    WHERE id = p_vente_id;

                    SET p_result_message = 'Vente annulée avec succès. Stocks remis en place.';
                END IF;
            END IF;
        ELSE
            -- Vente en attente, peut être annulée directement
            UPDATE ventes
            SET statut = 'ANNULEE',
                dateupdate = NOW(),
                userupdateid = p_user_id
            WHERE id = p_vente_id;

            SET p_result_message = 'Vente annulée avec succès.';
        END IF;
    END IF;

END$$

-- ==========================================
-- SP_ANNULER_VENTE_REMBOURSE
-- ==========================================
-- Annule une vente avec remboursement (statut ANNULEE-REMBOURSE)
-- Même logique que SP_ANNULER_VENTE mais avec un statut différent
-- ==========================================
DROP PROCEDURE IF EXISTS SP_ANNULER_VENTE_REMBOURSE$$
CREATE PROCEDURE SP_ANNULER_VENTE_REMBOURSE(
    IN p_vente_id BIGINT,
    IN p_user_id BIGINT,
    OUT p_result_code INT,
    OUT p_result_message VARCHAR(500)
)
BEGIN
    DECLARE v_statut_actuel VARCHAR(50);
    DECLARE v_date_update TIMESTAMP;
    DECLARE v_ligne_count INT DEFAULT 0;
    DECLARE v_fk_stock BIGINT;
    DECLARE v_ligne_qt FLOAT;
    DECLARE v_done INT DEFAULT 0;

    -- Curseur pour parcourir les lignes de vente
    DECLARE cur_lignes CURSOR FOR
        SELECT fkStock, qt
        FROM lignes_vente
        WHERE fkVente = p_vente_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    -- Initialiser les variables de retour
    SET p_result_code = 0;
    SET p_result_message = 'Annulation avec remboursement réussie';

    -- Vérifier que la vente existe
    SELECT statut, dateupdate INTO v_statut_actuel, v_date_update
    FROM ventes
    WHERE id = p_vente_id;

    IF v_statut_actuel IS NULL THEN
        SET p_result_code = 1;
        SET p_result_message = CONCAT('Vente non trouvée: ID=', p_vente_id);
    ELSEIF v_statut_actuel IN ('ANNULEE', 'ANNULEE-REMBOURSE') THEN
        SET p_result_code = 2;
        SET p_result_message = CONCAT('Vente déjà annulée avec le statut: ', v_statut_actuel);
    ELSE
        -- Vérifier le délai de 24h si la vente est validée
        IF v_statut_actuel IN ('SORTIE-USAGE', 'PAYEE', 'FACTUREE', 'VALIDEE') THEN
            IF v_date_update IS NOT NULL AND TIMESTAMPDIFF(HOUR, v_date_update, NOW()) > 24 THEN
                SET p_result_code = 3;
                SET p_result_message = 'Impossible d\'annuler une vente validée il y a plus de 24h';
            ELSE
                -- Vérifier qu'il y a des lignes
                SELECT COUNT(*) INTO v_ligne_count
                FROM lignes_vente
                WHERE fkVente = p_vente_id;

                IF v_ligne_count = 0 THEN
                    SET p_result_code = 4;
                    SET p_result_message = 'Aucune ligne de vente trouvée';
                ELSE
                    -- Remettre les stocks en place
                    OPEN cur_lignes;

                    read_loop: LOOP
                        FETCH cur_lignes INTO v_fk_stock, v_ligne_qt;

                        IF v_done = 1 THEN
                            LEAVE read_loop;
                        END IF;

                        -- Remettre le stock
                        IF v_fk_stock IS NOT NULL THEN
                            UPDATE stock_produits
                            SET qte = qte + v_ligne_qt,
                                dateupdate = NOW()
                            WHERE id = v_fk_stock;
                        END IF;

                    END LOOP;

                    CLOSE cur_lignes;

                    -- Mettre à jour le statut de la vente
                    UPDATE ventes
                    SET statut = 'ANNULEE-REMBOURSE',
                        dateupdate = NOW(),
                        userupdateid = p_user_id
                    WHERE id = p_vente_id;

                    SET p_result_message = 'Vente annulée avec remboursement. Stocks remis en place.';
                END IF;
            END IF;
        ELSE
            -- Vente en attente, peut être annulée directement
            UPDATE ventes
            SET statut = 'ANNULEE-REMBOURSE',
                dateupdate = NOW(),
                userupdateid = p_user_id
            WHERE id = p_vente_id;

            SET p_result_message = 'Vente annulée avec remboursement.';
        END IF;
    END IF;

END$$

DELIMITER ;
