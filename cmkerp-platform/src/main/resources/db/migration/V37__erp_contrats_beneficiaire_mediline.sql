-- Classification bénéficiaire ERP (hors schéma CLINIQUE / Mediline).
-- libelle_beneficiaire : libellé métier ERP
-- mediline_categorie_code : code réel t_categorie_patient (AFF, ATS, CASH, CONV…)

ALTER TABLE erp_contrats
  ADD COLUMN libelle_beneficiaire VARCHAR(75) NULL AFTER designation,
  ADD COLUMN mediline_categorie_code VARCHAR(5) NULL AFTER libelle_beneficiaire;

CREATE INDEX idx_erp_contrats_mediline_categorie
  ON erp_contrats (mediline_categorie_code);
