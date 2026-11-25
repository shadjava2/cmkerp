-- CMK ERP — V29 : Facturation V2 — menus SIDEBAR_* + KPI dashboard opérationnels
-- Idempotent. Aucune attribution métier hors ADMIN (trigger / syncAdmin).
-- Conserve SIDEBAR_FACTURATION sous MODULE_DMI (parcours clinique).

-- Libellé Accueil → Tableau de bord
UPDATE permissions
SET description = 'Menu Tableau de bord du module Facturation', dateupdate = NOW()
WHERE nom = 'SIDEBAR_FACTURATION_ACCUEIL';

-- Menus navigation (parent = MODULE_FACTURATION)
INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, mf.id, NOW(), NOW()
FROM (
  SELECT 'SIDEBAR_FACTURATION_CONTRATS' AS nom, 'Menu Contrats du module Facturation' AS description
  UNION ALL SELECT 'SIDEBAR_FACTURATION_AYANTSDROIT', 'Menu Ayants droit du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_SOCIETES', 'Menu Sociétés du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_PRESTATIONS', 'Menu Prestations du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_MEDICAMENTS', 'Menu Médicaments du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_TARIFS', 'Menu Tarifs du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_IMPUTATIONS', 'Menu Imputations du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_CLASSES', 'Menu Classes du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_MEDECINS', 'Menu Médecins du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_SERVICES', 'Menu Services du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_PHARMACIES', 'Menu Pharmacies du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_PRODUITS', 'Menu Produits du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_STOCK', 'Menu Stock du module Facturation'
  UNION ALL SELECT 'SIDEBAR_FACTURATION_UTILISATEURS', 'Menu Utilisateurs du module Facturation'
) v
CROSS JOIN permissions mf
WHERE mf.nom = 'MODULE_FACTURATION'
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.nom = v.nom);

-- KPI / graphiques dashboard réellement branchés
INSERT INTO permissions (nom, description, parent, datecreate, dateupdate)
SELECT v.nom, v.description, acc.id, NOW(), NOW()
FROM (
  SELECT 'FACTURATION_DASHBOARD_KPI_MEDICAMENTS_VIEW' AS nom,
         'Voir le KPI médicaments facturés (dashboard)' AS description
  UNION ALL SELECT 'FACTURATION_DASHBOARD_KPI_PRIVES_VIEW',
         'Voir le KPI patients privés (dashboard)'
  UNION ALL SELECT 'FACTURATION_DASHBOARD_KPI_CONTRATS_USAGE_VIEW',
         'Voir le KPI facturation sous contrat (dashboard)'
  UNION ALL SELECT 'FACTURATION_DASHBOARD_CHART_EVOLUTION_VIEW',
         'Voir le graphique d''évolution (dashboard)'
  UNION ALL SELECT 'FACTURATION_DASHBOARD_TABLE_PRODUITS_VIEW',
         'Voir le tableau produits facturés (dashboard)'
) v
CROSS JOIN permissions acc
WHERE acc.nom = 'SIDEBAR_FACTURATION_ACCUEIL'
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.nom = v.nom);

-- Parentage correct si permission déjà créée sans parent
UPDATE permissions p
JOIN permissions mf ON mf.nom = 'MODULE_FACTURATION'
SET p.parent = mf.id, p.dateupdate = NOW()
WHERE p.nom IN (
  'SIDEBAR_FACTURATION_ACCUEIL',
  'SIDEBAR_FACTURATION_CONTRATS',
  'SIDEBAR_FACTURATION_AYANTSDROIT',
  'SIDEBAR_FACTURATION_SOCIETES',
  'SIDEBAR_FACTURATION_PRESTATIONS',
  'SIDEBAR_FACTURATION_MEDICAMENTS',
  'SIDEBAR_FACTURATION_TARIFS',
  'SIDEBAR_FACTURATION_IMPUTATIONS',
  'SIDEBAR_FACTURATION_CLASSES',
  'SIDEBAR_FACTURATION_MEDECINS',
  'SIDEBAR_FACTURATION_SERVICES',
  'SIDEBAR_FACTURATION_PHARMACIES',
  'SIDEBAR_FACTURATION_PRODUITS',
  'SIDEBAR_FACTURATION_STOCK',
  'SIDEBAR_FACTURATION_UTILISATEURS'
);

UPDATE permissions p
JOIN permissions acc ON acc.nom = 'SIDEBAR_FACTURATION_ACCUEIL'
SET p.parent = acc.id, p.dateupdate = NOW()
WHERE p.nom IN (
  'FACTURATION_DASHBOARD_KPI_MEDICAMENTS_VIEW',
  'FACTURATION_DASHBOARD_KPI_PRIVES_VIEW',
  'FACTURATION_DASHBOARD_KPI_CONTRATS_USAGE_VIEW',
  'FACTURATION_DASHBOARD_CHART_EVOLUTION_VIEW',
  'FACTURATION_DASHBOARD_TABLE_PRODUITS_VIEW'
);
