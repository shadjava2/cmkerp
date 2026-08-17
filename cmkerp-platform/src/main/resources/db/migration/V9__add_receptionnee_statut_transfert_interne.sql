-- ==========================================
-- Migration V9 : Ajout du statut RECEPTIONNEE pour les transferts internes
-- ==========================================
-- Date: 2025-12-18
-- Description: Ajout du statut 'RECEPTIONNEE' à l'enum de la table transfert_interne
--              pour permettre de marquer un transfert comme réceptionné
-- ==========================================

-- Modifier l'enum pour ajouter 'RECEPTIONNEE'
ALTER TABLE `transfert_interne`
MODIFY COLUMN `statut` enum('EN ATTENTE','TRANSFEREE','ANNULEE','RECEPTIONNEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE';

