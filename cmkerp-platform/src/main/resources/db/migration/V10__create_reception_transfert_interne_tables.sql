-- ==========================================
-- Migration V10 : Création des tables pour les réceptions de transferts internes
-- ==========================================
-- Date: 2025-12-18
-- Description: Création des tables reception_transfert_interne et lignes_reception_transfert_interne
--              pour gérer la réception des transferts internes par la pharmacie destination
-- ==========================================

-- Table: reception_transfert_interne
CREATE TABLE IF NOT EXISTS `reception_transfert_interne` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkTransfertInterne` bigint NOT NULL,
  `statut` enum('EN ATTENTE','RECEPTIONNEE','ANNULEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_reception_transfert_interne_fktransfert`(`fkTransfertInterne` ASC, `statut` ASC, `datecreate` ASC) USING BTREE,
  CONSTRAINT `fk_reception_transfert_interne_transfert` FOREIGN KEY (`fkTransfertInterne`) REFERENCES `transfert_interne` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: lignes_reception_transfert_interne
-- Note: Utilise fkReceptionStock pour correspondre à la structure existante de la base de données
CREATE TABLE IF NOT EXISTS `lignes_reception_transfert_interne` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkReceptionStock` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `quantiteDemandee` float NULL DEFAULT NULL,
  `quantiteTransferee` float NULL DEFAULT NULL,
  `quantite` float NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_ligne_reception_fkreception`(`fkReceptionStock` ASC, `datecreate` ASC) USING BTREE,
  INDEX `index_ligne_reception_fkstock`(`fkStock` ASC) USING BTREE,
  CONSTRAINT `fk_ligne_reception_transfert_interne_reception` FOREIGN KEY (`fkReceptionStock`) REFERENCES `reception_transfert_interne` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

