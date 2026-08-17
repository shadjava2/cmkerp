-- ==========================================
-- RÉPARATION FLYWAY V4 - Script SQL Rapide
-- ==========================================
-- Copiez-collez ce script dans votre client MySQL
-- ==========================================

USE `cmkerp-v24dev`;

-- Supprimer l'entrée échouée de la migration V4
DELETE FROM `flyway_schema_history`
WHERE `version` = '4' AND `success` = 0;

-- Vérifier que l'entrée a été supprimée
SELECT version, description, type, success, installed_on
FROM `flyway_schema_history`
WHERE `version` = '4';

-- Si la requête ci-dessus retourne des lignes avec success = 0, exécutez :
-- DELETE FROM `flyway_schema_history` WHERE `version` = '4' AND `success` = 0;

-- ==========================================
-- APRÈS RÉPARATION :
-- 1. Réactivez Flyway dans application-dev.yml : enabled: true
-- 2. Redémarrez l'application
-- ==========================================

