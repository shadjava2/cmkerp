-- Permet plusieurs approvisionnements pour le même fournisseur/pharmacie/devise
-- (doublon géré par numéro de bon côté application).
-- Exécuter une seule fois sur cmkerp-v24prod.

ALTER TABLE approvsionnements DROP INDEX index_appro_unique;
