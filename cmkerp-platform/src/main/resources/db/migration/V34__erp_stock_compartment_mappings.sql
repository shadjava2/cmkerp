-- V34 — Mapping compartiments historiques STOCK1..STOCKn → pharmacie / location ERP
-- Non destructif. Aucune attribution automatique inventée.

CREATE TABLE IF NOT EXISTS erp_stock_compartment_mappings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  compartment_code VARCHAR(32) NOT NULL,
  legacy_label VARCHAR(128) NULL,
  fk_pharmacy BIGINT UNSIGNED NULL,
  fk_location BIGINT NULL,
  mapping_status VARCHAR(32) NOT NULL DEFAULT 'UNMAPPED',
  notes VARCHAR(512) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_stock_compartment (compartment_code),
  KEY idx_erp_stock_comp_status (mapping_status),
  KEY idx_erp_stock_comp_pharma (fk_pharmacy)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO erp_stock_compartment_mappings (compartment_code, legacy_label, mapping_status)
SELECT CONCAT('STOCK', n.n), CONCAT('Compartiment historique STOCK', n.n), 'UNMAPPED'
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
  UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
  UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
) n;
