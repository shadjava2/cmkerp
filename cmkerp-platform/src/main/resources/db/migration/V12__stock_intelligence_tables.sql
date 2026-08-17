-- Intelligence stock : snapshots, destinataires email, log WhatsApp
CREATE TABLE IF NOT EXISTS stock_intelligence_snapshots (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_type ENUM('MORNING', 'EVENING', 'ON_DEMAND', 'WHATSAPP') NOT NULL DEFAULT 'ON_DEMAND',
  pharmacie_id BIGINT UNSIGNED NULL,
  snapshot_json MEDIUMTEXT NOT NULL,
  ai_analysis_json MEDIUMTEXT NULL,
  generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_si_snapshots_generated (generated_at),
  INDEX idx_si_snapshots_type (report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE IF NOT EXISTS stock_intelligence_recipients (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  label VARCHAR(100) NULL,
  email VARCHAR(255) NULL,
  whatsapp_number VARCHAR(20) NULL,
  receive_morning_report TINYINT(1) NOT NULL DEFAULT 1,
  receive_whatsapp_chat TINYINT(1) NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE IF NOT EXISTS whatsapp_chat_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  wa_message_id VARCHAR(128) NULL,
  from_number VARCHAR(20) NOT NULL,
  direction ENUM('IN', 'OUT') NOT NULL,
  message_text TEXT NULL,
  ai_response TEXT NULL,
  snapshot_id BIGINT UNSIGNED NULL,
  status ENUM('RECEIVED', 'PROCESSED', 'SENT', 'FAILED') NOT NULL DEFAULT 'RECEIVED',
  error_detail TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_wa_log_from (from_number),
  INDEX idx_wa_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
