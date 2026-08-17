-- ==========================================
-- Migration V8 : Création des tables pour les transferts internes
-- ==========================================
-- Date: 2025-12-18
-- Description: Création des tables transfert_interne et lignes_transfert_interne
--              pour gérer les transferts de stock entre pharmacies/services
-- ==========================================

-- Table: transfert_interne
CREATE TABLE IF NOT EXISTS `transfert_interne` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkPharmacieSource` bigint UNSIGNED NOT NULL,
  `fkPharmacieDestination` bigint UNSIGNED NOT NULL,
  `statut` enum('EN ATTENTE','TRANSFEREE','ANNULEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `commentaire` mediumtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_transfert_interne_source`(`fkPharmacieSource` ASC, `statut` ASC, `datecreate` ASC) USING BTREE,
  INDEX `index_transfert_interne_destination`(`fkPharmacieDestination` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: lignes_transfert_interne
CREATE TABLE IF NOT EXISTS `lignes_transfert_interne` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkTransfertInterne` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `quantite` float NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_ligne_transfert_interne_fktransfert`(`fkTransfertInterne` ASC, `datecreate` ASC) USING BTREE,
  INDEX `index_ligne_transfert_interne_fkstock`(`fkStock` ASC) USING BTREE,
  CONSTRAINT `fk_ligne_transfert_interne_transfert` FOREIGN KEY (`fkTransfertInterne`) REFERENCES `transfert_interne` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

