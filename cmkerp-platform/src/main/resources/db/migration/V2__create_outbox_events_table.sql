-- Migration Flyway : Création de la table outbox_events pour le pattern Outbox
-- Pattern Outbox : garantit la publication transactionnelle des événements vers Kafka
--
-- Cette table permet de :
-- 1. Persister les événements dans la même transaction DB que l'entité métier
-- 2. Un processus séparé (OutboxEventPublisher) publie les événements PENDING vers Kafka
-- 3. Garantit que l'événement est publié même en cas de crash après le commit

CREATE TABLE IF NOT EXISTS outbox_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_type VARCHAR(100) NOT NULL COMMENT 'Type d''événement (USER_EVENT, AUDIT_EVENT, etc.)',
  topic VARCHAR(255) NOT NULL COMMENT 'Topic Kafka de destination',
  event_key VARCHAR(255) NOT NULL COMMENT 'Clé de partition Kafka (userId, etc.)',
  event_payload TEXT NOT NULL COMMENT 'Payload JSON de l''événement',
  status ENUM('PENDING', 'PUBLISHED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT 'Statut de publication',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Nombre de tentatives de publication',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  published_at DATETIME NULL COMMENT 'Date de publication réussie',
  error_message TEXT NULL COMMENT 'Message d''erreur en cas d''échec',
  INDEX idx_status_created (status, created_at) COMMENT 'Index pour récupérer les événements PENDING',
  INDEX idx_event_type (event_type) COMMENT 'Index pour filtrage par type d''événement'
) ENGINE = InnoDB
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT = 'Table Outbox pour événements Kafka (pattern Outbox)';
