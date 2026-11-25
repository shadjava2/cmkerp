-- V26 : Permissions granulaires de la console Administration RBAC
-- Ne modifie pas V24/V25. Idempotent.

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, COALESCE(p.id, 0), NOW(), NOW()
FROM (
  SELECT 'ADMIN_ROLES_VIEW' AS nom, 'Consulter les rôles' AS description, 'MODULE_ADMINISTRATION' AS parent_nom
  UNION ALL SELECT 'ADMIN_ROLES_CREATE', 'Créer un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_UPDATE', 'Modifier un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_DELETE', 'Supprimer un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_DUPLICATE', 'Dupliquer un rôle métier', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_ROLES_PERMISSIONS_UPDATE', 'Modifier les permissions d''un rôle', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_PERMISSIONS_CATALOG_VIEW', 'Consulter le catalogue des permissions', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_ROLE_ASSIGN', 'Attribuer un rôle à un utilisateur', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_USERS_PERMISSIONS_VIEW', 'Consulter les permissions effectives d''un utilisateur', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_RBAC_AUDIT_VIEW', 'Consulter l''audit des modifications de droits', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_RBAC_INTEGRITY_VIEW', 'Vérifier l''intégrité du rôle ADMIN', 'MODULE_ADMINISTRATION'
  UNION ALL SELECT 'ADMIN_RBAC_INTEGRITY_REPAIR', 'Réparer la synchronisation ADMIN', 'MODULE_ADMINISTRATION'
) AS v
LEFT JOIN permissions p ON p.nom = v.parent_nom
WHERE NOT EXISTS (SELECT 1 FROM permissions x WHERE x.nom = v.nom);

-- Attribution ADMIN via trigger AFTER INSERT existant (ou sync initializer).
