-- Script de test pour insérer des ventes de test
-- Utilisé par les tests d'intégration

-- Insérer une vente de test avec ID 1 pour la pharmacie ID 1
INSERT INTO ventes (id, fkEntreprise, fkPatient, fkPharmacie, statut, taux, typepaiement, raisonsortie, demandeur, fkPatientMediline, fkFicheMedicale, datecreate, dateupdate, usercreateid, userupdateid)
VALUES (1, 1, NULL, 1, 'EN ATTENTE', NULL, NULL, 'Test Integration', 'Test User', NULL, NULL, NOW(), NOW(), 1, 1)
ON DUPLICATE KEY UPDATE
  fkPharmacie = 1,
  statut = 'EN ATTENTE',
  raisonsortie = 'Test Integration',
  demandeur = 'Test User';
