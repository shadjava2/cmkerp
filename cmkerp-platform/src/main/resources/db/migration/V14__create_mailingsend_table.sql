-- Destinataires des rapports stock intelligence (matin / soir)
CREATE TABLE IF NOT EXISTS mailingsend (
  id BIGINT NOT NULL AUTO_INCREMENT,
  mail VARCHAR(255) NULL,
  actif TINYINT(1) NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
