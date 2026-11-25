-- CMK ERP — V30 : coexistence CLINIQUE — erp_contrats + outbox + RBAC Facturation Contrats / Sync
-- MySQL primaire uniquement. Aucun DDL SQL Server.

CREATE TABLE IF NOT EXISTS erp_contrats (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(3) NOT NULL,
  designation VARCHAR(50) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'ERP',
  legacy_code VARCHAR(3) NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'LOCAL_ONLY',
  sync_fingerprint VARCHAR(64) NULL,
  last_synced_at DATETIME NULL,
  version INT NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_contrats_code (code),
  KEY idx_erp_contrats_actif (actif),
  KEY idx_erp_contrats_sync (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS erp_sync_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  operation_id VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id BIGINT NULL,
  legacy_key VARCHAR(64) NULL,
  source_system VARCHAR(32) NOT NULL,
  target_system VARCHAR(32) NOT NULL,
  operation_type VARCHAR(16) NOT NULL,
  payload_json MEDIUMTEXT NOT NULL,
  expected_fingerprint VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME NULL,
  last_error VARCHAR(1000) NULL,
  correlation_id VARCHAR(64) NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  datesynced DATETIME NULL,
  dateupdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_sync_outbox_op (operation_id),
  KEY idx_erp_sync_outbox_status (status, next_attempt_at),
  KEY idx_erp_sync_outbox_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Menus + permissions Contrats / Synchronisation / Locations
INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'SIDEBAR_FACTURATION_LOCATIONS', 'Menu Locations du module Facturation', mf.id, NOW(), NOW()
FROM permissions mf
WHERE mf.nom = 'MODULE_FACTURATION'
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.nom = 'SIDEBAR_FACTURATION_LOCATIONS');

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'SIDEBAR_FACTURATION_SYNCHRONISATION', 'Menu Synchronisation CLINIQUE', mf.id, NOW(), NOW()
FROM permissions mf
WHERE mf.nom = 'MODULE_FACTURATION'
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.nom = 'SIDEBAR_FACTURATION_SYNCHRONISATION');

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, c.id, NOW(), NOW()
FROM (
  SELECT 'FACTURATION_CONTRATS_VIEW' AS nom, 'Consulter les contrats facturation' AS description
  UNION ALL SELECT 'FACTURATION_CONTRATS_CREATE', 'Créer un contrat facturation'
  UNION ALL SELECT 'FACTURATION_CONTRATS_UPDATE', 'Modifier un contrat facturation'
  UNION ALL SELECT 'FACTURATION_CONTRATS_DELETE', 'Supprimer un contrat facturation (contrôlé)'
  UNION ALL SELECT 'FACTURATION_CONTRATS_STATUS_UPDATE', 'Activer / désactiver un contrat (ERP seulement)'
  UNION ALL SELECT 'FACTURATION_CONTRATS_IMPORT', 'Importer les contrats depuis CLINIQUE'
  UNION ALL SELECT 'FACTURATION_CONTRATS_EXPORT', 'Exporter les contrats'
  UNION ALL SELECT 'FACTURATION_CONTRATS_HISTORY_VIEW', 'Consulter l''historique des contrats'
) v
CROSS JOIN permissions c
WHERE c.nom = 'SIDEBAR_FACTURATION_CONTRATS'
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.nom = v.nom);

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, s.id, NOW(), NOW()
FROM (
  SELECT 'FACTURATION_SYNC_VIEW' AS nom, 'Consulter la supervision synchronisation CLINIQUE' AS description
  UNION ALL SELECT 'FACTURATION_SYNC_DRY_RUN', 'Exécuter un Dry Run d''import CLINIQUE'
  UNION ALL SELECT 'FACTURATION_SYNC_IMPORT', 'Lancer un import CLINIQUE vers MySQL'
  UNION ALL SELECT 'FACTURATION_SYNC_RETRY', 'Relancer une opération de synchronisation'
  UNION ALL SELECT 'FACTURATION_SYNC_CONFLICT_RESOLVE', 'Résoudre un conflit de synchronisation'
  UNION ALL SELECT 'FACTURATION_SYNC_HISTORY_VIEW', 'Consulter l''historique de synchronisation'
) v
CROSS JOIN permissions s
WHERE s.nom = 'SIDEBAR_FACTURATION_SYNCHRONISATION'
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.nom = v.nom);

UPDATE permissions p
JOIN permissions mf ON mf.nom = 'MODULE_FACTURATION'
SET p.parent = mf.id, p.dateupdate = NOW()
WHERE p.nom IN ('SIDEBAR_FACTURATION_LOCATIONS', 'SIDEBAR_FACTURATION_SYNCHRONISATION');
