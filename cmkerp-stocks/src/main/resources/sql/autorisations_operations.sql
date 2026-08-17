-- Table centralisée des demandes d'autorisation (annulations tardives, etc.)
CREATE TABLE IF NOT EXISTS autorisations_operations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  table_cible VARCHAR(100) NOT NULL,
  enregistrement_id BIGINT NOT NULL,
  type_operation VARCHAR(50) NOT NULL,
  statut ENUM('EN_ATTENTE','APPROUVEE','REJETEE') NOT NULL DEFAULT 'EN_ATTENTE',
  motif TEXT,
  datecreate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate TIMESTAMP NULL,
  usercreateid BIGINT,
  userdecideid BIGINT,
  datedecision TIMESTAMP NULL,
  commentaire_decision TEXT,
  INDEX idx_auth_statut (statut, datecreate),
  INDEX idx_auth_cible (table_cible, enregistrement_id, type_operation)
);
