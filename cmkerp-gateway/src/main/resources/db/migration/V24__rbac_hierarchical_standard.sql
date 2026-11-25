-- V24 : Standard RBAC hiérarchique CMK ERP
-- - Colonne parent sur permissions (et roles_permissions pour alignement schéma)
-- - Catalogue MODULE_* / SIDEBAR_* (IDs 436–451 conservés)
-- - Triggers de synchronisation automatique du rôle système ADMIN (id=5)
-- - Unicité (fkRole, fkPermission) + index parent
-- Idempotent : compatible environnements où parent / MODULE_* existent déjà.

SET @dbname = DATABASE();

-- ---------------------------------------------------------------------------
-- 1. Colonne parent sur permissions
-- ---------------------------------------------------------------------------
SET @tablename = 'permissions';
SET @columnname = 'parent';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE permissions ADD COLUMN parent BIGINT NULL DEFAULT 0 AFTER description'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'permissions' AND INDEX_NAME = 'idx_permissions_parent') > 0,
  'SELECT 1',
  'CREATE INDEX idx_permissions_parent ON permissions (parent)'
));
PREPARE idxIfNotExists FROM @preparedStatement;
EXECUTE idxIfNotExists;
DEALLOCATE PREPARE idxIfNotExists;

-- ---------------------------------------------------------------------------
-- 2. Colonne parent sur roles_permissions (alignement schéma live)
-- ---------------------------------------------------------------------------
SET @tablename = 'roles_permissions';
SET @columnname = 'parent';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE roles_permissions ADD COLUMN parent BIGINT NULL DEFAULT NULL AFTER fkPermission'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'roles_permissions'
     AND INDEX_NAME = 'uk_roles_permissions_role_perm') > 0,
  'SELECT 1',
  'ALTER TABLE roles_permissions ADD UNIQUE INDEX uk_roles_permissions_role_perm (fkRole, fkPermission)'
));
PREPARE ukIfNotExists FROM @preparedStatement;
EXECUTE ukIfNotExists;
DEALLOCATE PREPARE ukIfNotExists;

-- ---------------------------------------------------------------------------
-- 3. Catalogue hiérarchique (IDs stables 436–451)
-- ---------------------------------------------------------------------------
INSERT INTO permissions (id, nom, description, parent, datecreate, dateupdate)
VALUES
  (436, 'MODULE_ADMINISTRATION', 'Module Administration', 0, NOW(), NOW()),
  (437, 'MODULE_PILOTAGE_STOCK', 'Module Pilotage du stock', 0, NOW(), NOW()),
  (438, 'MODULE_STOCK', 'Module Stock et mouvements', 0, NOW(), NOW()),
  (439, 'MODULE_GMAO', 'Module GMAO', 0, NOW(), NOW()),
  (440, 'MODULE_DMI', 'Module Dossier médical informatisé', 0, NOW(), NOW()),
  (441, 'SIDEBAR_ACCUEIL', 'Menu Accueil DMI', 440, NOW(), NOW()),
  (442, 'SIDEBAR_RECEPTION', 'Menu Réception DMI', 440, NOW(), NOW()),
  (443, 'SIDEBAR_CARDEX', 'Menu Cardex DMI', 440, NOW(), NOW()),
  (444, 'SIDEBAR_TRIAGE', 'Menu Triage DMI', 440, NOW(), NOW()),
  (445, 'SIDEBAR_CABINET', 'Menu Cabinet médical DMI', 440, NOW(), NOW()),
  (446, 'SIDEBAR_URGENCES', 'Menu Urgences DMI', 440, NOW(), NOW()),
  (447, 'SIDEBAR_HOSPITALISATION', 'Menu Hospitalisation DMI', 440, NOW(), NOW()),
  (448, 'SIDEBAR_SOINSRAPPROCHE', 'Menu Soins rapprochés DMI', 440, NOW(), NOW()),
  (449, 'SIDEBAR_PRELEVEMENT', 'Menu Prélèvement DMI', 440, NOW(), NOW()),
  (450, 'SIDEBAR_PHARMACIE', 'Menu Pharmacie DMI', 440, NOW(), NOW()),
  (451, 'SIDEBAR_FACTURATION', 'Menu Facturation DMI', 440, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  parent = VALUES(parent),
  dateupdate = NOW();

-- Sous-permissions métier DMI (nouveaux IDs, parents = SIDEBAR_*)
INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, p.id, NOW(), NOW()
FROM (
  SELECT 'DMI_ACCUEIL_VIEW' AS nom, 'Consulter l''accueil DMI' AS description, 'SIDEBAR_ACCUEIL' AS parent_nom
  UNION ALL SELECT 'DMI_RECEPTION_PATIENT_VIEW', 'Consulter les patients (réception)', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_PATIENT_CREATE', 'Enregistrer un patient', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_RECEPTION_PATIENT_UPDATE', 'Modifier un patient', 'SIDEBAR_RECEPTION'
  UNION ALL SELECT 'DMI_CARDEX_VIEW', 'Consulter le cardex', 'SIDEBAR_CARDEX'
  UNION ALL SELECT 'DMI_TRIAGE_VIEW', 'Accéder au triage', 'SIDEBAR_TRIAGE'
  UNION ALL SELECT 'DMI_CABINET_PATIENT_VIEW', 'Consulter le dossier patient', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_CONSULTATION_CREATE', 'Créer une consultation', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_PRESCRIPTION_CREATE', 'Prescrire un médicament', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_CABINET_CONSULTATION_CANCEL', 'Annuler une consultation', 'SIDEBAR_CABINET'
  UNION ALL SELECT 'DMI_URGENCES_VIEW', 'Accéder aux urgences', 'SIDEBAR_URGENCES'
  UNION ALL SELECT 'DMI_HOSPITALISATION_VIEW', 'Accéder à l''hospitalisation', 'SIDEBAR_HOSPITALISATION'
  UNION ALL SELECT 'DMI_SOINSRAPPROCHE_VIEW', 'Accéder aux soins rapprochés', 'SIDEBAR_SOINSRAPPROCHE'
  UNION ALL SELECT 'DMI_PRELEVEMENT_VIEW', 'Accéder aux prélèvements', 'SIDEBAR_PRELEVEMENT'
  UNION ALL SELECT 'DMI_PHARMACIE_VIEW', 'Accéder à la pharmacie DMI', 'SIDEBAR_PHARMACIE'
  UNION ALL SELECT 'DMI_FACTURATION_VIEW', 'Consulter la facturation des soins', 'SIDEBAR_FACTURATION'
  UNION ALL SELECT 'ADMIN_USERS_VIEW', 'Consulter les utilisateurs', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_CREATE', 'Créer un utilisateur', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_UPDATE', 'Modifier un utilisateur', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_MANAGE', 'Gérer les rôles et permissions', 'MODULE_ADMINISTRATION'
) AS v
INNER JOIN permissions p ON p.nom = v.parent_nom
WHERE NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = v.nom);

-- Aligner parent des permissions MODULE/SIDEBAR déjà présentes
UPDATE permissions SET parent = 0, dateupdate = NOW()
WHERE nom IN ('MODULE_ADMINISTRATION','MODULE_PILOTAGE_STOCK','MODULE_STOCK','MODULE_GMAO','MODULE_DMI')
  AND (parent IS NULL OR parent <> 0);

UPDATE permissions p
INNER JOIN permissions parent ON parent.nom = 'MODULE_DMI'
SET p.parent = parent.id, p.dateupdate = NOW()
WHERE p.nom LIKE 'SIDEBAR_%' AND (p.parent IS NULL OR p.parent = 0 OR p.parent <> parent.id);

-- ---------------------------------------------------------------------------
-- 4. Procédures + triggers synchronisation ADMIN (id=5)
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sync_permission_to_admin;
DROP PROCEDURE IF EXISTS remove_permission_from_admin;
DROP PROCEDURE IF EXISTS sync_all_permissions_to_admin;

DELIMITER $$

CREATE PROCEDURE sync_permission_to_admin(IN p_permission_id BIGINT)
BEGIN
  DECLARE admin_role_id BIGINT DEFAULT 5;
  IF p_permission_id IS NOT NULL AND EXISTS (SELECT 1 FROM roles WHERE id = admin_role_id) THEN
    INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
    VALUES (admin_role_id, p_permission_id, NOW(), NOW());
  END IF;
END$$

CREATE PROCEDURE remove_permission_from_admin(IN p_permission_id BIGINT)
BEGIN
  DECLARE admin_role_id BIGINT DEFAULT 5;
  IF p_permission_id IS NOT NULL THEN
    SET @cmk_rbac_bypass_admin_protect = 1;
    DELETE FROM roles_permissions
    WHERE fkRole = admin_role_id AND fkPermission = p_permission_id;
    SET @cmk_rbac_bypass_admin_protect = 0;
  END IF;
END$$

CREATE PROCEDURE sync_all_permissions_to_admin()
BEGIN
  DECLARE admin_role_id BIGINT DEFAULT 5;
  INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
  SELECT admin_role_id, p.id, NOW(), NOW()
  FROM permissions p
  WHERE NOT EXISTS (
    SELECT 1 FROM roles_permissions rp
    WHERE rp.fkRole = admin_role_id AND rp.fkPermission = p.id
  );
END$$

DELIMITER ;

DROP TRIGGER IF EXISTS trg_permissions_after_insert_sync_admin;
DROP TRIGGER IF EXISTS trg_permissions_after_delete_sync_admin;
DROP TRIGGER IF EXISTS trg_roles_permissions_before_delete_protect_admin;
DROP TRIGGER IF EXISTS trg_roles_permissions_before_update_protect_admin;

DELIMITER $$

CREATE TRIGGER trg_permissions_after_insert_sync_admin
AFTER INSERT ON permissions
FOR EACH ROW
BEGIN
  CALL sync_permission_to_admin(NEW.id);
END$$

CREATE TRIGGER trg_permissions_after_delete_sync_admin
AFTER DELETE ON permissions
FOR EACH ROW
BEGIN
  CALL remove_permission_from_admin(OLD.id);
END$$

-- Empêche le retrait manuel des associations ADMIN (hors suppression de permission)
CREATE TRIGGER trg_roles_permissions_before_delete_protect_admin
BEFORE DELETE ON roles_permissions
FOR EACH ROW
BEGIN
  IF OLD.fkRole = 5
     AND IFNULL(@cmk_rbac_bypass_admin_protect, 0) = 0
     AND EXISTS (SELECT 1 FROM permissions WHERE id = OLD.fkPermission) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Le rôle système ADMIN (id=5) ne peut pas perdre une permission existante';
  END IF;
END$$

CREATE TRIGGER trg_roles_permissions_before_update_protect_admin
BEFORE UPDATE ON roles_permissions
FOR EACH ROW
BEGIN
  IF OLD.fkRole = 5 OR NEW.fkRole = 5 THEN
    IF OLD.fkPermission <> NEW.fkPermission OR OLD.fkRole <> NEW.fkRole THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Les associations du rôle système ADMIN sont protégées';
    END IF;
  END IF;
END$$

DELIMITER ;

-- Synchronisation initiale
CALL sync_all_permissions_to_admin();
