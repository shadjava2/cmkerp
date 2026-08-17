CREATE TABLE IF NOT EXISTS stock_intelligence_email_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_type VARCHAR(20) NOT NULL,
  recipient VARCHAR(255) NOT NULL,
  status ENUM('SENT', 'FAILED', 'SKIPPED') NOT NULL,
  snapshot_id BIGINT UNSIGNED NULL,
  error_detail TEXT NULL,
  sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_si_email_log_sent (sent_at),
  INDEX idx_si_email_log_report (report_type, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
