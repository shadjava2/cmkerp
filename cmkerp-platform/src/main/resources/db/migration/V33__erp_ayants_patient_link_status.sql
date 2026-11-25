-- CMK ERP — V33 : statut de rapprochement patient Mediline sur ayants droit
-- MySQL uniquement. Aucun DDL SQL Server.

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_ayants_droit'
    AND COLUMN_NAME = 'patient_link_status'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE erp_ayants_droit ADD COLUMN patient_link_status VARCHAR(32) NOT NULL DEFAULT ''UNLINKED'' AFTER mediline_ipp',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_ayants_droit'
    AND INDEX_NAME = 'idx_erp_ayants_patient_link'
);

SET @sql := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_erp_ayants_patient_link ON erp_ayants_droit (patient_link_status)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
