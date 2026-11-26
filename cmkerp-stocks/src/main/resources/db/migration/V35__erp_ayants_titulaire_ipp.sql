-- V35 — Complément non destructif Ayants droit (IPP titulaire Mediline)
-- MySQL uniquement. Aucun DDL SQL Server CLINIQUE.

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_ayants_droit'
    AND COLUMN_NAME = 'titulaire_ipp'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE erp_ayants_droit ADD COLUMN titulaire_ipp VARCHAR(64) NULL AFTER fk_titulaire',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_ayants_droit'
    AND INDEX_NAME = 'idx_erp_ayants_titulaire_ipp'
);

SET @sql := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_erp_ayants_titulaire_ipp ON erp_ayants_droit (titulaire_ipp)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
