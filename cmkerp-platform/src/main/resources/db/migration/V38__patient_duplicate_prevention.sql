-- Prévention des doublons patients (DMI) — audit décisions + permissions.

CREATE TABLE IF NOT EXISTS erp_patient_duplicate_decision (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id      CHAR(36)      NOT NULL,
  fingerprint     VARCHAR(64)   NOT NULL,
  decision        VARCHAR(40)   NOT NULL,
  justification   VARCHAR(1000) NULL,
  candidates_json MEDIUMTEXT    NULL,
  usercreateid    BIGINT        NULL,
  datecreate      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dup_decision_request (request_id),
  INDEX idx_dup_decision_created (datecreate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Permissions DMI anti-doublons (parent = SIDEBAR_CARDEX si présent).
INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'DMI_PATIENT_DUPLICATE_DETECT', 'Détecter les doublons patients avant création', p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom = 'SIDEBAR_CARDEX'
  AND NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = 'DMI_PATIENT_DUPLICATE_DETECT');

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'DMI_PATIENT_DUPLICATE_COMPARE', 'Comparer les identités patient détectées', p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom = 'SIDEBAR_CARDEX'
  AND NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = 'DMI_PATIENT_DUPLICATE_COMPARE');

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'DMI_PATIENT_DUPLICATE_USE_EXISTING', 'Utiliser un dossier patient existant proposé', p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom = 'SIDEBAR_CARDEX'
  AND NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = 'DMI_PATIENT_DUPLICATE_USE_EXISTING');

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'DMI_PATIENT_DUPLICATE_OVERRIDE', 'Continuer la création malgré une correspondance forte', p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom = 'SIDEBAR_CARDEX'
  AND NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = 'DMI_PATIENT_DUPLICATE_OVERRIDE');

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'DMI_PATIENT_DUPLICATE_AUDIT_VIEW', 'Consulter l''audit des décisions anti-doublons', p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom = 'SIDEBAR_CARDEX'
  AND NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = 'DMI_PATIENT_DUPLICATE_AUDIT_VIEW');
