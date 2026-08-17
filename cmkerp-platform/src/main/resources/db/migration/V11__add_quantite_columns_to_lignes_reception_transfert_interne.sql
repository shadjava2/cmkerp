-- ==========================================
-- Migration V11 : Ajout des colonnes quantiteDemandee et quantiteTransferee
-- ==========================================
-- Date: 2025-12-18
-- Description: Ajoute les colonnes quantiteDemandee et quantiteTransferee à la table
--              lignes_reception_transfert_interne si elles n'existent pas déjà
-- ==========================================

-- Ajouter quantiteDemandee si elle n'existe pas
SET @dbname = DATABASE();
SET @tablename = 'lignes_reception_transfert_interne';
SET @columnname = 'quantiteDemandee';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' float NULL DEFAULT NULL')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Ajouter quantiteTransferee si elle n'existe pas
SET @columnname = 'quantiteTransferee';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' float NULL DEFAULT NULL')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

