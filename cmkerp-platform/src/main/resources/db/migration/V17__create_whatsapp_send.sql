-- Destinataires WhatsApp autorisés pour le chat stock intelligence
CREATE TABLE IF NOT EXISTS whatsapp_send (
  id BIGINT NOT NULL AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL,
  label VARCHAR(100) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_whatsapp_send_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
