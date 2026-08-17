package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.LigneApprov;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour les lignes d'approvisionnement.
 */
public interface LigneApprovRepository {

    /**
     * Trouve une ligne par son ID.
     */
    Optional<LigneApprov> findById(Long id);

    /**
     * Récupère toutes les lignes d'un approvisionnement.
     *
     * @param fkApprov l'ID de l'approvisionnement
     * @return la liste des lignes
     */
    List<LigneApprov> findByFkApprov(Long fkApprov);

    /**
     * Sauvegarde une nouvelle ligne.
     *
     * @param ligneApprov la ligne à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(LigneApprov ligneApprov);

    /**
     * Met à jour une ligne existante.
     *
     * @param ligneApprov la ligne à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(LigneApprov ligneApprov);

    /**
     * Supprime une ligne par son ID.
     *
     * @param id l'ID de la ligne
     * @return le nombre de lignes affectées
     */
    int deleteById(Long id);
}

