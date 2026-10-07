-- V20 : Journal d'utilisation console CMK (navigation + clics)

CREATE TABLE IF NOT EXISTS app_usage_events (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id BIGINT NOT NULL,
  username VARCHAR(80) NULL,
  event_type VARCHAR(20) NOT NULL,
  module VARCHAR(60) NULL,
  path VARCHAR(500) NULL,
  page_title VARCHAR(255) NULL,
  action_label VARCHAR(255) NULL,
  element VARCHAR(120) NULL,
  how_client VARCHAR(40) NOT NULL DEFAULT 'web',
  user_agent VARCHAR(512) NULL,
  pharmacie_id BIGINT NULL,
  portal VARCHAR(40) NULL,
  details_json TEXT NULL,
  PRIMARY KEY (id),
  INDEX idx_app_usage_occurred (occurred_at),
  INDEX idx_app_usage_user_occurred (user_id, occurred_at),
  INDEX idx_app_usage_type_occurred (event_type, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
