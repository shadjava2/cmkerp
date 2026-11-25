-- V25 : Catalogue RBAC Phase 2 — permissions granulaires + FK CASCADE + aliases OUVRIR_*
-- Ne modifie pas V24. Idempotent.

SET @dbname = DATABASE();

-- ---------------------------------------------------------------------------
-- 1. FK ON DELETE CASCADE roles_permissions → permissions
--    MySQL ne déclenche PAS les triggers sur les DELETE en cascade :
--    suppression légitime d'une permission = nettoyage associations ADMIN OK.
-- ---------------------------------------------------------------------------
SET @fkExists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = 'roles_permissions'
    AND CONSTRAINT_NAME = 'fk_roles_permissions_permission'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql = IF(@fkExists > 0, 'SELECT 1',
  'ALTER TABLE roles_permissions
     ADD CONSTRAINT fk_roles_permissions_permission
     FOREIGN KEY (fkPermission) REFERENCES permissions(id)
     ON DELETE CASCADE');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. Permissions granulaires (INSERT si absentes)
-- ---------------------------------------------------------------------------
INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, COALESCE(p.id, 0), NOW(), NOW()
FROM (
  SELECT 'STOCK_VIEW' AS nom, 'Consulter le module stock' AS description, 'MODULE_STOCK' AS parent_nom
  UNION ALL SELECT 'STOCK_PRODUIT_VIEW', 'Consulter les produits', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_CREATE', 'Créer un produit', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_UPDATE', 'Modifier un produit', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_DELETE', 'Supprimer un produit', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRODUIT_FUSION', 'Fusionner des produits CLINIQUE', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PRIX_AJUSTER', 'Ajuster les prix / marges', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_FACTURATION_SYNC', 'Synchroniser facturation Beach↔CH', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_VIEW', 'Consulter les approvisionnements', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_CREATE', 'Créer un approvisionnement', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_UPDATE', 'Modifier un approvisionnement', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_APPROV_CANCEL', 'Annuler un approvisionnement', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_VIEW', 'Consulter les transferts', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_CREATE', 'Créer un transfert', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_VALIDATE', 'Valider un transfert', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_REQUISITION_VIEW', 'Consulter les réquisitions', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_REQUISITION_CREATE', 'Créer une réquisition', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_TRANSFERT_INTERNE', 'Gérer les transferts internes', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_RECEPTION_TI', 'Réceptionner un transfert interne', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_INVENTAIRE_VIEW', 'Consulter les inventaires', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_INVENTAIRE_CREATE', 'Créer un inventaire', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_VENTE_VIEW', 'Consulter les ventes / sorties', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_VENTE_CREATE', 'Créer une vente / sortie usage', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_FOURNISSEUR_VIEW', 'Consulter les fournisseurs', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_PERIMABLE_VIEW', 'Consulter les alertes péremption', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_ORDONNANCE_VIEW', 'Consulter les ordonnances stock', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_POINT_APPEL', 'Gérer le point d''appel', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_DASHBOARD_VIEW', 'Consulter le dashboard stock', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_REPORT_EXPORT', 'Exporter / imprimer rapports stock', 'MODULE_STOCK'
  UNION ALL SELECT 'STOCK_AUTORISATION_DECIDE', 'Décider les autorisations opérations', 'MODULE_STOCK'
  UNION ALL SELECT 'PILOTAGE_VIEW', 'Consulter le pilotage stock', 'MODULE_PILOTAGE_STOCK'
  UNION ALL SELECT 'PILOTAGE_REPORT_RUN', 'Lancer les rapports pilotage', 'MODULE_PILOTAGE_STOCK'
  UNION ALL SELECT 'GMAO_VIEW', 'Consulter le module GMAO', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_VIEW', 'Consulter les équipements', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_CREATE', 'Créer un équipement', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_UPDATE', 'Modifier un équipement', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_EQUIPEMENT_DELETE', 'Supprimer un équipement', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_INTERVENTION_VIEW', 'Consulter les interventions', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_INTERVENTION_CREATE', 'Créer une intervention', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_PLAN_VIEW', 'Consulter les plans préventifs', 'MODULE_GMAO'
  UNION ALL SELECT 'GMAO_INVENTAIRE_VIEW', 'Consulter les inventaires GMAO', 'MODULE_GMAO'
  UNION ALL SELECT 'ADMIN_USAGE_VIEW', 'Consulter l''historique d''utilisation', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'OUVRIR_MODULE_GMAO', 'Alias legacy ouvrir GMAO', 'MODULE_GMAO'
) AS v
LEFT JOIN permissions p ON p.nom = v.parent_nom
WHERE NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = v.nom);

-- Alias legacy : rattacher OUVRIR_* aux modules si présents
UPDATE permissions child
INNER JOIN permissions parent ON parent.nom = 'MODULE_ADMINISTRATION'
SET child.parent = parent.id, child.dateupdate = NOW()
WHERE child.nom = 'OUVRIR_ADMINISTRATION' AND (child.parent IS NULL OR child.parent = 0);

UPDATE permissions child
INNER JOIN permissions parent ON parent.nom = 'MODULE_STOCK'
SET child.parent = parent.id, child.dateupdate = NOW()
WHERE child.nom = 'OUVRIR_MODULE_STOCK' AND (child.parent IS NULL OR child.parent = 0);

UPDATE permissions child
INNER JOIN permissions parent ON parent.nom = 'MODULE_GMAO'
SET child.parent = parent.id, child.dateupdate = NOW()
WHERE child.nom IN ('OUVRIR_MODULE_GMAO') AND (child.parent IS NULL OR child.parent = 0);

-- Sync ADMIN
INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
SELECT 5, p.id, NOW(), NOW()
FROM permissions p
WHERE EXISTS (SELECT 1 FROM roles WHERE id = 5)
  AND NOT EXISTS (
    SELECT 1 FROM roles_permissions rp WHERE rp.fkRole = 5 AND rp.fkPermission = p.id
  );
