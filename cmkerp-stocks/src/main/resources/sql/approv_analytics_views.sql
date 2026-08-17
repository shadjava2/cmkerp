-- Vues analytiques approvisionnements (lecture seule — pilotage décisionnel CMK)
-- Orthographe legacy : table approvsionnements

CREATE OR REPLACE VIEW vw_approvisionnement_global AS
SELECT
  a.id,
  COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
  a.statut,
  a.fkFournisseur AS fournisseur_id,
  f.nom AS fournisseur,
  a.fkPharmacie AS pharmacie_id,
  ph.designation AS pharmacie,
  ph.typepharmacie AS type_pharmacie,
  a.datebonliv AS date_bon_livraison,
  DATE(a.datecreate) AS date_encodage,
  a.numbonliv,
  a.taux,
  a.datecreate,
  a.dateupdate,
  a.usercreateid,
  a.userupdateid,
  CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
  uc.username AS encodeur_username,
  COALESCE(agg.lignes_count, 0) AS lignes_count,
  COALESCE(agg.produits_distinct, 0) AS produits_distinct,
  COALESCE(agg.quantite_totale, 0) AS quantite_totale,
  COALESCE(agg.montant_total, 0) AS montant_total
FROM approvsionnements a
INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
LEFT JOIN utilisateurs uc ON a.usercreateid = uc.id
LEFT JOIN (
  SELECT
    la.fkApprov,
    COUNT(*) AS lignes_count,
    COUNT(DISTINCT la.fkStock) AS produits_distinct,
    COALESCE(SUM(la.qt), 0) AS quantite_totale,
    COALESCE(SUM(COALESCE(la.prixachattotal, la.qt * la.prixachat, 0)), 0) AS montant_total
  FROM lignes_approv la
  GROUP BY la.fkApprov
) agg ON agg.fkApprov = a.id;
