-- Script de test pour insérer des pharmacies de test
-- Utilisé par les tests d'intégration

-- Insérer une pharmacie de test avec ID 1
INSERT INTO pharmacies (id, fkSite, designation, typepharmacie, codeimmo, typehospi, datecreate, dateupdate, usercreatedid, userupdateid)
VALUES (1, 1, 'Pharmacie de Test', 'Cliente', '001', 'ADMINISTRATION', NOW(), NOW(), 1, 1)
ON DUPLICATE KEY UPDATE
  designation = 'Pharmacie de Test',
  typepharmacie = 'Cliente',
  codeimmo = '001',
  typehospi = 'ADMINISTRATION';
