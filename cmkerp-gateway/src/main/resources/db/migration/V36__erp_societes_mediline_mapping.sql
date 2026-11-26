-- V36 — Correspondances ERP Société ↔ compte Mediline + registre enregistrement patient intégré
-- MySQL uniquement. Aucun DDL SQL Server CLINIQUE.

-- ========== erp_societes : colonnes mapping Mediline ==========
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_societes' AND COLUMN_NAME = 'mediline_compte_code');
SET @sql := IF(@col = 0,
  'ALTER TABLE erp_societes ADD COLUMN mediline_compte_code VARCHAR(20) NULL AFTER legacy_contrat_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_societes' AND COLUMN_NAME = 'mediline_compte_intitule');
SET @sql := IF(@col = 0,
  'ALTER TABLE erp_societes ADD COLUMN mediline_compte_intitule VARCHAR(255) NULL AFTER mediline_compte_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_societes' AND COLUMN_NAME = 'mediline_mapping_status');
SET @sql := IF(@col = 0,
  'ALTER TABLE erp_societes ADD COLUMN mediline_mapping_status VARCHAR(32) NOT NULL DEFAULT ''UNMATCHED'' AFTER mediline_compte_intitule',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_societes' AND COLUMN_NAME = 'mediline_mapping_verified_at');
SET @sql := IF(@col = 0,
  'ALTER TABLE erp_societes ADD COLUMN mediline_mapping_verified_at DATETIME NULL AFTER mediline_mapping_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_societes' AND COLUMN_NAME = 'mediline_mapping_verified_by');
SET @sql := IF(@col = 0,
  'ALTER TABLE erp_societes ADD COLUMN mediline_mapping_verified_by BIGINT NULL AFTER mediline_mapping_verified_at',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_societes' AND INDEX_NAME = 'idx_erp_societes_mediline_compte');
SET @sql := IF(@idx = 0,
  'CREATE INDEX idx_erp_societes_mediline_compte ON erp_societes (mediline_compte_code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_societes' AND INDEX_NAME = 'idx_erp_societes_mediline_mapping_status');
SET @sql := IF(@idx = 0,
  'CREATE INDEX idx_erp_societes_mediline_mapping_status ON erp_societes (mediline_mapping_status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== Registre orchestration DMI + Facturation ==========
CREATE TABLE IF NOT EXISTS erp_patient_registration (
  id BIGINT NOT NULL AUTO_INCREMENT,
  request_id CHAR(36) NOT NULL,
  mediline_ipp VARCHAR(64) NULL,
  mediline_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  ayant_id BIGINT NULL,
  ayant_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  societe_id BIGINT NULL,
  contrat_id BIGINT NULL,
  payload_json JSON NULL,
  last_error VARCHAR(2000) NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_patient_registration_request (request_id),
  KEY idx_erp_patient_registration_mediline_ipp (mediline_ipp),
  KEY idx_erp_patient_registration_ayant (ayant_id),
  KEY idx_erp_patient_registration_mediline_status (mediline_status),
  KEY idx_erp_patient_registration_ayant_status (ayant_status),
  CONSTRAINT fk_erp_patient_reg_societe FOREIGN KEY (societe_id) REFERENCES erp_societes (id) ON DELETE SET NULL,
  CONSTRAINT fk_erp_patient_reg_contrat FOREIGN KEY (contrat_id) REFERENCES erp_contrats (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;