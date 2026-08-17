/*
 Navicat Premium Dump SQL

 Source Server         : SERVEUR_LOCAL_MYSQL8
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : 127.0.0.1:32768
 Source Schema         : cmkerp-v24prod

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 26/06/2026 11:52:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agents
-- ----------------------------
DROP TABLE IF EXISTS `agents`;
CREATE TABLE `agents`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `photo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `matricule` char(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `nom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `postnom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `prenom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `genre` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `fonction` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `etatc` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `datenaiss` date NULL DEFAULT NULL,
  `lieunaiss` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `dateengagement` date NULL DEFAULT NULL,
  `fkPersonne` bigint NULL DEFAULT NULL,
  `fkCategorie` bigint NULL DEFAULT NULL,
  `carte` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `dateLog` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `userLog` bigint NULL DEFAULT NULL,
  `delData` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `carteagent`(`carte` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 166 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agentss
-- ----------------------------
DROP TABLE IF EXISTS `agentss`;
CREATE TABLE `agentss`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `photo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `matricule` char(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `nom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `postnom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `prenom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `genre` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `etatc` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `datenaiss` date NULL DEFAULT NULL,
  `lieunaiss` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `adresse` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `telephone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `nomreference` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `contactreference` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `fkPersonne` bigint NULL DEFAULT NULL,
  `carte` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `carteagent`(`carte` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for approvsionnements
-- ----------------------------
DROP TABLE IF EXISTS `approvsionnements`;
CREATE TABLE `approvsionnements`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkFournisseur` bigint UNSIGNED NOT NULL,
  `fkPharmacie` bigint UNSIGNED NOT NULL,
  `fkEchangeDevise` bigint UNSIGNED NULL DEFAULT NULL,
  `statut` enum('EN ATTENTE','VALIDEE','ANNULEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'EN ATTENTE',
  `numbonliv` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `taux` smallint NULL DEFAULT NULL,
  `datebonliv` date NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_appro_unique`(`fkFournisseur` ASC, `fkPharmacie` ASC, `fkEchangeDevise` ASC) USING BTREE,
  INDEX `index_approv_optimis`(`fkPharmacie` ASC, `statut` ASC, `datecreate` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3086 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for audit_events
-- ----------------------------
DROP TABLE IF EXISTS `audit_events`;
CREATE TABLE `audit_events`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'UUID de l\'événement (depuis DomainEvent)',
  `event_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Type d\'événement (AUDIT_EVENT)',
  `event_timestamp` datetime NOT NULL COMMENT 'Timestamp de l\'événement (depuis DomainEvent)',
  `user_id` bigint NULL DEFAULT NULL COMMENT 'ID de l\'utilisateur ayant effectué l\'action',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Nom d\'utilisateur',
  `action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Action effectuée (CREATE, UPDATE, DELETE, etc.)',
  `resource_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Type de ressource affectée (User, Pharmacie, etc.)',
  `resource_id` bigint NULL DEFAULT NULL COMMENT 'ID de la ressource affectée',
  `details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Détails supplémentaires de l\'action (JSON ou texte)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création de l\'enregistrement',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT 'Index pour recherche par utilisateur',
  INDEX `idx_action`(`action` ASC) USING BTREE COMMENT 'Index pour recherche par action',
  INDEX `idx_resource`(`resource_type` ASC, `resource_id` ASC) USING BTREE COMMENT 'Index pour recherche par ressource',
  INDEX `idx_event_timestamp`(`event_timestamp` ASC) USING BTREE COMMENT 'Index pour recherche par date',
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE COMMENT 'Index pour recherche par date de création',
  INDEX `idx_event_id`(`event_id` ASC) USING BTREE COMMENT 'Index unique pour éviter les doublons'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Table d\'audit pour événements d\'audit persistés depuis Kafka' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bureau_immo
-- ----------------------------
DROP TABLE IF EXISTS `bureau_immo`;
CREATE TABLE `bureau_immo`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkLocalisation` bigint NULL DEFAULT NULL,
  `codeimmo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for carrieres
-- ----------------------------
DROP TABLE IF EXISTS `carrieres`;
CREATE TABLE `carrieres`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkAgent` bigint NULL DEFAULT NULL,
  `fkPoste` bigint NULL DEFAULT NULL,
  `fkTypeMouvement` bigint NULL DEFAULT NULL,
  `fkService` bigint NULL DEFAULT NULL,
  `niveau` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `datedebut` date NULL DEFAULT NULL,
  `datefin` date NULL DEFAULT NULL,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for categorie_agent
-- ----------------------------
DROP TABLE IF EXISTS `categorie_agent`;
CREATE TABLE `categorie_agent`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for categorie_immo
-- ----------------------------
DROP TABLE IF EXISTS `categorie_immo`;
CREATE TABLE `categorie_immo`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `iduncatimmoindex`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for categorie_produit
-- ----------------------------
DROP TABLE IF EXISTS `categorie_produit`;
CREATE TABLE `categorie_produit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `abbreviation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chambres
-- ----------------------------
DROP TABLE IF EXISTS `chambres`;
CREATE TABLE `chambres`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkService` bigint NULL DEFAULT NULL,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_messages
-- ----------------------------
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `sender` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `receiver` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UNIQUEROLE`(`sender` ASC) USING BTREE,
  INDEX `ndex_role_unique`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for conditionnements
-- ----------------------------
DROP TABLE IF EXISTS `conditionnements`;
CREATE TABLE `conditionnements`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for conges
-- ----------------------------
DROP TABLE IF EXISTS `conges`;
CREATE TABLE `conges`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkAgent` bigint NULL DEFAULT NULL,
  `fkTypeConge` bigint NULL DEFAULT NULL,
  `date_debut` date NULL DEFAULT NULL,
  `date_fin` date NULL DEFAULT NULL,
  `duree` int NULL DEFAULT NULL,
  `statut` enum('En attente','Approuvé','Refusé','Annulé') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'En attente',
  `motif` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `certificat_medical` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `approuve_par` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `date_approbation` date NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for consultations
-- ----------------------------
DROP TABLE IF EXISTS `consultations`;
CREATE TABLE `consultations`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkPhase` bigint NULL DEFAULT NULL,
  `fkFicheMedicale` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `codeIPP` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `commentaire` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `service` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4081 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for conventions
-- ----------------------------
DROP TABLE IF EXISTS `conventions`;
CREATE TABLE `conventions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `indice` float UNSIGNED NULL DEFAULT NULL,
  `indicehospi` float UNSIGNED NULL DEFAULT NULL,
  `indiceurgence` float UNSIGNED NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreatedid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdatedid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_convention_unique`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for devenir
-- ----------------------------
DROP TABLE IF EXISTS `devenir`;
CREATE TABLE `devenir`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkFicheMedicale` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `iddeces` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '-',
  `service` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '-',
  `fkOrientation` bigint NULL DEFAULT NULL,
  `biologies` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `propostions` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `documentsjoin` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `deces` tinyint(1) NULL DEFAULT NULL,
  `datedevenir` date NULL DEFAULT NULL,
  `heuredeces` time NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 341 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for document_administratif
-- ----------------------------
DROP TABLE IF EXISTS `document_administratif`;
CREATE TABLE `document_administratif`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkAgent` bigint NULL DEFAULT NULL,
  `fkTypeDocument` bigint NULL DEFAULT NULL,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `note` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `fichier_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dosages
-- ----------------------------
DROP TABLE IF EXISTS `dosages`;
CREATE TABLE `dosages`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 443 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for droits_categorie
-- ----------------------------
DROP TABLE IF EXISTS `droits_categorie`;
CREATE TABLE `droits_categorie`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkPharmacie` bigint UNSIGNED NULL DEFAULT NULL,
  `fkCategorie` bigint UNSIGNED NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_droitcategorie_unique`(`fkPharmacie` ASC, `fkCategorie` ASC, `id` ASC) USING BTREE,
  INDEX `idx_droits_pharma_cat`(`fkPharmacie` ASC, `fkCategorie` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 801 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for droits_pharmacies
-- ----------------------------
DROP TABLE IF EXISTS `droits_pharmacies`;
CREATE TABLE `droits_pharmacies`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkUtilisateur` bigint UNSIGNED NULL DEFAULT NULL,
  `fkPharmacie` bigint UNSIGNED NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_droitpharmacie_unique`(`fkUtilisateur` ASC, `fkPharmacie` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1135 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for echange_devise
-- ----------------------------
DROP TABLE IF EXISTS `echange_devise`;
CREATE TABLE `echange_devise`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `monnaieprincipale` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `tauxechange` float UNSIGNED NULL DEFAULT NULL,
  `monnaieechange` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `symbole` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_devise_unique`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for entreprises
-- ----------------------------
DROP TABLE IF EXISTS `entreprises`;
CREATE TABLE `entreprises`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `adresse` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `numtel` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkConvention` bigint UNSIGNED NOT NULL,
  `cmk` tinyint(1) NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_entreprise_unique`(`id` ASC, `fkConvention` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 211 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `checksum` int NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for formes
-- ----------------------------
DROP TABLE IF EXISTS `formes`;
CREATE TABLE `formes`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fournisseurs
-- ----------------------------
DROP TABLE IF EXISTS `fournisseurs`;
CREATE TABLE `fournisseurs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `adresse` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `telephone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 232 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for immobilisation
-- ----------------------------
DROP TABLE IF EXISTS `immobilisation`;
CREATE TABLE `immobilisation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `fkSousCategorie` bigint NULL DEFAULT NULL,
  `fkService` bigint NULL DEFAULT NULL,
  `fkBureau` bigint NULL DEFAULT NULL,
  `valeurInitiale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `dateAcquisition` datetime NULL DEFAULT NULL,
  `dureeAmortissement` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `valeurResiduelle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `fkSite` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `etat` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for inventaires
-- ----------------------------
DROP TABLE IF EXISTS `inventaires`;
CREATE TABLE `inventaires`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkPharmacie` bigint NOT NULL,
  `date_debut` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `date_fin` datetime NULL DEFAULT NULL,
  `statut` enum('EN COURS','TERMINE','ANNULE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN COURS',
  `commentaire` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `typeinventaire` enum('PHYSIQUE','AJUSTEMENT','MENSUEL','PERIME') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'PHYSIQUE',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2325 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_approv
-- ----------------------------
DROP TABLE IF EXISTS `lignes_approv`;
CREATE TABLE `lignes_approv`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkApprov` bigint NOT NULL,
  `fkStock` bigint NULL DEFAULT NULL,
  `qt` float NULL DEFAULT NULL,
  `prixachat` decimal(10, 2) NULL DEFAULT NULL,
  `prixachattotal` decimal(10, 2) NULL DEFAULT NULL,
  `totalfournisseur` decimal(10, 2) NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index-approv-optimis`(`fkApprov` ASC, `fkStock` ASC, `datecreate` ASC) USING BTREE,
  INDEX `index_approv_index`(`fkStock` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14258 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_inventaire
-- ----------------------------
DROP TABLE IF EXISTS `lignes_inventaire`;
CREATE TABLE `lignes_inventaire`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkInventaire` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `quantite_theorique` float NOT NULL,
  `quantite_physique` float NOT NULL,
  `ecart` float GENERATED ALWAYS AS ((`quantite_physique` - `quantite_theorique`)) VIRTUAL NULL,
  `commentaire` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_optim_ligne_invent`(`fkInventaire` ASC, `fkStock` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7108042 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_ordonnance
-- ----------------------------
DROP TABLE IF EXISTS `lignes_ordonnance`;
CREATE TABLE `lignes_ordonnance`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkOrdonnance` bigint NOT NULL,
  `fkStock` bigint NULL DEFAULT NULL,
  `posologie` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `duree` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `frequence` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `voie_administration` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `condition_prise` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `unite_dose` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `qt` float NULL DEFAULT NULL,
  `instruction_patient` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_reception_stock
-- ----------------------------
DROP TABLE IF EXISTS `lignes_reception_stock`;
CREATE TABLE `lignes_reception_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkReceptionStock` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `quantiteDemandee` float NULL DEFAULT NULL,
  `quantiteTransferee` float NULL DEFAULT NULL,
  `quantite` float NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_lrs_reception_stock`(`fkReceptionStock` ASC, `fkStock` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 139388 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_reception_transfert_interne
-- ----------------------------
DROP TABLE IF EXISTS `lignes_reception_transfert_interne`;
CREATE TABLE `lignes_reception_transfert_interne`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkReceptionStock` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `fkAlertePeremption` bigint NULL DEFAULT NULL,
  `quantiteDemandee` float NULL DEFAULT NULL,
  `quantiteTransferee` float NULL DEFAULT NULL,
  `quantite` float NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_lrs_reception_stock`(`fkReceptionStock` ASC, `fkStock` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_requisitions
-- ----------------------------
DROP TABLE IF EXISTS `lignes_requisitions`;
CREATE TABLE `lignes_requisitions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkRequisition` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `quantite` float NOT NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 113407 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_transfert_interne
-- ----------------------------
DROP TABLE IF EXISTS `lignes_transfert_interne`;
CREATE TABLE `lignes_transfert_interne`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkTransfertInterne` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `fkAlertePeremption` bigint NULL DEFAULT NULL,
  `quantite` float NOT NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_transferts_stock
-- ----------------------------
DROP TABLE IF EXISTS `lignes_transferts_stock`;
CREATE TABLE `lignes_transferts_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkTransfertStock` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `quantiteDemandee` float NULL DEFAULT NULL,
  `quantite` float UNSIGNED NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_ligne_transfert`(`fkTransfertStock` ASC, `fkStock` ASC) USING BTREE,
  INDEX `index_ligne_transfert-optimis`(`fkTransfertStock` ASC, `fkStock` ASC, `datecreate` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 193941 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lignes_vente
-- ----------------------------
DROP TABLE IF EXISTS `lignes_vente`;
CREATE TABLE `lignes_vente`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkVente` bigint NOT NULL,
  `fkStock` bigint NULL DEFAULT NULL,
  `qt` float NULL DEFAULT NULL,
  `prixventes` decimal(10, 2) NULL DEFAULT NULL,
  `horsconvention` tinyint(1) NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 244246 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lits
-- ----------------------------
DROP TABLE IF EXISTS `lits`;
CREATE TABLE `lits`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkChambres` bigint NULL DEFAULT NULL,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for localisation_immo
-- ----------------------------
DROP TABLE IF EXISTS `localisation_immo`;
CREATE TABLE `localisation_immo`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkSite` bigint NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mailingsend
-- ----------------------------
DROP TABLE IF EXISTS `mailingsend`;
CREATE TABLE `mailingsend`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `actif` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkUtilisateur` bigint UNSIGNED NULL DEFAULT NULL,
  `type_notification` enum('email','sms') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `statut` enum('en attente','envoyée','échouée') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'en attente',
  `sujet` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `contenu` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `adresse_destinataire` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `date_programmee` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `date_envoi` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reponse` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_notif_unique`(`fkUtilisateur` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ordonnances
-- ----------------------------
DROP TABLE IF EXISTS `ordonnances`;
CREATE TABLE `ordonnances`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkFiche` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0',
  `fkMedecin` bigint NULL DEFAULT NULL,
  `fkService` bigint NOT NULL,
  `codeprescription` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `statut` enum('EN EDITION','EN ATTENTE','ACHAT EXTERNE','LIVRE','LIVRE PARTIELLEMENT','ANNULEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'EN EDITION',
  `indication` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniqueordonnance`(`codeprescription` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for orientation_devenir
-- ----------------------------
DROP TABLE IF EXISTS `orientation_devenir`;
CREATE TABLE `orientation_devenir`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `objet` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `typehospi` enum('AMBULATOIRE','URGENCE','HOSPITALISATION','DIAGNOSTICS','SOINS A DOMICILE','THERAPIE AMBULATOIRE') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'AMBULATOIRE',
  `dateoriente` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `heureoriente` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `deces` tinyint(1) NULL DEFAULT NULL,
  `text1oriente` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `texte2oriente` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `texteenrichioriente` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `createfiche` tinyint(1) NULL DEFAULT 0,
  `printdoc` tinyint(1) NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17452 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for outbox_events
-- ----------------------------
DROP TABLE IF EXISTS `outbox_events`;
CREATE TABLE `outbox_events`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Type d\'événement (USER_EVENT, AUDIT_EVENT, etc.)',
  `topic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Topic Kafka de destination',
  `event_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Clé de partition Kafka (userId, etc.)',
  `event_payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Payload JSON de l\'événement',
  `status` enum('PENDING','PUBLISHED','FAILED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'Statut de publication',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT 'Nombre de tentatives de publication',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  `published_at` datetime NULL DEFAULT NULL COMMENT 'Date de publication réussie',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Message d\'erreur en cas d\'échec',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status_created`(`status` ASC, `created_at` ASC) USING BTREE COMMENT 'Index pour récupérer les événements PENDING',
  INDEX `idx_event_type`(`event_type` ASC) USING BTREE COMMENT 'Index pour filtrage par type d\'événement'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Table Outbox pour événements Kafka (pattern Outbox)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for parametres_vitaux
-- ----------------------------
DROP TABLE IF EXISTS `parametres_vitaux`;
CREATE TABLE `parametres_vitaux`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `temperature` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `frequencecardiaque` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `frequencerespiratoire` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `tensionarterielle` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `spo2` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `glycemie` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `taille` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `poids` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `imc` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `fkFicheMedicale` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patients
-- ----------------------------
DROP TABLE IF EXISTS `patients`;
CREATE TABLE `patients`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nom` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `postnom` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `prenom` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `codeipp` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `datenaissance` date NULL DEFAULT NULL,
  `genre` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `matricule` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `departement` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28065 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for perimable_alerte_stock
-- ----------------------------
DROP TABLE IF EXISTS `perimable_alerte_stock`;
CREATE TABLE `perimable_alerte_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkStock` bigint NULL DEFAULT NULL,
  `fkAprov` bigint NULL DEFAULT 0,
  `lot` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `dateperemtion` date NULL DEFAULT NULL,
  `notifactif` tinyint(1) NULL DEFAULT NULL,
  `approv` tinyint(1) NULL DEFAULT NULL,
  `stockexpiree` float UNSIGNED NULL DEFAULT NULL,
  `datecreate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `opti_index_perime`(`fkStock` ASC, `fkAprov` ASC) USING BTREE,
  INDEX `idx_perimable_stock_notif_date`(`fkStock` ASC, `notifactif` ASC, `dateperemtion` ASC) USING BTREE,
  INDEX `idx_pas_stock_aprov_date`(`fkStock` ASC, `fkAprov` ASC, `dateperemtion` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19156 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniquepermission`(`nom` ASC) USING BTREE,
  INDEX `index_permission_unique`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 304 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for personnes_charges
-- ----------------------------
DROP TABLE IF EXISTS `personnes_charges`;
CREATE TABLE `personnes_charges`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkAgent` bigint NULL DEFAULT NULL,
  `photo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `nom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `postnom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `prenom` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `genre` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `datenaiss` date NULL DEFAULT NULL,
  `lieunaiss` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT NULL,
  `telephone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `lien` enum('Conjoint','Enfant','Parent','Autre') CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NULL DEFAULT 'Conjoint',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 166 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pharmacies
-- ----------------------------
DROP TABLE IF EXISTS `pharmacies`;
CREATE TABLE `pharmacies`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkSite` bigint UNSIGNED NULL DEFAULT NULL,
  `designation` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `typepharmacie` enum('Cliente','Urgence','Hospitalisation','Centrale','Autre') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'Cliente',
  `codeimmo` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `typehospi` enum('ADMINISTRATION','AMBULATOIRE','URGENCE','HOSPITALISATION','DIAGNOSTICS','SOINS A DOMICILE','THERAPIE AMBULATOIRE') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'ADMINISTRATION',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreatedid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniqueserviimmo`(`codeimmo` ASC) USING BTREE,
  INDEX `index_pharmacie_unique`(`id` ASC, `fkSite` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 74 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for phaseconsultation
-- ----------------------------
DROP TABLE IF EXISTS `phaseconsultation`;
CREATE TABLE `phaseconsultation`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `syntheseobservation` tinyint(1) NULL DEFAULT NULL,
  `presetnouveaunee` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `presetnourrissant` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `presetenfant` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `presetadultes` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `typehospi` enum('OUT AMBULATOIRE','AMBULATOIRE','OUT URGENCE','URGENCE','HOSPITALISATION','DIAGNOSTICS','SOINS A DOMICILE','THERAPIE AMBULATOIRE') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'AMBULATOIRE',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pieces_jointes_notif
-- ----------------------------
DROP TABLE IF EXISTS `pieces_jointes_notif`;
CREATE TABLE `pieces_jointes_notif`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkNotification` bigint UNSIGNED NOT NULL,
  `nom_fichier` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `chemin_fichier` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `taille_fichier` bigint NULL DEFAULT NULL,
  `type_mime` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_piecesjointe_unique`(`id` ASC, `fkNotification` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for point_appel
-- ----------------------------
DROP TABLE IF EXISTS `point_appel`;
CREATE TABLE `point_appel`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `message` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `designation` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `fkService` bigint NOT NULL,
  `cabinet` tinyint(1) NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for postes
-- ----------------------------
DROP TABLE IF EXISTS `postes`;
CREATE TABLE `postes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for produits
-- ----------------------------
DROP TABLE IF EXISTS `produits`;
CREATE TABLE `produits`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `codebarre` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `nomcommercial` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `nomscientifique` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkForme` bigint NULL DEFAULT NULL,
  `fkDosage` bigint NULL DEFAULT NULL,
  `fkConditionnement` bigint NULL DEFAULT NULL,
  `fkCategorie` bigint NULL DEFAULT NULL,
  `prixachat` decimal(10, 2) NOT NULL,
  `prixachatcomptable` decimal(10, 4) NULL DEFAULT NULL,
  `qtealert` float NOT NULL DEFAULT 0,
  `qtcritique` float NOT NULL DEFAULT 0,
  `perimable` tinyint(1) NOT NULL,
  `rayon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreatedid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniquecodebarre`(`codebarre` ASC) USING BTREE,
  INDEX `indexproduitid`(`fkForme` ASC, `fkDosage` ASC, `fkConditionnement` ASC, `fkCategorie` ASC) USING BTREE,
  INDEX `idx_produits_fk`(`fkForme` ASC, `fkDosage` ASC, `fkConditionnement` ASC, `fkCategorie` ASC) USING BTREE,
  INDEX `idx_nomcommercial`(`nomcommercial` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7851 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for produits_convention
-- ----------------------------
DROP TABLE IF EXISTS `produits_convention`;
CREATE TABLE `produits_convention`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkProduit` bigint NULL DEFAULT NULL,
  `fkConvention` bigint NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 539 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for reception_stock
-- ----------------------------
DROP TABLE IF EXISTS `reception_stock`;
CREATE TABLE `reception_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkTransfert` bigint NOT NULL,
  `statut` enum('EN ATTENTE','ANNULEE','RECEPTIONNEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15774 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for reception_transfert_interne
-- ----------------------------
DROP TABLE IF EXISTS `reception_transfert_interne`;
CREATE TABLE `reception_transfert_interne`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkTransfertInterne` bigint NOT NULL,
  `statut` enum('EN ATTENTE','ANNULEE','RECEPTIONNEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `perime` tinyint(1) NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for requisitions
-- ----------------------------
DROP TABLE IF EXISTS `requisitions`;
CREATE TABLE `requisitions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkPharmacie` bigint NOT NULL,
  `fkPharmacieStock` bigint NOT NULL,
  `statut` enum('EN ATTENTE','VALIDEE','REJETEE','TRANSFEREE','RECEPTIONNEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `niveau` smallint NULL DEFAULT NULL,
  `commentaire` mediumtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `urgent` tinyint(1) NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_req_optimis`(`id` ASC, `fkPharmacieStock` ASC, `statut` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21099 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for resumestaffs
-- ----------------------------
DROP TABLE IF EXISTS `resumestaffs`;
CREATE TABLE `resumestaffs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkFicheMedicale` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `codeIPP` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `commentaire` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `service` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `oriente` tinyint(1) NULL DEFAULT NULL,
  `statut` enum('URGENCE VITAL','CRITIQUE','NORMAL') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'NORMAL',
  `noms` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `genre` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `ageannee` int NULL DEFAULT NULL,
  `agemois` int NULL DEFAULT NULL,
  `agejour` int NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `nom` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `description` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UNIQUEROLE`(`nom` ASC) USING BTREE,
  INDEX `ndex_role_unique`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 91 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for roles_permissions
-- ----------------------------
DROP TABLE IF EXISTS `roles_permissions`;
CREATE TABLE `roles_permissions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkRole` bigint UNSIGNED NOT NULL,
  `fkPermission` bigint UNSIGNED NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_roleperimis_unique`(`id` ASC, `fkRole` ASC, `fkPermission` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 825 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sites
-- ----------------------------
DROP TABLE IF EXISTS `sites`;
CREATE TABLE `sites`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `abbreviation` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `addresse` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `bloquer` tinyint(1) NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_unique_code`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for soins_infirmiers
-- ----------------------------
DROP TABLE IF EXISTS `soins_infirmiers`;
CREATE TABLE `soins_infirmiers`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkFicheMedicale` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '',
  `codeIPP` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `commentaire` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `service` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sous_categorie_immo
-- ----------------------------
DROP TABLE IF EXISTS `sous_categorie_immo`;
CREATE TABLE `sous_categorie_immo`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `fkCategorie` bigint NULL DEFAULT NULL,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idunsouscatimmoindex`(`code` ASC, `fkCategorie` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stock_alert_history
-- ----------------------------
DROP TABLE IF EXISTS `stock_alert_history`;
CREATE TABLE `stock_alert_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkStock` bigint NOT NULL,
  `fkProduits` bigint NOT NULL,
  `fkPharmacies` bigint NOT NULL,
  `niveau_alerte` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `canal` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `destinataire` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `titre` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `statut_envoi` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'EN_ATTENTE',
  `erreur` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `datecreate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_history_stock`(`fkStock` ASC) USING BTREE,
  INDEX `idx_history_produit`(`fkProduits` ASC) USING BTREE,
  INDEX `idx_history_pharmacie`(`fkPharmacies` ASC) USING BTREE,
  INDEX `idx_history_niveau`(`niveau_alerte` ASC) USING BTREE,
  INDEX `idx_history_date`(`datecreate` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stock_alert_metrics
-- ----------------------------
DROP TABLE IF EXISTS `stock_alert_metrics`;
CREATE TABLE `stock_alert_metrics`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkStock` bigint NOT NULL,
  `fkProduits` bigint NOT NULL,
  `fkPharmacies` bigint NOT NULL,
  `stock_actuel` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `consommation_30j` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `consommation_90j` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `consommation_365j` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `consommation_moyenne_jour` decimal(12, 4) NOT NULL DEFAULT 0.0000,
  `qte_min` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `qte_max` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `stock_securite` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `jours_couverture` decimal(12, 2) NULL DEFAULT NULL,
  `delai_reappro_jour` int NOT NULL DEFAULT 7,
  `date_derniere_sortie` datetime NULL DEFAULT NULL,
  `date_derniere_entree` datetime NULL DEFAULT NULL,
  `date_derniere_alerte` datetime NULL DEFAULT NULL,
  `niveau_alerte` enum('NORMAL','SURVEILLANCE','CRITIQUE','RUPTURE','DORMANT','SURSTOCK') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL',
  `message_alerte` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `date_calcul` date NULL DEFAULT NULL,
  `datecreate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stock_alert_metrics_stock_date`(`fkStock` ASC, `date_calcul` ASC) USING BTREE,
  INDEX `idx_metrics_stock`(`fkStock` ASC) USING BTREE,
  INDEX `idx_metrics_produit`(`fkProduits` ASC) USING BTREE,
  INDEX `idx_metrics_pharmacie`(`fkPharmacies` ASC) USING BTREE,
  INDEX `idx_metrics_niveau_alerte`(`niveau_alerte` ASC) USING BTREE,
  INDEX `idx_metrics_date_calcul`(`date_calcul` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 132841 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stock_alert_settings
-- ----------------------------
DROP TABLE IF EXISTS `stock_alert_settings`;
CREATE TABLE `stock_alert_settings`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkStock` bigint NOT NULL,
  `delai_reappro_jour` int NOT NULL DEFAULT 7,
  `jours_securite` int NOT NULL DEFAULT 7,
  `jours_stock_max` int NOT NULL DEFAULT 30,
  `seuil_dormant_jour` int NOT NULL DEFAULT 60,
  `alerte_active` tinyint(1) NOT NULL DEFAULT 1,
  `datecreate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stock_alert_settings_fkStock`(`fkStock` ASC) USING BTREE,
  INDEX `idx_stock_alert_settings_active`(`alerte_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13425 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stock_intelligence_email_log
-- ----------------------------
DROP TABLE IF EXISTS `stock_intelligence_email_log`;
CREATE TABLE `stock_intelligence_email_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `report_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `recipient` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `status` enum('SENT','FAILED','SKIPPED') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `snapshot_id` bigint UNSIGNED NULL DEFAULT NULL,
  `error_detail` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `sent_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_si_email_log_sent`(`sent_at` ASC) USING BTREE,
  INDEX `idx_si_email_log_report`(`report_type` ASC, `sent_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stock_intelligence_recipients
-- ----------------------------
DROP TABLE IF EXISTS `stock_intelligence_recipients`;
CREATE TABLE `stock_intelligence_recipients`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `label` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `whatsapp_number` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `receive_morning_report` tinyint(1) NOT NULL DEFAULT 1,
  `receive_whatsapp_chat` tinyint(1) NOT NULL DEFAULT 0,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stock_intelligence_snapshots
-- ----------------------------
DROP TABLE IF EXISTS `stock_intelligence_snapshots`;
CREATE TABLE `stock_intelligence_snapshots`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `report_type` enum('MORNING','EVENING','ON_DEMAND','WHATSAPP') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'ON_DEMAND',
  `pharmacie_id` bigint UNSIGNED NULL DEFAULT NULL,
  `snapshot_json` mediumtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `ai_analysis_json` mediumtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `generated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_si_snapshots_generated`(`generated_at` ASC) USING BTREE,
  INDEX `idx_si_snapshots_type`(`report_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for stock_produits
-- ----------------------------
DROP TABLE IF EXISTS `stock_produits`;
CREATE TABLE `stock_produits`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkProduits` bigint NULL DEFAULT NULL,
  `fkPharmacies` bigint NULL DEFAULT NULL,
  `qte` float UNSIGNED NULL DEFAULT NULL,
  `operationnel` tinyint(1) NOT NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `index-stock-unique-pharma`(`fkProduits` ASC, `fkPharmacies` ASC) USING BTREE,
  INDEX `index-stockoptimise`(`id` ASC, `operationnel` ASC, `fkPharmacies` ASC) USING BTREE,
  INDEX `idx_sp_pharmacie_produit`(`fkPharmacies` ASC, `fkProduits` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 385944 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for suivi_administratif
-- ----------------------------
DROP TABLE IF EXISTS `suivi_administratif`;
CREATE TABLE `suivi_administratif`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkAgent` bigint NULL DEFAULT NULL,
  `fkTypeSuivi` bigint NULL DEFAULT NULL,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `decision` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for suivi_administratif_document
-- ----------------------------
DROP TABLE IF EXISTS `suivi_administratif_document`;
CREATE TABLE `suivi_administratif_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkSuivi` bigint NULL DEFAULT NULL,
  `fkDocument` bigint NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for surveillance
-- ----------------------------
DROP TABLE IF EXISTS `surveillance`;
CREATE TABLE `surveillance`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkLit` bigint NULL DEFAULT NULL,
  `service` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `etatpatient` enum('URGENCE','CRITIQUE','STABILISE','NORMAL','OBSERVATION') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'NORMAL',
  `statut` enum('EN_COURS','SORTIE_INTERNE','SORTIE_CLINIQUE','DECEDE','ANNULEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN_COURS',
  `codeipp` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_famille
-- ----------------------------
DROP TABLE IF EXISTS `t_famille`;
CREATE TABLE `t_famille`  (
  `CODE` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `INTITULE` varchar(75) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `ETAT` tinyint(1) NULL DEFAULT 1,
  `ORDRE` int NULL DEFAULT 0,
  `TYPE_PRODUIT` varchar(25) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`CODE`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_produit
-- ----------------------------
DROP TABLE IF EXISTS `t_produit`;
CREATE TABLE `t_produit`  (
  `CODE` varchar(20) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `INTITULE` varchar(75) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `FK_UNITE` varchar(5) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `FK_FAMILLE` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `TYPE_PRODUIT` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT 'EXAMEN',
  `COUT` decimal(20, 2) NULL DEFAULT 0.00,
  `DATE_CREAT` datetime NULL DEFAULT NULL,
  `AGENT_CREAT` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `ETAT` tinyint(1) NULL DEFAULT 1,
  `VALEUR_NOMINALE` longtext CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL,
  `PRODUIT_MERE` varchar(20) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `GROUPE_PRODUIT` tinyint(1) NULL DEFAULT 0,
  `AJOUT_EXAMEN` tinyint(1) NULL DEFAULT 1,
  `ORDRE` int NULL DEFAULT 0,
  `PROTOCOLABLE` tinyint(1) NULL DEFAULT 0,
  `ONLY_RIGHT` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`CODE`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tickets
-- ----------------------------
DROP TABLE IF EXISTS `tickets`;
CREATE TABLE `tickets`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `numero` int NOT NULL DEFAULT 0,
  `numeroDossier` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `nomPatient` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `genre` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `date_naissance` date NULL DEFAULT NULL,
  `mail` varchar(75) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  `statut` enum('EN ATTENTE','TICKET-AFFECTE','TRAITE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `type` enum('CONSULTATION','RENDEZ-VOUS','RESULTAT','RENSEIGNEMENT') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'CONSULTATION',
  `etatappel` enum('LIBRE','ENCOURS DE TRAITEMENT','TRAITE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'LIBRE',
  `fkPointAppel` bigint NULL DEFAULT NULL,
  `fkService` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21560 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for transfert_interne
-- ----------------------------
DROP TABLE IF EXISTS `transfert_interne`;
CREATE TABLE `transfert_interne`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkPharmacieSource` bigint NOT NULL,
  `fkPharmacieDestination` bigint NOT NULL,
  `statut` enum('EN ATTENTE','VALIDEE','REJETEE','TRANSFEREE','RECEPTIONNEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `commentaire` mediumtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `perime` tinyint(1) NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_req_optimis`(`id` ASC, `fkPharmacieDestination` ASC, `statut` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for transferts_stock
-- ----------------------------
DROP TABLE IF EXISTS `transferts_stock`;
CREATE TABLE `transferts_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkRequisition` bigint NOT NULL,
  `statut` enum('EN ATTENTE','ANNULEE','TRANSFEREE','RECEPTIONNEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_unique_transf`(`fkRequisition` ASC) USING BTREE,
  INDEX `index_transfer_optimis`(`id` ASC, `fkRequisition` ASC) USING BTREE,
  INDEX `idx_transfert_requisition`(`id` ASC, `datecreate` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18189 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for type_conge
-- ----------------------------
DROP TABLE IF EXISTS `type_conge`;
CREATE TABLE `type_conge`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for type_document
-- ----------------------------
DROP TABLE IF EXISTS `type_document`;
CREATE TABLE `type_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for type_mouvement
-- ----------------------------
DROP TABLE IF EXISTS `type_mouvement`;
CREATE TABLE `type_mouvement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for type_suivi
-- ----------------------------
DROP TABLE IF EXISTS `type_suivi`;
CREATE TABLE `type_suivi`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for utilisateurs
-- ----------------------------
DROP TABLE IF EXISTS `utilisateurs`;
CREATE TABLE `utilisateurs`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `specialite` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `nom` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `postnom` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `prenom` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `username` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `genre` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `mot_de_passe` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `locked` tinyint(1) NOT NULL,
  `fkRole` bigint UNSIGNED NULL DEFAULT NULL,
  `initPassword` tinyint(1) NULL DEFAULT 0,
  `carteid` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'aucun',
  `islogincard` tinyint(1) NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `userunique`(`username` ASC) USING BTREE,
  INDEX `fkroleindexuser`(`fkRole` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for utilisateurs_permissions
-- ----------------------------
DROP TABLE IF EXISTS `utilisateurs_permissions`;
CREATE TABLE `utilisateurs_permissions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkUtilisateur` bigint UNSIGNED NOT NULL,
  `fkPermission` bigint UNSIGNED NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_user_permis`(`id` ASC, `fkUtilisateur` ASC, `fkPermission` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 125135 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ventes
-- ----------------------------
DROP TABLE IF EXISTS `ventes`;
CREATE TABLE `ventes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fkEntreprise` bigint UNSIGNED NULL DEFAULT 0,
  `fkPatient` bigint NULL DEFAULT NULL,
  `fkPharmacie` bigint NOT NULL,
  `statut` enum('EN ATTENTE','PAYEE','ANNULEE','FACTUREE','SORTIE-USAGE','ANNULEE-REMBOURSE','ORDONNANCE EN ATTENTE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'EN ATTENTE',
  `taux` smallint NULL DEFAULT NULL,
  `typepaiement` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `raisonsortie` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `demandeur` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkPatientMediline` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkFicheMedicale` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 77547 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for whatsapp_chat_log
-- ----------------------------
DROP TABLE IF EXISTS `whatsapp_chat_log`;
CREATE TABLE `whatsapp_chat_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `wa_message_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `from_number` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `direction` enum('IN','OUT') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `message_text` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `ai_response` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `snapshot_id` bigint UNSIGNED NULL DEFAULT NULL,
  `status` enum('RECEIVED','PROCESSED','SENT','FAILED') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'RECEIVED',
  `error_detail` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wa_log_from`(`from_number` ASC) USING BTREE,
  INDEX `idx_wa_log_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for whatsapp_send
-- ----------------------------
DROP TABLE IF EXISTS `whatsapp_send`;
CREATE TABLE `whatsapp_send`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `label` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_whatsapp_send_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Procedure structure for AddStockAfterApprov
-- ----------------------------
DROP PROCEDURE IF EXISTS `AddStockAfterApprov`;
delimiter ;;
CREATE PROCEDURE `AddStockAfterApprov`(IN approvId BIGINT)
BEGIN
    -- Déclaration des variables
    DECLARE done INT DEFAULT 0;
    DECLARE stockId BIGINT;
    DECLARE produitId BIGINT;
    DECLARE quantiteAjoutee FLOAT;
    DECLARE stockActuel FLOAT;
    DECLARE prixAchatActuel DECIMAL(10, 2);
    DECLARE prixAchatApprov DECIMAL(10, 2);
    
    -- Curseur pour récupérer les lignes d'approvisionnement
    DECLARE cur CURSOR FOR 
        SELECT la.fkStock, la.qt, p.id, p.prixachat, la.prixachat
        FROM lignes_approv la
        JOIN stock_produits sp ON la.fkStock = sp.id
        JOIN produits p ON sp.fkProduits = p.id
        WHERE la.fkApprov = approvId;

    -- Gestion des exceptions
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Ouverture du curseur
    OPEN cur;
    
    read_loop: LOOP
        -- Lire la ligne d'approvisionnement
        FETCH cur INTO stockId, quantiteAjoutee, produitId, prixAchatActuel, prixAchatApprov;

        -- Si aucune ligne, sortir de la boucle
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        
        -- Récupérer le stock actuel du produit
        SELECT sp.qte INTO stockActuel
        FROM stock_produits sp
        WHERE sp.id = stockId;
        
        -- Mettre à jour le stock en ajoutant la quantité approvisionnée
        UPDATE stock_produits
        SET qte = stockActuel + quantiteAjoutee, operationnel = true
        WHERE id = stockId;
				
				UPDATE perimable_alerte_stock
        SET notifactif = true, approv = true
        WHERE fkAprov = approvId;
      
				-- Comparer les prix d'achat et ajuster dans la table `produits` si nécessaire
				IF prixAchatApprov > prixAchatActuel AND stockActuel > 0 THEN
						-- Si le prix d'achat de l'approvisionnement est supérieur, on met à jour
						UPDATE produits
						SET prixachat = prixAchatApprov
						WHERE id = produitId;
				ELSE
						-- Sinon, effectuer une autre action (par exemple : journalisation ou mise à jour d'une autre colonne)
						 UPDATE produits
						SET prixachat = prixAchatApprov
						WHERE id = produitId;
				END IF;

				
        
    END LOOP;
    
    -- Fermer le curseur
    CLOSE cur;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for AjouterLignesReception
-- ----------------------------
DROP PROCEDURE IF EXISTS `AjouterLignesReception`;
delimiter ;;
CREATE PROCEDURE `AjouterLignesReception`(IN idReception INT)
BEGIN
    INSERT INTO lignes_reception_stock (fkReceptionStock, fkStock, quantite,quantiteDemandee,quantiteTransferee, usercreateid, userupdateid)
    SELECT 
        rc.id AS fkReceptionStock, 
        sp.id AS fkStock,
        lr.quantite, 
				lr.quantiteDemandee, 
				lr.quantite,
        rc.usercreateid,
        rc.userupdateid
    FROM lignes_transferts_stock lr
		INNER JOIN transferts_stock ts ON ts.id = lr.fkTransfertStock
    INNER JOIN requisitions rs ON rs.id = ts.fkRequisition
    INNER JOIN stock_produits sp ON sp.id = (
        SELECT id 
        FROM stock_produits 
        WHERE fkProduits = (SELECT fkProduits FROM stock_produits WHERE id = lr.fkStock) 
        AND fkPharmacies = rs.fkPharmacie
    )
    INNER JOIN reception_stock rc ON rc.fkTransfert = ts.id
    WHERE rc.id = idReception;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for AjouterLignesReceptionTransfertInterne
-- ----------------------------
DROP PROCEDURE IF EXISTS `AjouterLignesReceptionTransfertInterne`;
delimiter ;;
CREATE PROCEDURE `AjouterLignesReceptionTransfertInterne`(IN idReception INT)
BEGIN
    INSERT INTO lignes_reception_transfert_interne (fkReceptionStock, fkStock, quantite,quantiteDemandee,quantiteTransferee, usercreateid, userupdateid)
    SELECT 
        rc.id AS fkReceptionStock, 
        sp.id AS fkStock,
        lr.quantite, 
				lr.quantite, 
				lr.quantite,
        rc.usercreateid,
        rc.userupdateid
    FROM lignes_transfert_interne lr
		INNER JOIN transfert_interne ts ON ts.id = lr.fkTransfertInterne
    INNER JOIN stock_produits sp ON sp.id = (
        SELECT id 
        FROM stock_produits 
        WHERE fkProduits = (SELECT fkProduits FROM stock_produits WHERE id = lr.fkStock) 
        AND fkPharmacies = ts.fkPharmacieSource
    )
    INNER JOIN reception_transfert_interne rc ON rc.fkTransfertInterne = ts.id
    WHERE rc.id = idReception;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for AjouterLignesTransfert
-- ----------------------------
DROP PROCEDURE IF EXISTS `AjouterLignesTransfert`;
delimiter ;;
CREATE PROCEDURE `AjouterLignesTransfert`(IN idtransfert INT)
BEGIN
    INSERT INTO lignes_transferts_stock (fkTransfertStock, fkStock, quantite,quantiteDemandee, usercreateid, userupdateid)
    SELECT 
        ts.id AS fkTransfertStock, 
        sp.id AS fkStock,
        lr.quantite, 
				lr.quantite,
        ts.usercreateid,
        ts.userupdateid
    FROM lignes_requisitions lr
    INNER JOIN requisitions rs ON rs.id = lr.fkRequisition
    INNER JOIN stock_produits sp ON sp.id = (
        SELECT id 
        FROM stock_produits 
        WHERE fkProduits = (SELECT fkProduits FROM stock_produits WHERE id = lr.fkStock) 
        AND fkPharmacies = rs.fkPharmacieStock
    )
    INNER JOIN transferts_stock ts ON ts.fkRequisition = rs.id
    WHERE ts.id = idtransfert;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for ajouter_stock_pour_nouvelle_pharmacie
-- ----------------------------
DROP PROCEDURE IF EXISTS `ajouter_stock_pour_nouvelle_pharmacie`;
delimiter ;;
CREATE PROCEDURE `ajouter_stock_pour_nouvelle_pharmacie`(IN pharmacie_id BIGINT)
BEGIN
    -- Déclarer une variable pour stocker l'ID du produit
    DECLARE produit_id BIGINT;
    
    -- Curseur pour parcourir tous les produits
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR 
        SELECT id FROM produits;
    
    -- Déclaration de la condition de fin
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- Ouvrir le curseur
    OPEN cur;
    
    -- Lire chaque produit et insérer une ligne de stock avec qte = 0 pour la pharmacie
    read_loop: LOOP
        FETCH cur INTO produit_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Insérer une ligne de stock avec quantité 0 pour le produit et la pharmacie donnés
        INSERT INTO stock_produits (fkProduits, fkPharmacies, qte, datecreate, usercreateid) 
        VALUES (produit_id, pharmacie_id, 0, NOW(), 1);  -- '1' est supposé être l'ID de l'utilisateur admin
    END LOOP;
    
    -- Fermer le curseur
    CLOSE cur;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for ajouter_stock_pour_pharmacies
-- ----------------------------
DROP PROCEDURE IF EXISTS `ajouter_stock_pour_pharmacies`;
delimiter ;;
CREATE PROCEDURE `ajouter_stock_pour_pharmacies`(IN produit_id BIGINT)
BEGIN
    DECLARE stock_id BIGINT;
    DECLARE inventaire_id BIGINT;
    DECLARE typeInventaire VARCHAR(50);
    DECLARE pharmacie_id BIGINT;
    
    -- Curseur pour parcourir toutes les pharmacies
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR 
        SELECT id FROM pharmacies;
    
    -- Déclaration de la condition de fin
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- Ouvrir le curseur pour toutes les pharmacies
    OPEN cur;
    
    -- Lire chaque pharmacie et insérer une ligne de stock
    read_loop: LOOP
        FETCH cur INTO pharmacie_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Insérer une ligne de stock avec quantité 0 pour chaque pharmacie et le produit donné
        INSERT INTO stock_produits (fkProduits, fkPharmacies, qte, datecreate, usercreateid) 
        VALUES (produit_id, pharmacie_id, 0, NOW(), 1);
        
    END LOOP;
    
    -- Fermer le curseur
    CLOSE cur;
    
    -- Maintenant, récupérer l'inventaire en cours pour une pharmacie spécifique
    SELECT i.id, i.typeinventaire, i.fkPharmacie 
    INTO inventaire_id, typeInventaire, pharmacie_id
    FROM inventaires i
    WHERE i.statut = 'EN COURS' AND i.fkPharmacie = pharmacie_id
    LIMIT 1;

    -- Vérifier si un inventaire en cours a été trouvé
    IF inventaire_id IS NOT NULL THEN
        -- Récupérer l'ID du stock pour cette pharmacie (celle de l'inventaire en cours)
        SELECT s.id INTO stock_id
        FROM stock_produits s
        WHERE s.fkProduits = produit_id
        AND s.fkPharmacies = pharmacie_id
        LIMIT 1;

        -- Si l'inventaire est de type 'PHYSIQUE', insérer une ligne dans 'lignes_inventaire'
        IF typeInventaire = 'PHYSIQUE' THEN
            INSERT INTO lignes_inventaire (
                fkInventaire, 
                fkStock, 
                quantite_theorique, 
                quantite_physique, 
                commentaire, 
                datecreate, 
                dateupdate, 
                usercreateid, 
                userupdateid
            )
            VALUES (
                inventaire_id, 
                stock_id,                -- Utiliser l'ID du stock pour cette pharmacie
                0,                       -- Quantité théorique initialisée à 0
                0,                       -- Quantité physique à 0 pour le type 'PHYSIQUE'
                NULL,                    -- Pas de commentaire initial
                NOW(), 
                NOW(), 
                1,                       -- Utilisateur admin ID 1
                0
            );
        END IF;
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for ajuster_stock_apres_inventaire
-- ----------------------------
DROP PROCEDURE IF EXISTS `ajuster_stock_apres_inventaire`;
delimiter ;;
CREATE PROCEDURE `ajuster_stock_apres_inventaire`(IN inventaireId BIGINT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE stockId BIGINT;
    DECLARE quantitePhysique FLOAT;
    DECLARE quantiteTheorique FLOAT;
    DECLARE ecart FLOAT;
    DECLARE idUser BIGINT;
		DECLARE typeinvent VARCHAR(255);

    -- Curseur pour sélectionner les lignes de l'inventaire en fonction de la nouvelle relation avec stock_produits
    DECLARE cur CURSOR FOR
        SELECT li.fkStock AS stockId, li.quantite_physique, li.quantite_theorique, li.ecart, li.userupdateid, i.typeinventaire as typeinvent
        FROM lignes_inventaire li
				INNER JOIN inventaires i ON i.id = li.fkInventaire
        WHERE li.fkInventaire = inventaireId;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- Ouvrir le curseur
    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO stockId, quantitePhysique, quantiteTheorique, ecart, idUser, typeinvent;
        IF done THEN
            LEAVE read_loop;
        END IF;

         IF typeinvent = 'PHYSIQUE' THEN
				 
				  UPDATE stock_produits
                SET qte = quantitePhysique, 
                    operationnel = CASE WHEN idUser > 0 THEN TRUE ELSE FALSE END,
                    dateupdate = NOW(),
                    userupdateid = idUser
                WHERE id = stockId;
								
				 END IF;
				
				 IF typeinvent = 'AJUSTEMENT' THEN
				 
				 IF idUser > 0 THEN
				 UPDATE stock_produits
                SET qte = quantitePhysique, 
                    operationnel = CASE WHEN idUser > 0 THEN TRUE ELSE FALSE END,
                    dateupdate = NOW(),
                    userupdateid = idUser
                WHERE id = stockId;				 
				 END IF;				  
								
				 END IF;
				
    END LOOP;

    -- Fermer le curseur
    CLOSE cur;

END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for assign_permissions_to_user
-- ----------------------------
DROP PROCEDURE IF EXISTS `assign_permissions_to_user`;
delimiter ;;
CREATE PROCEDURE `assign_permissions_to_user`(IN user_id BIGINT, IN role_id BIGINT)
BEGIN
    -- Récupérer toutes les permissions associées au rôle
    DECLARE done INT DEFAULT 0;
    DECLARE permission_id BIGINT;

    -- Curseur pour parcourir les permissions du rôle
    DECLARE cur_permissions CURSOR FOR 
    SELECT fkPermission FROM roles_permissions WHERE fkRole = role_id;

    -- Gestion des exceptions (fin du curseur)
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Ouvrir le curseur
    OPEN cur_permissions;

    -- Boucle à travers toutes les permissions du rôle
    permissions_loop: LOOP
        FETCH cur_permissions INTO permission_id;
        IF done = 1 THEN
            LEAVE permissions_loop;
        END IF;

        -- Insertion dans la table utilisateurs_permissions
        INSERT INTO utilisateurs_permissions (fkUtilisateur, fkPermission, datecreate, usercreateid)
        VALUES (user_id, permission_id, NOW(), user_id);

    END LOOP;

    -- Fermer le curseur
    CLOSE cur_permissions;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for InsertLignesInventaire
-- ----------------------------
DROP PROCEDURE IF EXISTS `InsertLignesInventaire`;
delimiter ;;
CREATE PROCEDURE `InsertLignesInventaire`(IN inventaire_id BIGINT, 
    IN pharmacie_id BIGINT, 
    IN user_create_id BIGINT)
BEGIN
    DECLARE typeInventaire VARCHAR(50);

    -- Récupérer le type d'inventaire (PHYSIQUE ou AJUSTEMENT) pour l'inventaire donné
    SELECT i.typeinventaire
    INTO typeInventaire
    FROM inventaires i
    WHERE i.id = inventaire_id;

    -- Insérer les lignes d'inventaire avec une logique basée sur le type d'inventaire
    IF typeInventaire = 'PHYSIQUE' THEN
        -- Si typeInventaire est PHYSIQUE, quantitePhysique = 0
        INSERT INTO lignes_inventaire (
            fkInventaire, 
            fkStock, 
            quantite_theorique, 
            quantite_physique, 
            datecreate, 
            dateupdate, 
            usercreateid, 
            userupdateid
        )
        SELECT
            inventaire_id AS fkInventaire,
            s.id AS fkStock,
            s.qte AS quantiteTheorique,
            0 AS quantitePhysique,            -- Initialiser la quantité physique à zéro
            NOW() AS datecreate,
            NOW() AS dateupdate,
            user_create_id AS usercreateid,
            0 AS userupdateid
        FROM stock_produits s
        INNER JOIN produits p ON s.fkProduits = p.id
				INNER JOIN categorie_produit  ct ON ct.id = p.fkCategorie
				INNER JOIN droits_categorie dr ON dr.fkCategorie = ct.id AND dr.fkPharmacie = s.fkPharmacies
        WHERE s.fkPharmacies = pharmacie_id;
        
    ELSEIF typeInventaire = 'AJUSTEMENT'  THEN
        -- Si typeInventaire est AJUSTEMENT, quantitePhysique = quantiteTheorique
        INSERT INTO lignes_inventaire (
            fkInventaire, 
            fkStock, 
            quantite_theorique, 
            quantite_physique, 
            datecreate, 
            dateupdate, 
            usercreateid, 
            userupdateid
        )
        SELECT
            inventaire_id AS fkInventaire,
            s.id AS fkStock,
            s.qte AS quantiteTheorique,
            s.qte AS quantitePhysique,        -- Initialiser la quantité physique égale à la quantité théorique
            NOW() AS datecreate,
            NOW() AS dateupdate,
            user_create_id AS usercreateid,
            0 AS userupdateid
        FROM stock_produits s
        INNER JOIN produits p ON s.fkProduits = p.id
				INNER JOIN categorie_produit  ct ON ct.id = p.fkCategorie
				INNER JOIN droits_categorie dr ON dr.fkCategorie = ct.id AND dr.fkPharmacie = s.fkPharmacies
        WHERE s.fkPharmacies = pharmacie_id;
     
			ELSEIF typeInventaire =  'MENSUEL' THEN
        -- Si typeInventaire est AJUSTEMENT, quantitePhysique = quantiteTheorique
        INSERT INTO lignes_inventaire (
            fkInventaire, 
            fkStock, 
            quantite_theorique, 
            quantite_physique, 
            datecreate, 
            dateupdate, 
            usercreateid, 
            userupdateid
        )
        SELECT
            inventaire_id AS fkInventaire,
            s.id AS fkStock,
            s.qte AS quantiteTheorique,
            s.qte AS quantitePhysique,        -- Initialiser la quantité physique égale à la quantité théorique
            NOW() AS datecreate,
            NOW() AS dateupdate,
            user_create_id AS usercreateid,
            0 AS userupdateid
        FROM stock_produits s
        INNER JOIN produits p ON s.fkProduits = p.id
				INNER JOIN categorie_produit  ct ON ct.id = p.fkCategorie
				INNER JOIN droits_categorie dr ON dr.fkCategorie = ct.id AND dr.fkPharmacie = s.fkPharmacies
        WHERE s.fkPharmacies = pharmacie_id;   
				
    ELSE
        -- Gérer les cas où le type d'inventaire n'est pas reconnu (optionnel)
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Type d''inventaire inconnu';
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for ReduceStockAfterSale
-- ----------------------------
DROP PROCEDURE IF EXISTS `ReduceStockAfterSale`;
delimiter ;;
CREATE PROCEDURE `ReduceStockAfterSale`(IN venteId BIGINT)
BEGIN
    -- Déclaration des variables
    DECLARE done INT DEFAULT 0;
    DECLARE produitId BIGINT;
    DECLARE quantiteVendu FLOAT;
    DECLARE stockId BIGINT;
    DECLARE stockActuel FLOAT;
    
    -- Curseur pour récupérer les lignes de vente
    DECLARE cur CURSOR FOR 
        SELECT lv.fkStock, lv.qt
        FROM lignes_vente lv
        WHERE lv.fkVente = venteId;

    -- Gestion des exceptions
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Ouverture du curseur
    OPEN cur;
    
    read_loop: LOOP
        -- Lire la ligne de vente
        FETCH cur INTO stockId, quantiteVendu;

        -- Si aucune ligne, sortir de la boucle
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        
        -- Récupérer le stock actuel du produit
        SELECT sp.qte INTO stockActuel
        FROM stock_produits sp
        WHERE sp.id = stockId;
        
        -- Mettre à jour le stock
        UPDATE stock_produits
        SET qte = stockActuel - quantiteVendu
        WHERE id = stockId;
    END LOOP;
    
    -- Fermer le curseur
    CLOSE cur;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for RestoreStockAfterAnnule
-- ----------------------------
DROP PROCEDURE IF EXISTS `RestoreStockAfterAnnule`;
delimiter ;;
CREATE PROCEDURE `RestoreStockAfterAnnule`(IN venteId BIGINT)
BEGIN
    -- Déclaration des variables
    DECLARE done INT DEFAULT 0;
    DECLARE produitId BIGINT;
    DECLARE quantiteVendu FLOAT;
    DECLARE stockId BIGINT;
    DECLARE stockActuel FLOAT;
    
    -- Curseur pour récupérer les lignes de vente
    DECLARE cur CURSOR FOR 
        SELECT lv.fkStock, lv.qt
        FROM lignes_vente lv
        WHERE lv.fkVente = venteId;

    -- Gestion des exceptions
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Ouverture du curseur
    OPEN cur;
    
    read_loop: LOOP
        -- Lire la ligne de vente
        FETCH cur INTO stockId, quantiteVendu;

        -- Si aucune ligne, sortir de la boucle
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        
        -- Récupérer le stock actuel du produit
        SELECT sp.qte INTO stockActuel
        FROM stock_produits sp
        WHERE sp.id = stockId;
        
        -- Mettre à jour le stock
        UPDATE stock_produits
        SET qte = stockActuel + quantiteVendu
        WHERE id = stockId;
    END LOOP;
    
    -- Fermer le curseur
    CLOSE cur;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for UpdateRequisitionStatusAndStockOnTransfertStock
-- ----------------------------
DROP PROCEDURE IF EXISTS `UpdateRequisitionStatusAndStockOnTransfertStock`;
delimiter ;;
CREATE PROCEDURE `UpdateRequisitionStatusAndStockOnTransfertStock`(IN p_transfert_id INT)
BEGIN
    DECLARE v_requisition_id INT;
    DECLARE v_stock_id INT;
    DECLARE v_quantite FLOAT;
    DECLARE done INT DEFAULT 0;
    
    -- Déclare un curseur pour parcourir les lignes de transfert
    DECLARE cur CURSOR FOR 
        SELECT ligne.fkStock, ligne.quantite
        FROM lignes_transferts_stock ligne
        WHERE ligne.fkTransfertStock = p_transfert_id;

    -- Gestionnaire pour sortir de la boucle du curseur
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    
    -- Récupérer l'ID de la Requisition associée au TransfertStock
    SELECT fkRequisition INTO v_requisition_id
    FROM transferts_stock
    WHERE id = p_transfert_id;

    -- Si un transfert stock est associé à une réquisition, mettre à jour le statut
    IF v_requisition_id IS NOT NULL THEN
        UPDATE requisitions
        SET statut = 'TRANSFEREE'
        WHERE id = v_requisition_id;
    END IF;

    -- Ouvrir le curseur pour parcourir les lignes de transfert associées
    OPEN cur;

    read_loop: LOOP
        -- Lire les lignes de transfert
        FETCH cur INTO v_stock_id, v_quantite;
        
        -- Si toutes les lignes sont parcourues, sortir de la boucle
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Mettre à jour le stock du produit (retrait de la quantité transférée)
        UPDATE stock_produits
        SET qte = qte - v_quantite
        WHERE id = v_stock_id;
        
    END LOOP;

    -- Fermer le curseur après utilisation
    CLOSE cur;

END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for UpdateTransfertRequisitionStatusAndStockOnReceptionStock
-- ----------------------------
DROP PROCEDURE IF EXISTS `UpdateTransfertRequisitionStatusAndStockOnReceptionStock`;
delimiter ;;
CREATE PROCEDURE `UpdateTransfertRequisitionStatusAndStockOnReceptionStock`(IN p_reception_id INT)
BEGIN
    DECLARE v_transfert_id INT;
    DECLARE v_requisition_id INT;
    DECLARE v_stock_id INT;
    DECLARE v_quantite FLOAT;
    DECLARE v_src_pharmacie INT;
    DECLARE v_dst_pharmacie INT;
    DECLARE done INT DEFAULT 0;

    -- Curseur: lignes réception
    DECLARE cur CURSOR FOR
        SELECT ligne.fkStock, ligne.quantite
        FROM lignes_reception_stock ligne
        WHERE ligne.fkReceptionStock = p_reception_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Transfert lié
    SELECT fkTransfert INTO v_transfert_id
    FROM reception_stock
    WHERE id = p_reception_id;

    IF v_transfert_id IS NOT NULL THEN

        UPDATE transferts_stock
        SET statut = 'RECEPTIONNEE'
        WHERE id = v_transfert_id;

        -- Requisition liée
        SELECT fkRequisition INTO v_requisition_id
        FROM transferts_stock
        WHERE id = v_transfert_id;

        IF v_requisition_id IS NOT NULL THEN

            UPDATE requisitions
            SET statut = 'RECEPTIONNEE'
            WHERE id = v_requisition_id;

            -- Pharmacie source & destination (selon ta table requisitions)
            SELECT r.fkPharmacieStock, r.fkPharmacie
              INTO v_src_pharmacie, v_dst_pharmacie
            FROM requisitions r
            WHERE r.id = v_requisition_id;

        END IF;
    END IF;

    -- Update stock (comme avant)
    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_stock_id, v_quantite;

        IF done THEN
            LEAVE read_loop;
        END IF;

        UPDATE stock_produits
        SET qte = qte + v_quantite,
            operationnel = TRUE
        WHERE id = v_stock_id;

    END LOOP;

    CLOSE cur;

    -- Copier perimable_alerte_stock de la pharmacie source -> destination (si absent)
    -- Mapping stock source -> stock destination via fkProduit
   -- IF v_src_pharmacie IS NOT NULL AND v_dst_pharmacie IS NOT NULL THEN

     --   INSERT INTO perimable_alerte_stock
     --   (
      --      fkStock,
       --     fkAprov,
        --    dateperemtion,
        --    notifactif,
         --   approv,
          --  stockexpiree,
--            datecreate,
  --          dateupdate,
    --        usercreateid,
      --      userupdateid
      --  )
       -- SELECT
        --    sp_dst.id,
         --   pas_src.fkAprov,
          --  pas_src.dateperemtion,
           -- pas_src.notifactif,
           -- pas_src.approv,
           -- pas_src.stockexpiree,
            -- NOW(),
            -- NOW(),
            -- pas_src.usercreateid,
            -- pas_src.userupdateid
     --   FROM perimable_alerte_stock pas_src
     --   JOIN stock_produits sp_src
      --    ON sp_src.id = pas_src.fkStock
      --   AND sp_src.fkPharmacie = v_src_pharmacie
      --  JOIN stock_produits sp_dst
       --   ON sp_dst.fkPharmacies = v_dst_pharmacie
       --  AND sp_dst.fkProduit   = sp_src.fkProduit
      --  WHERE NOT EXISTS (
       --     SELECT 1
        --    FROM perimable_alerte_stock pas_dst
        --    WHERE pas_dst.fkStock = sp_dst.id
        --      AND pas_dst.fkAprov = pas_src.fkAprov
--              AND pas_dst.dateperemtion = pas_src.dateperemtion
  --      );

  --  END IF;

END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for UpdateTransfertStatusAndStockOnReceptionStock
-- ----------------------------
DROP PROCEDURE IF EXISTS `UpdateTransfertStatusAndStockOnReceptionStock`;
delimiter ;;
CREATE PROCEDURE `UpdateTransfertStatusAndStockOnReceptionStock`(IN p_reception_id INT)
BEGIN
    DECLARE v_transfert_id INT;
    DECLARE v_requisition_id INT;
    DECLARE v_stock_id INT;
    DECLARE v_quantite FLOAT;
    DECLARE v_src_pharmacie INT;
    DECLARE v_dst_pharmacie INT;
    DECLARE done INT DEFAULT 0;

    -- Curseur: lignes réception
    DECLARE cur CURSOR FOR
        SELECT ligne.fkStock, ligne.quantite
        FROM lignes_reception_transfert_interne ligne
        WHERE ligne.fkReceptionStock = p_reception_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Transfert lié
    SELECT fkTransfertInterne INTO v_transfert_id
    FROM reception_transfert_interne
    WHERE id = p_reception_id;

    IF v_transfert_id IS NOT NULL THEN

        UPDATE transfert_interne
        SET statut = 'RECEPTIONNEE'
        WHERE id = v_transfert_id;

    END IF;

    -- Update stock (comme avant)
    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_stock_id, v_quantite;

        IF done THEN
            LEAVE read_loop;
        END IF;

        UPDATE stock_produits
        SET qte = qte + v_quantite,
            operationnel = TRUE
        WHERE id = v_stock_id;

    END LOOP;

    CLOSE cur;

    -- Copier perimable_alerte_stock de la pharmacie source -> destination (si absent)
    -- Mapping stock source -> stock destination via fkProduit
    IF v_src_pharmacie IS NOT NULL AND v_dst_pharmacie IS NOT NULL THEN

        INSERT INTO perimable_alerte_stock
        (
            fkStock,
            fkAprov,
            dateperemption,
            notifactiv,
            approv,
            stockexpiree,
            datecreate,
            dateupdate,
            usercreateid,
            userupdateid
        )
        SELECT
            sp_dst.id,
            pas_src.fkAprov,
            pas_src.dateperemption,
            pas_src.notifactif,
            pas_src.approv,
            pas_src.stockexpiree,
            NOW(),
            NOW(),
            pas_src.usercreateid,
            pas_src.userupdateid
        FROM perimable_alerte_stock pas_src
        JOIN stock_produits sp_src
          ON sp_src.id = pas_src.fkStock
         AND sp_src.fkPharmacie = v_src_pharmacie
        JOIN stock_produits sp_dst
          ON sp_dst.fkPharmacie = v_dst_pharmacie
         AND sp_dst.fkProduit   = sp_src.fkProduit
        WHERE NOT EXISTS (
            SELECT 1
            FROM perimable_alerte_stock pas_dst
            WHERE pas_dst.fkStock = sp_dst.id
              AND pas_dst.fkAprov = pas_src.fkAprov
              AND pas_dst.dateperemption = pas_src.dateperemption
        );

    END IF;

END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for update_permissions_for_role
-- ----------------------------
DROP PROCEDURE IF EXISTS `update_permissions_for_role`;
delimiter ;;
CREATE PROCEDURE `update_permissions_for_role`(IN role_id BIGINT)
BEGIN
    -- Suppression des permissions actuelles des utilisateurs ayant ce rôle
    DELETE FROM utilisateurs_permissions
    WHERE fkUtilisateur IN (
        SELECT id FROM utilisateurs WHERE fkRole = role_id
    );

    -- Insertion des nouvelles permissions basées sur le rôle
    INSERT INTO utilisateurs_permissions (fkUtilisateur, fkPermission, datecreate, dateupdate)
    SELECT u.id, rp.fkPermission, NOW(), NOW()
    FROM utilisateurs u
    JOIN roles_permissions rp ON rp.fkRole = u.fkRole
    WHERE u.fkRole = role_id;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for update_stock_quantity
-- ----------------------------
DROP PROCEDURE IF EXISTS `update_stock_quantity`;
delimiter ;;
CREATE PROCEDURE `update_stock_quantity`(IN stockId BIGINT, IN expiree FLOAT)
BEGIN
    -- Mettre à jour la quantité dans la table stock_produits
    UPDATE stock_produits
    SET qte = qte - expiree
    WHERE id = stockId;

END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table approvsionnements
-- ----------------------------
DROP TRIGGER IF EXISTS `newupdattrigerappro`;
delimiter ;;
CREATE TRIGGER `newupdattrigerappro` AFTER UPDATE ON `approvsionnements` FOR EACH ROW BEGIN
    -- Vérifier si le statut de l'approvisionnement est passé à "VALIDEE"
    IF NEW.statut = 'VALIDEE' AND OLD.statut != 'VALIDEE' THEN
        -- Appeler la procédure pour ajouter le stock
       CALL AddStockAfterApprov(NEW.id);
    END IF; 
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table inventaires
-- ----------------------------
DROP TRIGGER IF EXISTS `trg_after_insert_inventaire`;
delimiter ;;
CREATE TRIGGER `trg_after_insert_inventaire` AFTER INSERT ON `inventaires` FOR EACH ROW BEGIN
    -- Appeler la procédure pour insérer les lignes d'inventaire pour le nouvel inventaire
  CALL InsertLignesInventaire(NEW.id, NEW.fkPharmacie, NEW.usercreateid);
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table inventaires
-- ----------------------------
DROP TRIGGER IF EXISTS `after_inventaire_validation`;
delimiter ;;
CREATE TRIGGER `after_inventaire_validation` AFTER UPDATE ON `inventaires` FOR EACH ROW BEGIN
    -- Vérifie si le statut de l'inventaire est passé à "TERMINE"
    IF NEW.statut = 'TERMINE' THEN
       -- Appelle la procédure pour ajuster le stock
     CALL ajuster_stock_apres_inventaire(NEW.id);
  
   END IF;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table perimable_alerte_stock
-- ----------------------------
DROP TRIGGER IF EXISTS `trigger_perimable`;
delimiter ;;
CREATE TRIGGER `trigger_perimable` AFTER UPDATE ON `perimable_alerte_stock` FOR EACH ROW BEGIN
    -- Si l'alerte devient inactive (notifactif = false)
  -- IF NEW.notifactif = false THEN
        -- Appeler la procédure pour mettre à jour le stock
    --   CALL update_stock_quantity(NEW.fkStock, NEW.stockexpiree);
    -- END IF; 
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table pharmacies
-- ----------------------------
DROP TRIGGER IF EXISTS `after_insert_pharmacies`;
delimiter ;;
CREATE TRIGGER `after_insert_pharmacies` AFTER INSERT ON `pharmacies` FOR EACH ROW BEGIN
    -- Appel de la procédure pour insérer le stock pour la nouvelle pharmacie
    CALL ajouter_stock_pour_nouvelle_pharmacie(NEW.id); 
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table produits
-- ----------------------------
DROP TRIGGER IF EXISTS `after_insert_produits`;
delimiter ;;
CREATE TRIGGER `after_insert_produits` AFTER INSERT ON `produits` FOR EACH ROW BEGIN
    -- Appel de la procédure pour insérer le stock avec produit_id
    CALL ajouter_stock_pour_pharmacies(NEW.id);
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table reception_stock
-- ----------------------------
DROP TRIGGER IF EXISTS `trigger_ajouter_lignes_reception`;
delimiter ;;
CREATE TRIGGER `trigger_ajouter_lignes_reception` AFTER INSERT ON `reception_stock` FOR EACH ROW BEGIN
    CALL AjouterLignesReception(NEW.id);
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table reception_stock
-- ----------------------------
DROP TRIGGER IF EXISTS `after_reception_status_update`;
delimiter ;;
CREATE TRIGGER `after_reception_status_update` AFTER UPDATE ON `reception_stock` FOR EACH ROW BEGIN
    -- Vérifier si le nouveau statut est "TRANSFÉRÉE"
    IF NEW.statut = 'RECEPTIONNEE' THEN
        -- Appeler la procédure pour mettre à jour le statut de la réquisition associée
        CALL UpdateTransfertRequisitionStatusAndStockOnReceptionStock(NEW.id);
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table reception_transfert_interne
-- ----------------------------
DROP TRIGGER IF EXISTS `trigger_ajouter_lignes_reception_copy1`;
delimiter ;;
CREATE TRIGGER `trigger_ajouter_lignes_reception_copy1` AFTER INSERT ON `reception_transfert_interne` FOR EACH ROW BEGIN
    CALL AjouterLignesReceptionTransfertInterne(NEW.id);
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table reception_transfert_interne
-- ----------------------------
DROP TRIGGER IF EXISTS `after_reception_status_update_copy1`;
delimiter ;;
CREATE TRIGGER `after_reception_status_update_copy1` AFTER UPDATE ON `reception_transfert_interne` FOR EACH ROW BEGIN
    -- Vérifier si le nouveau statut est "TRANSFÉRÉE"
    IF NEW.statut = 'RECEPTIONNEE' THEN
        -- Appeler la procédure pour mettre à jour le statut de la réquisition associée
        CALL UpdateTransfertStatusAndStockOnReceptionStock(NEW.id);
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table roles_permissions
-- ----------------------------
DROP TRIGGER IF EXISTS `update_permissions_on_role_insert`;
delimiter ;;
CREATE TRIGGER `update_permissions_on_role_insert` AFTER INSERT ON `roles_permissions` FOR EACH ROW BEGIN
    -- Appeler la procédure pour mettre à jour les permissions des utilisateurs après insertion d'une nouvelle permission pour un rôle
    IF NEW.fkRole IS NOT NULL THEN
        CALL update_permissions_for_role(NEW.fkRole);
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table roles_permissions
-- ----------------------------
DROP TRIGGER IF EXISTS `update_permissions_on_role_delete`;
delimiter ;;
CREATE TRIGGER `update_permissions_on_role_delete` AFTER DELETE ON `roles_permissions` FOR EACH ROW BEGIN
    -- Appeler la procédure pour mettre à jour les permissions des utilisateurs après suppression d'une permission pour un rôle
    IF OLD.fkRole IS NOT NULL THEN
        CALL update_permissions_for_role(OLD.fkRole);
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table transferts_stock
-- ----------------------------
DROP TRIGGER IF EXISTS `trigger_ajouter_lignes_transfert`;
delimiter ;;
CREATE TRIGGER `trigger_ajouter_lignes_transfert` AFTER INSERT ON `transferts_stock` FOR EACH ROW BEGIN 
    CALL AjouterLignesTransfert(NEW.id);  
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table transferts_stock
-- ----------------------------
DROP TRIGGER IF EXISTS `after_transfert_status_update`;
delimiter ;;
CREATE TRIGGER `after_transfert_status_update` AFTER UPDATE ON `transferts_stock` FOR EACH ROW BEGIN
    -- Vérifier si le nouveau statut est "TRANSFÉRÉE"
    IF NEW.statut = 'TRANSFEREE' THEN
        -- Appeler la procédure pour mettre à jour le statut de la réquisition associée
       CALL UpdateRequisitionStatusAndStockOnTransfertStock(NEW.id); 
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table utilisateurs
-- ----------------------------
DROP TRIGGER IF EXISTS `assign_permissions_on_user_creation`;
delimiter ;;
CREATE TRIGGER `assign_permissions_on_user_creation` AFTER INSERT ON `utilisateurs` FOR EACH ROW BEGIN
    -- Appeler la procédure stockée pour assigner les permissions
    CALL assign_permissions_to_user(NEW.id, NEW.fkRole);
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table ventes
-- ----------------------------
DROP TRIGGER IF EXISTS `TriggerReduceStockAfterSale`;
delimiter ;;
CREATE TRIGGER `TriggerReduceStockAfterSale` AFTER UPDATE ON `ventes` FOR EACH ROW BEGIN
    -- Vérifier si le statut de la vente est passé à "PAYEE" ou "FACTUREE"
    IF (NEW.statut = 'PAYEE' OR NEW.statut = 'FACTUREE' OR NEW.statut = 'SORTIE-USAGE') 
       AND (OLD.statut != 'PAYEE' AND OLD.statut != 'FACTUREE' OR OLD.statut != 'SORTIE-USAGE') THEN
        -- Appeler la procédure stockée pour réduire le stock
        CALL ReduceStockAfterSale(NEW.id); 
    END IF;
		
		 -- Vérifier si le statut de la vente est passé à "PAYEE" ou "FACTUREE"
    IF (NEW.statut = 'ANNULEE-REMBOURSE') 
       AND (OLD.statut != 'ANNULEE-REMBOURSE' ) THEN
        -- Appeler la procédure stockée pour réduire le stock
        CALL RestoreStockAfterAnnule(NEW.id);
    END IF;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
