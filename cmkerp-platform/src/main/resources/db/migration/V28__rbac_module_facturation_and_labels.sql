-- CMK ERP — V28 : module Facturation + libellés modules portail
-- Idempotent. N'attribue MODULE_FACTURATION à aucun rôle métier (ADMIN via trigger / sync).

UPDATE permissions
SET description = 'Accéder au module Administration', dateupdate = NOW()
WHERE nom = 'MODULE_ADMINISTRATION';

UPDATE permissions
SET description = 'Accéder au module Pilotage', dateupdate = NOW()
WHERE nom = 'MODULE_PILOTAGE_STOCK';

UPDATE permissions
SET description = 'Accéder au module Stock', dateupdate = NOW()
WHERE nom = 'MODULE_STOCK';

UPDATE permissions
SET description = 'Accéder au module GMAO', dateupdate = NOW()
WHERE nom = 'MODULE_GMAO';

UPDATE permissions
SET description = 'Accéder au module DMI', dateupdate = NOW()
WHERE nom = 'MODULE_DMI';

UPDATE permissions
SET description = 'Menu Facturation des soins (DMI)', dateupdate = NOW()
WHERE nom = 'SIDEBAR_FACTURATION';

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT 'MODULE_FACTURATION', 'Accéder au module Facturation', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE nom = 'MODULE_FACTURATION'
);

INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT
  'SIDEBAR_FACTURATION_ACCUEIL',
  'Menu Accueil du module Facturation',
  mf.id,
  NOW(),
  NOW()
FROM permissions mf
WHERE mf.nom = 'MODULE_FACTURATION'
  AND NOT EXISTS (
    SELECT 1 FROM permissions WHERE nom = 'SIDEBAR_FACTURATION_ACCUEIL'
  );
