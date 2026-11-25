-- Libellés français pour MODULE_* / SIDEBAR_* (idempotent)
UPDATE permissions SET description='Module Administration', dateupdate=NOW() WHERE nom='MODULE_ADMINISTRATION';
UPDATE permissions SET description='Module Pilotage du stock', dateupdate=NOW() WHERE nom='MODULE_PILOTAGE_STOCK';
UPDATE permissions SET description='Module Stock et mouvements', dateupdate=NOW() WHERE nom='MODULE_STOCK';
UPDATE permissions SET description='Module GMAO', dateupdate=NOW() WHERE nom='MODULE_GMAO';
UPDATE permissions SET description='Dossier patient', dateupdate=NOW() WHERE nom='MODULE_DMI';
UPDATE permissions SET description='Accéder à l''accueil du dossier patient', dateupdate=NOW() WHERE nom='SIDEBAR_ACCUEIL';
UPDATE permissions SET description='Accéder à la réception', dateupdate=NOW() WHERE nom='SIDEBAR_RECEPTION';
UPDATE permissions SET description='Accéder au cardex des patients', dateupdate=NOW() WHERE nom='SIDEBAR_CARDEX';
UPDATE permissions SET description='Accéder au triage et dispensaire', dateupdate=NOW() WHERE nom='SIDEBAR_TRIAGE';
UPDATE permissions SET description='Accéder au cabinet médical', dateupdate=NOW() WHERE nom='SIDEBAR_CABINET';
UPDATE permissions SET description='Accéder aux urgences', dateupdate=NOW() WHERE nom='SIDEBAR_URGENCES';
UPDATE permissions SET description='Accéder à l''hospitalisation', dateupdate=NOW() WHERE nom='SIDEBAR_HOSPITALISATION';
UPDATE permissions SET description='Accéder aux soins rapprochés', dateupdate=NOW() WHERE nom='SIDEBAR_SOINSRAPPROCHE';
UPDATE permissions SET description='Accéder aux prélèvements', dateupdate=NOW() WHERE nom='SIDEBAR_PRELEVEMENT';
UPDATE permissions SET description='Accéder à la pharmacie DMI', dateupdate=NOW() WHERE nom='SIDEBAR_PHARMACIE';
UPDATE permissions SET description='Accéder à la facturation des soins', dateupdate=NOW() WHERE nom='SIDEBAR_FACTURATION';
