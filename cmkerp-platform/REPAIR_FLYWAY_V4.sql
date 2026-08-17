-- ==========================================
-- SCRIPT DE RÉPARATION FLYWAY - Migration V4
-- ==========================================
-- Ce script supprime l'entrée échouée de la migration V4
-- dans la table flyway_schema_history pour permettre
-- la réexécution de la migration corrigée.
--
-- INSTRUCTIONS :
-- 1. Connectez-vous à votre base de données MySQL
-- 2. Sélectionnez la base de données : USE cmkerp-v24dev;
-- 3. Exécutez ce script
-- 4. Redémarrez l'application
-- ==========================================

USE `cmkerp-v24dev`;

-- Supprimer l'entrée échouée de la migration V4
DELETE FROM `flyway_schema_history`
WHERE `version` = '4'
  AND `type` = 'SQL'
  AND `description` = 'add performance indexes'
  AND `success` = 0;

-- Vérifier que l'entrée a été supprimée (devrait retourner 0 lignes)
SELECT * FROM `flyway_schema_history` WHERE `version` = '4';

-- Alternative : Si la requête ci-dessus ne fonctionne pas, utilisez cette version plus simple
-- DELETE FROM `flyway_schema_history` WHERE `version` = '4';

