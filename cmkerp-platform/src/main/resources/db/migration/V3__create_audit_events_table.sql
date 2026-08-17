-- Migration Flyway : Création de la table audit_events pour la persistance des événements d'audit
-- Cette table permet de :
-- 1. Persister tous les événements d'audit reçus depuis Kafka
-- 2. Maintenir un historique complet et inviolable des actions utilisateur
-- 3. Permettre la recherche et l'analyse des événements d'audit

CREATE TABLE IF NOT EXISTS audit_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id VARCHAR(36) NOT NULL COMMENT 'UUID de l''événement (depuis DomainEvent)',
  event_type VARCHAR(100) NOT NULL COMMENT 'Type d''événement (AUDIT_EVENT)',
  event_timestamp DATETIME NOT NULL COMMENT 'Timestamp de l''événement (depuis DomainEvent)',
  user_id BIGINT NULL COMMENT 'ID de l''utilisateur ayant effectué l''action',
  username VARCHAR(255) NULL COMMENT 'Nom d''utilisateur',
  action VARCHAR(100) NOT NULL COMMENT 'Action effectuée (CREATE, UPDATE, DELETE, etc.)',
  resource_type VARCHAR(100) NULL COMMENT 'Type de ressource affectée (User, Pharmacie, etc.)',
  resource_id BIGINT NULL COMMENT 'ID de la ressource affectée',
  details TEXT NULL COMMENT 'Détails supplémentaires de l''action (JSON ou texte)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création de l''enregistrement',
  INDEX idx_user_id (user_id) COMMENT 'Index pour recherche par utilisateur',
  INDEX idx_action (action) COMMENT 'Index pour recherche par action',
  INDEX idx_resource (resource_type, resource_id) COMMENT 'Index pour recherche par ressource',
  INDEX idx_event_timestamp (event_timestamp) COMMENT 'Index pour recherche par date',
  INDEX idx_created_at (created_at) COMMENT 'Index pour recherche par date de création',
  INDEX idx_event_id (event_id) COMMENT 'Index unique pour éviter les doublons'
) ENGINE = InnoDB
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT = 'Table d''audit pour événements d''audit persistés depuis Kafka';





