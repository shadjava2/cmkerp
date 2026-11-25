-- CMK ERP — V32 : tables relationnelles Facturation (MySQL uniquement)
-- Migre les données déjà importées depuis erp_clinic_referentials.
-- Aucun événement outbox / aucune écriture SQL Server.

-- ========== 1. Référentiels simples ==========
CREATE TABLE IF NOT EXISTS erp_services (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(16) NOT NULL,
  libelle VARCHAR(255) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(16) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_services_code (code),
  KEY idx_erp_services_actif (actif),
  KEY idx_erp_services_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_imputations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  designation VARCHAR(255) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(32) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_imputations_code (code),
  KEY idx_erp_imputations_actif (actif),
  KEY idx_erp_imputations_sync (sync_status),
  KEY idx_erp_imputations_designation (designation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_locations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(16) NOT NULL,
  libelle VARCHAR(255) NULL,
  adresse VARCHAR(512) NULL,
  fk_site BIGINT NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(16) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_locations_code (code),
  KEY idx_erp_locations_site (fk_site),
  KEY idx_erp_locations_actif (actif),
  KEY idx_erp_locations_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_medecins (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  nom VARCHAR(255) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(32) NULL,
  mediline_ref VARCHAR(64) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_medecins_code (code),
  KEY idx_erp_medecins_nom (nom),
  KEY idx_erp_medecins_actif (actif),
  KEY idx_erp_medecins_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========== 2. Sociétés / classes / tarifs ==========
CREATE TABLE IF NOT EXISTS erp_societes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(16) NOT NULL,
  designation VARCHAR(255) NULL,
  adresse VARCHAR(512) NULL,
  n_impot VARCHAR(64) NULL,
  nrc VARCHAR(64) NULL,
  id_nat VARCHAR(64) NULL,
  fk_contrat BIGINT NULL,
  legacy_contrat_code VARCHAR(8) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(16) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_societes_code (code),
  KEY idx_erp_societes_contrat (fk_contrat),
  KEY idx_erp_societes_designation (designation),
  KEY idx_erp_societes_actif (actif),
  KEY idx_erp_societes_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_classes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  libelle VARCHAR(255) NULL,
  legacy_service_text VARCHAR(255) NULL,
  fk_service BIGINT NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(32) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_classes_code (code),
  KEY idx_erp_classes_service (fk_service),
  KEY idx_erp_classes_actif (actif),
  KEY idx_erp_classes_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_tarifs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  designation VARCHAR(255) NULL,
  fk_service BIGINT NULL,
  legacy_service_code VARCHAR(16) NULL,
  fk_classe BIGINT NULL,
  legacy_classe_code VARCHAR(32) NULL,
  fk_imputation BIGINT NULL,
  legacy_imputation_code VARCHAR(32) NULL,
  montant DECIMAL(18,4) NOT NULL DEFAULT 0,
  montant_prive DECIMAL(18,4) NOT NULL DEFAULT 0,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(32) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_tarifs_code (code),
  KEY idx_erp_tarifs_service (fk_service),
  KEY idx_erp_tarifs_classe (fk_classe),
  KEY idx_erp_tarifs_imputation (fk_imputation),
  KEY idx_erp_tarifs_designation (designation),
  KEY idx_erp_tarifs_actif (actif),
  KEY idx_erp_tarifs_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========== 3. Ayants droit ==========
CREATE TABLE IF NOT EXISTS erp_ayants_droit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  matricule VARCHAR(32) NOT NULL,
  nom_complet VARCHAR(255) NULL,
  sexe VARCHAR(8) NULL,
  date_naissance DATE NULL,
  nationalite VARCHAR(16) NULL,
  relation VARCHAR(16) NULL,
  code_famille VARCHAR(32) NULL,
  fk_titulaire BIGINT NULL,
  fk_societe BIGINT NULL,
  legacy_codesoc VARCHAR(16) NULL,
  fk_contrat BIGINT NULL,
  legacy_contrat_code VARCHAR(8) NULL,
  mediline_patient_id BIGINT NULL,
  mediline_ipp VARCHAR(64) NULL,
  assurance VARCHAR(8) NULL,
  covid VARCHAR(8) NULL,
  legacy_location_code VARCHAR(16) NULL,
  fk_location BIGINT NULL,
  source_legacy VARCHAR(16) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_code VARCHAR(32) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_ayants_matricule (matricule),
  KEY idx_erp_ayants_societe (fk_societe),
  KEY idx_erp_ayants_contrat (fk_contrat),
  KEY idx_erp_ayants_nom (nom_complet),
  KEY idx_erp_ayants_ipp (mediline_ipp),
  KEY idx_erp_ayants_famille (code_famille),
  KEY idx_erp_ayants_actif (actif),
  KEY idx_erp_ayants_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========== 4. Mappings (pas de duplication référentiels ERP) ==========
CREATE TABLE IF NOT EXISTS erp_user_external_mappings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  erp_user_id BIGINT NULL,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_user_code VARCHAR(64) NOT NULL,
  legacy_location_code VARCHAR(16) NULL,
  legacy_display_name VARCHAR(255) NULL,
  mapping_status VARCHAR(32) NOT NULL DEFAULT 'UNMATCHED',
  actif TINYINT(1) NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_user_ext_src_code (source_system, legacy_user_code),
  KEY idx_erp_user_ext_erp (erp_user_id),
  KEY idx_erp_user_ext_status (mapping_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_pharmacy_external_mappings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  erp_pharmacy_id BIGINT UNSIGNED NULL,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_pharmacy_code VARCHAR(64) NOT NULL,
  legacy_designation VARCHAR(255) NULL,
  mapping_status VARCHAR(32) NOT NULL DEFAULT 'UNMATCHED',
  actif TINYINT(1) NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_pharma_ext_src_code (source_system, legacy_pharmacy_code),
  KEY idx_erp_pharma_ext_erp (erp_pharmacy_id),
  KEY idx_erp_pharma_ext_status (mapping_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_product_external_mappings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  erp_product_id BIGINT NULL,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  legacy_product_code VARCHAR(64) NOT NULL,
  legacy_designation VARCHAR(255) NULL,
  mapping_status VARCHAR(32) NOT NULL DEFAULT 'UNMATCHED',
  actif TINYINT(1) NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_prod_ext_src_code (source_system, legacy_product_code),
  KEY idx_erp_prod_ext_erp (erp_product_id),
  KEY idx_erp_prod_ext_status (mapping_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========== 5. Stock clinique normalisé (projection) ==========
CREATE TABLE IF NOT EXISTS erp_stock_clinique (
  id BIGINT NOT NULL AUTO_INCREMENT,
  legacy_product_code VARCHAR(64) NOT NULL,
  fk_product BIGINT NULL,
  compartment_code VARCHAR(32) NOT NULL,
  quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
  fk_pharmacy BIGINT UNSIGNED NULL,
  fk_location BIGINT NULL,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_stock_clinique (legacy_product_code, compartment_code),
  KEY idx_erp_stock_clinique_prod (fk_product),
  KEY idx_erp_stock_clinique_pharma (fk_pharmacy)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========== 6. Transactions facturation (structure ; import sélectif ultérieur) ==========
CREATE TABLE IF NOT EXISTS erp_facturation_medicaments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  legacy_id VARCHAR(64) NULL,
  operation_id VARCHAR(64) NULL,
  fk_ayant_droit BIGINT NULL,
  legacy_matricule VARCHAR(32) NULL,
  mediline_patient_id BIGINT NULL,
  fk_societe BIGINT NULL,
  fk_contrat BIGINT NULL,
  fk_product BIGINT NULL,
  legacy_product_code VARCHAR(64) NULL,
  fk_medecin BIGINT NULL,
  legacy_medecin_code VARCHAR(32) NULL,
  fk_pharmacy BIGINT UNSIGNED NULL,
  legacy_pharmacy_code VARCHAR(64) NULL,
  fk_service BIGINT NULL,
  fk_location BIGINT NULL,
  quantite DECIMAL(18,4) NULL,
  montant DECIMAL(18,4) NULL,
  date_mouvement DATETIME NULL,
  payload_snapshot_json MEDIUMTEXT NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_fact_med_legacy (legacy_id),
  UNIQUE KEY uk_erp_fact_med_opid (operation_id),
  KEY idx_erp_fact_med_ayant (fk_ayant_droit),
  KEY idx_erp_fact_med_soc (fk_societe),
  KEY idx_erp_fact_med_prod (fk_product)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS erp_facturation_prestations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  legacy_id VARCHAR(64) NULL,
  operation_id VARCHAR(64) NULL,
  legacy_idmvt VARCHAR(64) NULL,
  fk_ayant_droit BIGINT NULL,
  legacy_matricule VARCHAR(32) NULL,
  mediline_patient_id BIGINT NULL,
  fk_societe BIGINT NULL,
  fk_contrat BIGINT NULL,
  fk_tarif BIGINT NULL,
  legacy_tarif_code VARCHAR(32) NULL,
  fk_service BIGINT NULL,
  fk_classe BIGINT NULL,
  fk_imputation BIGINT NULL,
  fk_medecin BIGINT NULL,
  legacy_medecin_code VARCHAR(32) NULL,
  fk_location BIGINT NULL,
  montant DECIMAL(18,4) NULL,
  date_prestation DATETIME NULL,
  payload_snapshot_json MEDIUMTEXT NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'CLINIQUE_BEACH',
  sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_fact_prest_legacy (legacy_id),
  UNIQUE KEY uk_erp_fact_prest_opid (operation_id),
  KEY idx_erp_fact_prest_ayant (fk_ayant_droit),
  KEY idx_erp_fact_prest_tarif (fk_tarif),
  KEY idx_erp_fact_prest_soc (fk_societe)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========== 7. Migration données depuis erp_clinic_referentials ==========
INSERT INTO erp_services (code, libelle, actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at, version, datecreate, dateupdate, usercreateid, userupdateid)
SELECT
  r.natural_key,
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LIBELLE')), 'null'),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'SERVICES'
  AND NOT EXISTS (SELECT 1 FROM erp_services s WHERE s.code = r.natural_key);

INSERT INTO erp_imputations (code, designation, actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at, version, datecreate, dateupdate, usercreateid, userupdateid)
SELECT
  r.natural_key,
  COALESCE(
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.DESIGNATION')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LIBELLE')), 'null')
  ),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'IMPUTATIONS'
  AND NOT EXISTS (SELECT 1 FROM erp_imputations i WHERE i.code = r.natural_key);

INSERT INTO erp_locations (code, libelle, adresse, fk_site, actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at, version, datecreate, dateupdate, usercreateid, userupdateid)
SELECT
  r.natural_key,
  COALESCE(
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LIBELLE')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.DESIGNATION')), 'null')
  ),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.ADRESSE')), 'null'),
  CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.siteId')), 'null') AS UNSIGNED),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'LOCATIONS'
  AND NOT EXISTS (SELECT 1 FROM erp_locations l WHERE l.code = r.natural_key);

INSERT INTO erp_medecins (code, nom, actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at, version, datecreate, dateupdate, usercreateid, userupdateid)
SELECT
  r.natural_key,
  COALESCE(
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.NOM')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.NOMMED')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.DESIGNATION')), 'null')
  ),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'MEDECINS'
  AND NOT EXISTS (SELECT 1 FROM erp_medecins m WHERE m.code = r.natural_key);

INSERT INTO erp_societes (
  code, designation, adresse, n_impot, nrc, id_nat, legacy_contrat_code,
  actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at, version, datecreate, dateupdate, usercreateid, userupdateid
)
SELECT
  r.natural_key,
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.NOMSOC')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.ADRESSE')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.N_IMPOT')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.NRC')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.ID_NAT')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.CONTRAT')), 'null'),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'SOCIETES'
  AND NOT EXISTS (SELECT 1 FROM erp_societes s WHERE s.code = r.natural_key);

UPDATE erp_societes s
INNER JOIN erp_contrats c ON c.code = s.legacy_contrat_code
SET s.fk_contrat = c.id
WHERE s.fk_contrat IS NULL AND s.legacy_contrat_code IS NOT NULL;

INSERT INTO erp_classes (
  code, libelle, legacy_service_text, actif, source_system, legacy_code,
  sync_status, sync_fingerprint, last_synced_at, version, datecreate, dateupdate, usercreateid, userupdateid
)
SELECT
  r.natural_key,
  COALESCE(
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LIBELLE')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.DESIGNATION')), 'null')
  ),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.SERVICE')), 'null'),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'CLASSES'
  AND NOT EXISTS (SELECT 1 FROM erp_classes c WHERE c.code = r.natural_key);

UPDATE erp_classes c
INNER JOIN erp_services s ON s.code = c.legacy_service_text
SET c.fk_service = s.id
WHERE c.fk_service IS NULL AND c.legacy_service_text IS NOT NULL;

UPDATE erp_classes c
INNER JOIN erp_services s ON UPPER(TRIM(s.libelle)) = UPPER(TRIM(c.legacy_service_text))
SET c.fk_service = s.id
WHERE c.fk_service IS NULL AND c.legacy_service_text IS NOT NULL;

INSERT INTO erp_tarifs (
  code, designation, legacy_service_code, legacy_classe_code, legacy_imputation_code,
  montant, montant_prive, actif, source_system, legacy_code,
  sync_status, sync_fingerprint, last_synced_at, version, datecreate, dateupdate, usercreateid, userupdateid
)
SELECT
  r.natural_key,
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LIBPRES')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.CODESERV')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.CLASSE')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.IMPUTATION')), 'null'),
  CAST(COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.MONTANT')), 'null'), '0') AS DECIMAL(18,4)),
  CAST(COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.MONTPRIVE')), 'null'), '0') AS DECIMAL(18,4)),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'TARIFS'
  AND NOT EXISTS (SELECT 1 FROM erp_tarifs t WHERE t.code = r.natural_key);

UPDATE erp_tarifs t
LEFT JOIN erp_services s ON s.code = t.legacy_service_code
LEFT JOIN erp_classes c ON c.code = t.legacy_classe_code
LEFT JOIN erp_imputations i ON i.code = t.legacy_imputation_code
SET t.fk_service = s.id, t.fk_classe = c.id, t.fk_imputation = i.id
WHERE t.fk_service IS NULL OR t.fk_classe IS NULL OR t.fk_imputation IS NULL;

INSERT INTO erp_ayants_droit (
  matricule, nom_complet, sexe, date_naissance, nationalite, relation, code_famille,
  legacy_codesoc, legacy_contrat_code, assurance, covid, legacy_location_code, source_legacy,
  actif, source_system, legacy_code, sync_status, sync_fingerprint, last_synced_at, version,
  datecreate, dateupdate, usercreateid, userupdateid
)
SELECT
  r.natural_key,
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.NOM')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.SEXE')), 'null'),
  STR_TO_DATE(LEFT(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.DATENAIS')), 'null'), 10), '%Y-%m-%d'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.NATIONALITE')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.RELATION')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.CODEFAM')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.CODESOC')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.CONTRAT')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.ASSURANCE')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.COVID')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LOCATION')), 'null'),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.SOURCE')), 'null'),
  COALESCE(r.actif, 1),
  COALESCE(NULLIF(r.source_system, ''), 'CLINIQUE_BEACH'),
  r.natural_key,
  COALESCE(NULLIF(r.sync_status, ''), 'SYNCED'),
  r.sync_fingerprint,
  r.last_synced_at,
  COALESCE(r.version, 1),
  COALESCE(r.datecreate, NOW()),
  COALESCE(r.dateupdate, NOW()),
  r.usercreateid,
  r.userupdateid
FROM erp_clinic_referentials r
WHERE r.referential_key = 'AYANTS_DROIT'
  AND NOT EXISTS (SELECT 1 FROM erp_ayants_droit a WHERE a.matricule = r.natural_key);

UPDATE erp_ayants_droit a
LEFT JOIN erp_societes s ON s.code = a.legacy_codesoc
LEFT JOIN erp_contrats c ON c.code = a.legacy_contrat_code
LEFT JOIN erp_locations l ON l.code = a.legacy_location_code
SET a.fk_societe = s.id, a.fk_contrat = c.id, a.fk_location = l.id
WHERE a.fk_societe IS NULL OR a.fk_contrat IS NULL OR a.fk_location IS NULL;

UPDATE erp_ayants_droit a
INNER JOIN erp_ayants_droit t ON t.matricule = a.code_famille AND t.id <> a.id
SET a.fk_titulaire = t.id
WHERE a.fk_titulaire IS NULL
  AND a.code_famille IS NOT NULL
  AND a.code_famille <> a.matricule;

INSERT INTO erp_user_external_mappings (legacy_user_code, legacy_display_name, legacy_location_code, mapping_status, source_system)
SELECT
  r.natural_key,
  COALESCE(
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.NOM')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LOGIN')), 'null'),
    r.natural_key
  ),
  NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LOCATION')), 'null'),
  'UNMATCHED',
  'CLINIQUE_BEACH'
FROM erp_clinic_referentials r
WHERE r.referential_key = 'UTILISATEURS'
  AND NOT EXISTS (
    SELECT 1 FROM erp_user_external_mappings m
    WHERE m.source_system = 'CLINIQUE_BEACH' AND m.legacy_user_code = r.natural_key
  );

INSERT INTO erp_pharmacy_external_mappings (legacy_pharmacy_code, legacy_designation, mapping_status, source_system)
SELECT
  r.natural_key,
  COALESCE(
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.DESIGNATION')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LIBELLE')), 'null'),
    r.natural_key
  ),
  'UNMATCHED',
  'CLINIQUE_BEACH'
FROM erp_clinic_referentials r
WHERE r.referential_key = 'PHARMACIES'
  AND NOT EXISTS (
    SELECT 1 FROM erp_pharmacy_external_mappings m
    WHERE m.source_system = 'CLINIQUE_BEACH' AND m.legacy_pharmacy_code = r.natural_key
  );

INSERT INTO erp_product_external_mappings (erp_product_id, legacy_product_code, legacy_designation, mapping_status, source_system)
SELECT
  p.id,
  r.natural_key,
  COALESCE(
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.DESIGNATION')), 'null'),
    NULLIF(JSON_UNQUOTE(JSON_EXTRACT(r.payload_json, '$.LIBELLE')), 'null'),
    r.natural_key
  ),
  CASE WHEN p.id IS NULL THEN 'UNMATCHED' ELSE 'MATCHED' END,
  'CLINIQUE_BEACH'
FROM erp_clinic_referentials r
LEFT JOIN produits p ON p.CODECLINIQUE = r.natural_key OR p.codebarre = r.natural_key
WHERE r.referential_key = 'PRODUITS'
  AND NOT EXISTS (
    SELECT 1 FROM erp_product_external_mappings m
    WHERE m.source_system = 'CLINIQUE_BEACH' AND m.legacy_product_code = r.natural_key
  );

-- ========== 8. FK contraintes (après rapprochement) ==========
ALTER TABLE erp_societes
  ADD CONSTRAINT fk_erp_societes_contrat FOREIGN KEY (fk_contrat) REFERENCES erp_contrats (id);

ALTER TABLE erp_classes
  ADD CONSTRAINT fk_erp_classes_service FOREIGN KEY (fk_service) REFERENCES erp_services (id);

ALTER TABLE erp_tarifs
  ADD CONSTRAINT fk_erp_tarifs_service FOREIGN KEY (fk_service) REFERENCES erp_services (id),
  ADD CONSTRAINT fk_erp_tarifs_classe FOREIGN KEY (fk_classe) REFERENCES erp_classes (id),
  ADD CONSTRAINT fk_erp_tarifs_imputation FOREIGN KEY (fk_imputation) REFERENCES erp_imputations (id);

ALTER TABLE erp_ayants_droit
  ADD CONSTRAINT fk_erp_ayants_societe FOREIGN KEY (fk_societe) REFERENCES erp_societes (id),
  ADD CONSTRAINT fk_erp_ayants_contrat FOREIGN KEY (fk_contrat) REFERENCES erp_contrats (id),
  ADD CONSTRAINT fk_erp_ayants_location FOREIGN KEY (fk_location) REFERENCES erp_locations (id),
  ADD CONSTRAINT fk_erp_ayants_titulaire FOREIGN KEY (fk_titulaire) REFERENCES erp_ayants_droit (id);
