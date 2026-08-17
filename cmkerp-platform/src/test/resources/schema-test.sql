-- ==========================================
-- Script d'initialisation SQL pour les tests MySQL
-- ==========================================
-- Ce script crée les tables nécessaires pour les tests JDBC qui n'utilisent pas JPA
-- Syntaxe MySQL 8.0

-- Table: sites
CREATE TABLE IF NOT EXISTS sites (
  id BIGINT NOT NULL AUTO_INCREMENT,
  designation VARCHAR(100),
  abbreviation VARCHAR(2),
  addresse VARCHAR(255),
  bloquer TINYINT(1) NOT NULL DEFAULT 0,
  datecreate TIMESTAMP NULL DEFAULT NULL,
  dateupdate TIMESTAMP NULL DEFAULT NULL,
  usercreatedid BIGINT,
  userupdatedid BIGINT,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: roles
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nom VARCHAR(50) NOT NULL,
  description VARCHAR(100),
  datecreate TIMESTAMP NULL DEFAULT NULL,
  dateupdate TIMESTAMP NULL DEFAULT NULL,
  usercreatedid BIGINT,
  userupdatedid BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT UNIQUEROLE UNIQUE (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

