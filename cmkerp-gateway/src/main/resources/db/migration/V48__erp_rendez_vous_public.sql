CREATE TABLE IF NOT EXISTS erp_rdv_creneaux (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  fk_medecin BIGINT NOT NULL,
  fk_service BIGINT NOT NULL,
  fk_location BIGINT NOT NULL,
  debut DATETIME NOT NULL COMMENT 'UTC',
  fin DATETIME NOT NULL COMMENT 'UTC',
  public_visible BOOLEAN NOT NULL DEFAULT FALSE,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rdv_medecin_debut (fk_medecin, debut),
  KEY ix_rdv_public_debut (public_visible, debut),
  FOREIGN KEY (fk_medecin) REFERENCES erp_medecins(id),
  FOREIGN KEY (fk_service) REFERENCES erp_services(id),
  FOREIGN KEY (fk_location) REFERENCES erp_locations(id),
  CHECK (fin > debut)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS erp_rdv_limites (
  source_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  bucket BIGINT NOT NULL,
  tentatives INT NOT NULL DEFAULT 1,
  PRIMARY KEY (source_hash, bucket),
  KEY ix_rdv_limit_bucket (bucket)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS erp_rdv_comptes (
  email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  mediline_ipp VARCHAR(25) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  linked_at DATETIME NULL,
  UNIQUE KEY uk_rdv_mediline_ipp (mediline_ipp)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS erp_rdv_liens (
  token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  purpose VARCHAR(12) NOT NULL,
  fk_creneau BIGINT NULL,
  valid_until DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  throttle_bucket BIGINT NOT NULL,
  UNIQUE KEY uk_rdv_email_window (email, throttle_bucket),
  KEY ix_rdv_lien_expiry (valid_until),
  FOREIGN KEY (fk_creneau) REFERENCES erp_rdv_creneaux(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS erp_rdv_sessions (
  token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  purpose VARCHAR(12) NOT NULL,
  fk_creneau BIGINT NULL,
  valid_until DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  KEY ix_rdv_session_expiry (valid_until)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS erp_rendez_vous (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  reference CHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  session_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  fk_creneau BIGINT NOT NULL,
  fk_creneau_actif BIGINT NULL UNIQUE,
  email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  patient_nom VARCHAR(100) NOT NULL,
  patient_prenom VARCHAR(100) NOT NULL,
  mediline_ipp VARCHAR(25) NULL,
  statut VARCHAR(12) NOT NULL DEFAULT 'CONFIRME',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cancelled_at DATETIME NULL,
  KEY ix_rdv_email (email, created_at),
  KEY ix_rdv_session (session_hash),
  FOREIGN KEY (fk_creneau) REFERENCES erp_rdv_creneaux(id),
  FOREIGN KEY (fk_creneau_actif) REFERENCES erp_rdv_creneaux(id)
) ENGINE=InnoDB;
