-- ==========================================
-- FLYWAY BASELINE - Schéma CMK-ERP
-- ==========================================
-- V1 : baseline schéma CMK-ERP (export cmkerp-v24prod du 29/11/2025)
-- Généré automatiquement depuis le dump Navicat
--
-- Ce fichier contient uniquement la structure (DDL),
-- sans données métiers.
-- Les données de référence seront dans V2__seed_reference_data.sql
-- Les procédures stockées et triggers seront dans V3__procedures_triggers.sql
-- ==========================================

-- Table: devenir
CREATE TABLE IF NOT EXISTS `devenir` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 341 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: entreprises
CREATE TABLE IF NOT EXISTS `entreprises` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 208 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: categorie_immo
CREATE TABLE IF NOT EXISTS `categorie_immo` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- Table: chambres
CREATE TABLE IF NOT EXISTS `chambres` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `fkService` bigint NULL DEFAULT NULL,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: dosages
CREATE TABLE IF NOT EXISTS `dosages` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 442 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: categorie_agent
CREATE TABLE IF NOT EXISTS `categorie_agent` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: t_produit
CREATE TABLE IF NOT EXISTS `t_produit` (
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
) ENGINE = InnoDB CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = DYNAMIC;

-- Table: type_mouvement
CREATE TABLE IF NOT EXISTS `type_mouvement` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: type_suivi
CREATE TABLE IF NOT EXISTS `type_suivi` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: requisitions
CREATE TABLE IF NOT EXISTS `requisitions` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 13602 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: utilisateurs
CREATE TABLE IF NOT EXISTS `utilisateurs` (
`id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `specialite` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `nom` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `postnom` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `prenom` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `username` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
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
) ENGINE = InnoDB AUTO_INCREMENT = 189 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: ventes
CREATE TABLE IF NOT EXISTS `ventes` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 52253 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: document_administratif
CREATE TABLE IF NOT EXISTS `document_administratif` (
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

-- Table: carrieres
CREATE TABLE IF NOT EXISTS `carrieres` (
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

-- Table: pieces_jointes_notif
CREATE TABLE IF NOT EXISTS `pieces_jointes_notif` (
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

-- Table: roles
CREATE TABLE IF NOT EXISTS `roles` (
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

-- Table: conges
CREATE TABLE IF NOT EXISTS `conges` (
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

-- Table: orientation_devenir
CREATE TABLE IF NOT EXISTS `orientation_devenir` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 17452 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: parametres_vitaux
CREATE TABLE IF NOT EXISTS `parametres_vitaux` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: produits
CREATE TABLE IF NOT EXISTS `produits` (
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
  `datecreate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreatedid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `indexproduitid`(`fkForme` ASC, `fkDosage` ASC, `fkConditionnement` ASC, `fkCategorie` ASC) USING BTREE,
  INDEX `uniquecodebarre`(`codebarre` ASC) USING BTREE,
  INDEX `idx_produits_fk`(`fkForme` ASC, `fkDosage` ASC, `fkConditionnement` ASC, `fkCategorie` ASC) USING BTREE,
  INDEX `idx_nomcommercial`(`nomcommercial` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7639 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: sites
CREATE TABLE IF NOT EXISTS `sites` (
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

-- Table: echange_devise
CREATE TABLE IF NOT EXISTS `echange_devise` (
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

-- Table: suivi_administratif
CREATE TABLE IF NOT EXISTS `suivi_administratif` (
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

-- Table: suivi_administratif_document
CREATE TABLE IF NOT EXISTS `suivi_administratif_document` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `fkSuivi` bigint NULL DEFAULT NULL,
  `fkDocument` bigint NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: patients
CREATE TABLE IF NOT EXISTS `patients` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 23509 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: approvsionnements
CREATE TABLE IF NOT EXISTS `approvsionnements` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1984 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: type_conge
CREATE TABLE IF NOT EXISTS `type_conge` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: utilisateurs_permissions
CREATE TABLE IF NOT EXISTS `utilisateurs_permissions` (
`id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkUtilisateur` bigint UNSIGNED NOT NULL,
  `fkPermission` bigint UNSIGNED NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_user_permis`(`id` ASC, `fkUtilisateur` ASC, `fkPermission` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 91962 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: lits
CREATE TABLE IF NOT EXISTS `lits` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `fkChambres` bigint NULL DEFAULT NULL,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: notifications
CREATE TABLE IF NOT EXISTS `notifications` (
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

-- Table: lignes_transferts_stock
CREATE TABLE IF NOT EXISTS `lignes_transferts_stock` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 117226 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: lignes_reception_stock
CREATE TABLE IF NOT EXISTS `lignes_reception_stock` (
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 84987 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: droits_pharmacies
CREATE TABLE IF NOT EXISTS `droits_pharmacies` (
`id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkUtilisateur` bigint UNSIGNED NULL DEFAULT NULL,
  `fkPharmacie` bigint UNSIGNED NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_droitpharmacie_unique`(`fkUtilisateur` ASC, `fkPharmacie` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 966 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: ordonnances
CREATE TABLE IF NOT EXISTS `ordonnances` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: localisation_immo
CREATE TABLE IF NOT EXISTS `localisation_immo` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkSite` bigint NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: conditionnements
CREATE TABLE IF NOT EXISTS `conditionnements` (
`id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: reception_stock
CREATE TABLE IF NOT EXISTS `reception_stock` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `fkTransfert` bigint NOT NULL,
  `statut` enum('EN ATTENTE','ANNULEE','RECEPTIONNEE') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'EN ATTENTE',
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9997 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: chat_messages
CREATE TABLE IF NOT EXISTS `chat_messages` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: perimable_alerte_stock
CREATE TABLE IF NOT EXISTS `perimable_alerte_stock` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `fkStock` bigint NULL DEFAULT NULL,
  `fkAprov` bigint NULL DEFAULT NULL,
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
  INDEX `idx_perimable_stock_notif_date`(`fkStock` ASC, `notifactif` ASC, `dateperemtion` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6650 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: resumestaffs
CREATE TABLE IF NOT EXISTS `resumestaffs` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: droits_categorie
CREATE TABLE IF NOT EXISTS `droits_categorie` (
`id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkPharmacie` bigint UNSIGNED NULL DEFAULT NULL,
  `fkCategorie` bigint UNSIGNED NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_droitcategorie_unique`(`fkPharmacie` ASC, `fkCategorie` ASC, `id` ASC) USING BTREE,
  INDEX `idx_droits_pharma_cat`(`fkPharmacie` ASC, `fkCategorie` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 790 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: formes
CREATE TABLE IF NOT EXISTS `formes` (
`id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: agentss
CREATE TABLE IF NOT EXISTS `agentss` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_bin ROW_FORMAT = DYNAMIC;

-- Table: t_famille
CREATE TABLE IF NOT EXISTS `t_famille` (
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
) ENGINE = InnoDB CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = DYNAMIC;

-- Table: roles_permissions
CREATE TABLE IF NOT EXISTS `roles_permissions` (
`id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `fkRole` bigint UNSIGNED NOT NULL,
  `fkPermission` bigint UNSIGNED NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint UNSIGNED NULL DEFAULT NULL,
  `userupdateid` bigint UNSIGNED NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_roleperimis_unique`(`id` ASC, `fkRole` ASC, `fkPermission` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 714 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: sous_categorie_immo
CREATE TABLE IF NOT EXISTS `sous_categorie_immo` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- Table: fournisseurs
CREATE TABLE IF NOT EXISTS `fournisseurs` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 206 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: categorie_produit
CREATE TABLE IF NOT EXISTS `categorie_produit` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `abbreviation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: type_document
CREATE TABLE IF NOT EXISTS `type_document` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: produits_convention
CREATE TABLE IF NOT EXISTS `produits_convention` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `fkProduit` bigint NULL DEFAULT NULL,
  `fkConvention` bigint NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 539 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: stock_produits
CREATE TABLE IF NOT EXISTS `stock_produits` (
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
  INDEX `index-stockoptimise`(`id` ASC, `operationnel` ASC, `fkPharmacies` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 340110 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: immobilisation
CREATE TABLE IF NOT EXISTS `immobilisation` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- Table: agents
CREATE TABLE IF NOT EXISTS `agents` (
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

-- Table: personnes_charges
CREATE TABLE IF NOT EXISTS `personnes_charges` (
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

-- Table: lignes_vente
CREATE TABLE IF NOT EXISTS `lignes_vente` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 159892 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: soins_infirmiers
CREATE TABLE IF NOT EXISTS `soins_infirmiers` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: lignes_approv
CREATE TABLE IF NOT EXISTS `lignes_approv` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 9232 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: postes
CREATE TABLE IF NOT EXISTS `postes` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- Table: permissions
CREATE TABLE IF NOT EXISTS `permissions` (
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

-- Table: surveillance
CREATE TABLE IF NOT EXISTS `surveillance` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: tickets
CREATE TABLE IF NOT EXISTS `tickets` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 10232 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: phaseconsultation
CREATE TABLE IF NOT EXISTS `phaseconsultation` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: inventaires
CREATE TABLE IF NOT EXISTS `inventaires` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1726 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: consultations
CREATE TABLE IF NOT EXISTS `consultations` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 4081 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: transferts_stock
CREATE TABLE IF NOT EXISTS `transferts_stock` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 11336 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: pharmacies
CREATE TABLE IF NOT EXISTS `pharmacies` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 64 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: bureau_immo
CREATE TABLE IF NOT EXISTS `bureau_immo` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `designation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `fkLocalisation` bigint NULL DEFAULT NULL,
  `codeimmo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: lignes_inventaire
CREATE TABLE IF NOT EXISTS `lignes_inventaire` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 5302000 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- Table: point_appel
CREATE TABLE IF NOT EXISTS `point_appel` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: conventions
CREATE TABLE IF NOT EXISTS `conventions` (
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

-- Table: lignes_ordonnance
CREATE TABLE IF NOT EXISTS `lignes_ordonnance` (
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- Table: lignes_requisitions
CREATE TABLE IF NOT EXISTS `lignes_requisitions` (
`id` bigint NOT NULL AUTO_INCREMENT,
  `fkRequisition` bigint NOT NULL,
  `fkStock` bigint NOT NULL,
  `quantite` float NOT NULL DEFAULT 0,
  `datecreate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateupdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `usercreateid` bigint NULL DEFAULT NULL,
  `userupdateid` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 69557 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

