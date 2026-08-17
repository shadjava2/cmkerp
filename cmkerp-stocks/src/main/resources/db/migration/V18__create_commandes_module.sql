-- V18 : Module Commandes fournisseurs (cotation → attribution → BC → réception → évaluation)
-- Compatible MySQL 8. Mode A (triggers stock sur VALIDEE) inchangé.

-- ---------------------------------------------------------------------------
-- Colonnes de lien sur approvsionnements (Mode A / Mode B)
-- ---------------------------------------------------------------------------
SET @dbname = DATABASE();
SET @tablename = 'approvsionnements';
SET @columnname = 'fk_bon_commande';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE approvsionnements ADD COLUMN fk_bon_commande BIGINT NULL'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'fk_reception_commande';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE approvsionnements ADD COLUMN fk_reception_commande BIGINT NULL'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @indexname = 'idx_approv_fk_bon_commande';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = @indexname) > 0,
  'SELECT 1',
  'CREATE INDEX idx_approv_fk_bon_commande ON approvsionnements (fk_bon_commande)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @indexname = 'idx_approv_fk_reception_commande';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = @indexname) > 0,
  'SELECT 1',
  'CREATE INDEX idx_approv_fk_reception_commande ON approvsionnements (fk_reception_commande)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ---------------------------------------------------------------------------
-- Demandes de cotation
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demandes_cotation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  numero VARCHAR(50) NOT NULL,
  objet VARCHAR(500) NOT NULL,
  description TEXT NULL,
  fk_pharmacie_demandeur BIGINT NOT NULL,
  date_limite_reponse DATETIME NULL,
  date_livraison_souhaitee DATE NULL,
  lieu_livraison VARCHAR(500) NULL,
  conditions TEXT NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_demande_cotation_numero (numero),
  KEY idx_dc_pharmacie (fk_pharmacie_demandeur),
  KEY idx_dc_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lignes_demande_cotation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_demande_cotation BIGINT NOT NULL,
  fk_produit BIGINT NOT NULL,
  fk_categorie BIGINT NULL,
  quantite DECIMAL(18,4) NOT NULL,
  specifications TEXT NULL,
  ordre INT NOT NULL DEFAULT 0,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  KEY idx_ldc_demande (fk_demande_cotation),
  KEY idx_ldc_produit (fk_produit),
  CONSTRAINT fk_ldc_demande FOREIGN KEY (fk_demande_cotation) REFERENCES demandes_cotation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Invitations fournisseur + historique envois
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invitations_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_demande_cotation BIGINT NOT NULL,
  fk_fournisseur BIGINT NOT NULL,
  public_token VARCHAR(64) NOT NULL,
  access_code_hash VARCHAR(64) NOT NULL,
  session_token_hash VARCHAR(64) NULL,
  session_expires_at DATETIME NULL,
  unlock_attempts INT NOT NULL DEFAULT 0,
  unlock_locked_until DATETIME NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'CREEE',
  expires_at DATETIME NULL,
  opened_at DATETIME NULL,
  submitted_at DATETIME NULL,
  relances INT NOT NULL DEFAULT 0,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inv_public_token (public_token),
  UNIQUE KEY uk_inv_demande_fourn (fk_demande_cotation, fk_fournisseur),
  KEY idx_inv_fournisseur (fk_fournisseur),
  KEY idx_inv_statut (statut),
  CONSTRAINT fk_inv_demande FOREIGN KEY (fk_demande_cotation) REFERENCES demandes_cotation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS historique_envois_invitation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_invitation BIGINT NOT NULL,
  canal VARCHAR(30) NOT NULL DEFAULT 'EMAIL',
  destinataire VARCHAR(255) NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  message_ref VARCHAR(255) NULL,
  date_envoi DATETIME NULL,
  erreur TEXT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_hei_invitation (fk_invitation),
  CONSTRAINT fk_hei_invitation FOREIGN KEY (fk_invitation) REFERENCES invitations_fournisseur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Offres fournisseur
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS offres_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_invitation BIGINT NOT NULL,
  fk_demande_cotation BIGINT NOT NULL,
  fk_fournisseur BIGINT NOT NULL,
  devise VARCHAR(10) NULL,
  taux_declare DECIMAL(18,6) NULL,
  validite_jusquau DATE NULL,
  frais_livraison DECIMAL(18,4) NULL,
  conditions TEXT NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
  version_no INT NOT NULL DEFAULT 1,
  date_soumission DATETIME NULL,
  locked_at DATETIME NULL,
  idempotence_submit_key VARCHAR(100) NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_offre_invitation (fk_invitation),
  UNIQUE KEY uk_offre_idempotence (idempotence_submit_key),
  KEY idx_offre_demande (fk_demande_cotation),
  KEY idx_offre_fournisseur (fk_fournisseur),
  KEY idx_offre_statut (statut),
  CONSTRAINT fk_offre_invitation FOREIGN KEY (fk_invitation) REFERENCES invitations_fournisseur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS versions_offre_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_offre BIGINT NOT NULL,
  version_no INT NOT NULL,
  snapshot_json LONGTEXT NOT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_version_offre (fk_offre, version_no),
  CONSTRAINT fk_vof_offre FOREIGN KEY (fk_offre) REFERENCES offres_fournisseur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lignes_offre_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_offre BIGINT NOT NULL,
  fk_ligne_demande BIGINT NOT NULL,
  prix_original DECIMAL(18,4) NOT NULL,
  devise VARCHAR(10) NOT NULL,
  taux DECIMAL(18,6) NULL,
  fk_echange_devise BIGINT NULL,
  prix_usd DECIMAL(18,4) NULL,
  prix_cdf DECIMAL(18,4) NULL,
  quantite_disponible DECIMAL(18,4) NULL,
  delai_jours INT NULL,
  substitution VARCHAR(500) NULL,
  commentaire TEXT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_lof_offre (fk_offre),
  KEY idx_lof_ligne_demande (fk_ligne_demande),
  CONSTRAINT fk_lof_offre FOREIGN KEY (fk_offre) REFERENCES offres_fournisseur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pieces_jointes_offre (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_offre BIGINT NOT NULL,
  nom_fichier VARCHAR(255) NOT NULL,
  mime_type VARCHAR(100) NULL,
  taille BIGINT NULL,
  storage_key VARCHAR(500) NOT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usercreateid BIGINT NULL,
  PRIMARY KEY (id),
  KEY idx_pjo_offre (fk_offre),
  CONSTRAINT fk_pjo_offre FOREIGN KEY (fk_offre) REFERENCES offres_fournisseur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS demandes_reouverture_offre (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_offre BIGINT NOT NULL,
  motif TEXT NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
  nouvelle_date_limite DATETIME NULL,
  commentaire_decision TEXT NULL,
  date_decision DATETIME NULL,
  decideur_id BIGINT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_dro_offre (fk_offre),
  KEY idx_dro_statut (statut),
  CONSTRAINT fk_dro_offre FOREIGN KEY (fk_offre) REFERENCES offres_fournisseur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Attributions
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attributions_cotation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_demande_cotation BIGINT NOT NULL,
  scope VARCHAR(30) NOT NULL,
  justification TEXT NOT NULL,
  fk_categorie BIGINT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  KEY idx_ac_demande (fk_demande_cotation),
  CONSTRAINT fk_ac_demande FOREIGN KEY (fk_demande_cotation) REFERENCES demandes_cotation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lignes_attribution (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_attribution BIGINT NOT NULL,
  fk_ligne_demande BIGINT NOT NULL,
  fk_fournisseur BIGINT NOT NULL,
  quantite_attribuee DECIMAL(18,4) NOT NULL,
  motif TEXT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_la_attribution (fk_attribution),
  CONSTRAINT fk_la_attribution FOREIGN KEY (fk_attribution) REFERENCES attributions_cotation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Bons de commande
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bons_commande (
  id BIGINT NOT NULL AUTO_INCREMENT,
  numero VARCHAR(50) NOT NULL,
  fk_demande_cotation BIGINT NULL,
  fk_attribution BIGINT NULL,
  fk_fournisseur BIGINT NOT NULL,
  fk_pharmacie BIGINT NULL,
  fk_echange_devise BIGINT NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
  montant_total_usd DECIMAL(18,4) NULL,
  date_commande DATE NULL,
  date_livraison_prevue DATE NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bc_numero (numero),
  KEY idx_bc_fournisseur (fk_fournisseur),
  KEY idx_bc_demande (fk_demande_cotation),
  KEY idx_bc_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lignes_bon_commande (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_bon_commande BIGINT NOT NULL,
  fk_ligne_demande BIGINT NULL,
  fk_produit BIGINT NOT NULL,
  quantite_commandee DECIMAL(18,4) NOT NULL,
  quantite_recue DECIMAL(18,4) NOT NULL DEFAULT 0,
  prix_unitaire_usd DECIMAL(18,4) NULL,
  montant_ligne_usd DECIMAL(18,4) NULL,
  prix_original DECIMAL(18,4) NULL,
  devise VARCHAR(10) NULL,
  taux DECIMAL(18,6) NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_lbc_bon (fk_bon_commande),
  KEY idx_lbc_produit (fk_produit),
  CONSTRAINT fk_lbc_bon FOREIGN KEY (fk_bon_commande) REFERENCES bons_commande (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Réceptions
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS receptions_commande (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_bon_commande BIGINT NOT NULL,
  numero VARCHAR(50) NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
  date_reception DATE NULL,
  fk_approvisionnement BIGINT NULL,
  commentaire TEXT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  KEY idx_rc_bon (fk_bon_commande),
  KEY idx_rc_approv (fk_approvisionnement),
  CONSTRAINT fk_rc_bon FOREIGN KEY (fk_bon_commande) REFERENCES bons_commande (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lignes_reception_commande (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_reception BIGINT NOT NULL,
  fk_ligne_bon_commande BIGINT NOT NULL,
  quantite_recue DECIMAL(18,4) NOT NULL,
  lot VARCHAR(100) NULL,
  date_peremption DATE NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_lrc_reception (fk_reception),
  CONSTRAINT fk_lrc_reception FOREIGN KEY (fk_reception) REFERENCES receptions_commande (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Évaluations & scoring
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluations_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_fournisseur BIGINT NOT NULL,
  fk_bon_commande BIGINT NULL,
  fk_reception BIGINT NULL,
  note_delais DECIMAL(5,2) NOT NULL,
  note_qualite DECIMAL(5,2) NOT NULL,
  note_prix DECIMAL(5,2) NOT NULL,
  note_completude DECIMAL(5,2) NOT NULL,
  note_reactivite DECIMAL(5,2) NOT NULL,
  score_global DECIMAL(8,4) NULL,
  commentaire TEXT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  usercreateid BIGINT NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id),
  KEY idx_ef_fournisseur (fk_fournisseur),
  KEY idx_ef_bc (fk_bon_commande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS snapshots_perf_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_fournisseur BIGINT NOT NULL,
  periode VARCHAR(20) NOT NULL,
  score_moyen DECIMAL(8,4) NULL,
  nb_evaluations INT NOT NULL DEFAULT 0,
  snapshot_json LONGTEXT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_spf_fourn_periode (fk_fournisseur, periode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS param_score_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  poids_delais DECIMAL(5,2) NOT NULL DEFAULT 30.00,
  poids_qualite DECIMAL(5,2) NOT NULL DEFAULT 25.00,
  poids_prix DECIMAL(5,2) NOT NULL DEFAULT 20.00,
  poids_completude DECIMAL(5,2) NOT NULL DEFAULT 15.00,
  poids_reactivite DECIMAL(5,2) NOT NULL DEFAULT 10.00,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  userupdateid BIGINT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO param_score_fournisseur (poids_delais, poids_qualite, poids_prix, poids_completude, poids_reactivite)
SELECT 30.00, 25.00, 20.00, 15.00, 10.00
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM param_score_fournisseur LIMIT 1);

-- ---------------------------------------------------------------------------
-- Demandes de modification profil fournisseur
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demandes_modif_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_fournisseur BIGINT NOT NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
  motif TEXT NULL,
  commentaire_decision TEXT NULL,
  date_decision DATETIME NULL,
  decideur_id BIGINT NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_dmf_fournisseur (fk_fournisseur),
  KEY idx_dmf_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS champs_modif_fournisseur (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fk_demande_modif BIGINT NOT NULL,
  champ VARCHAR(100) NOT NULL,
  valeur_actuelle VARCHAR(500) NULL,
  valeur_proposee VARCHAR(500) NOT NULL,
  approuve TINYINT(1) NULL,
  PRIMARY KEY (id),
  KEY idx_cmf_demande (fk_demande_modif),
  CONSTRAINT fk_cmf_demande FOREIGN KEY (fk_demande_modif) REFERENCES demandes_modif_fournisseur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Mail log + notification routing
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cmd_mail_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  idempotence_key VARCHAR(100) NOT NULL,
  destinataire VARCHAR(255) NOT NULL,
  sujet VARCHAR(500) NOT NULL,
  corps TEXT NULL,
  statut VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  fk_invitation BIGINT NULL,
  fk_demande_cotation BIGINT NULL,
  attempts INT NOT NULL DEFAULT 0,
  last_error TEXT NULL,
  sent_at DATETIME NULL,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dateupdate DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cmd_mail_idempotence (idempotence_key),
  KEY idx_cmd_mail_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cmd_notification_routing (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_type VARCHAR(100) NOT NULL,
  canal VARCHAR(30) NOT NULL DEFAULT 'EMAIL',
  destinataire_role VARCHAR(100) NULL,
  template_key VARCHAR(100) NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  datecreate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cnr_event (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Permissions CMD_*
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO permissions (nom, description, datecreate) VALUES
('CMD_CONSULTER', 'Consulter le module commandes fournisseurs', NOW()),
('CMD_CREER_COTATION', 'Créer une demande de cotation', NOW()),
('CMD_ENVOYER_COTATION', 'Envoyer une cotation aux fournisseurs', NOW()),
('CMD_ANALYSER', 'Analyser / comparer les offres', NOW()),
('CMD_ATTRIBUER', 'Attribuer une cotation', NOW()),
('CMD_CREER_BC', 'Créer un bon de commande', NOW()),
('CMD_VALIDER_BC', 'Valider un bon de commande', NOW()),
('CMD_RECEPTIONNER', 'Réceptionner une livraison liée à un BC', NOW()),
('CMD_EVALUER', 'Évaluer un fournisseur', NOW()),
('CMD_APPROUVER_REOUVERTURE', 'Approuver une demande de réouverture d''offre', NOW()),
('CMD_APPROUVER_MODIF_FOURN', 'Approuver une modification profil fournisseur', NOW()),
('CMD_PARAMETRES', 'Gérer les paramètres score fournisseur', NOW()),
('CMD_AUDIT', 'Audit du module commandes', NOW());
