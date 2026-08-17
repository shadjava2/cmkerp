package cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour les lignes de transfert interne.
 */
public interface LigneTransfertInterneRepository {

    /**
     * Trouve une ligne de transfert interne par son ID.
     */
    Optional<LigneTransfertInterne> findById(Long id);

    /**
     * Récupère toutes les lignes d'un transfert interne.
     *
     * @param fkTransfertInterne l'ID du transfert interne
     * @return la liste des lignes de transfert interne
     */
    List<LigneTransfertInterne> findByFkTransfertInterne(Long fkTransfertInterne);

    /**
     * Sauvegarde une nouvelle ligne de transfert interne.
     *
     * @param ligneTransfertInterne la ligne de transfert interne à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(LigneTransfertInterne ligneTransfertInterne);

    /**
     * Met à jour une ligne de transfert interne existante.
     *
     * @param ligneTransfertInterne la ligne de transfert interne à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(LigneTransfertInterne ligneTransfertInterne);

    /**
     * Supprime une ligne de transfert interne.
     *
     * @param id l'ID de la ligne de transfert interne à supprimer
     * @return le nombre de lignes affectées
     */
    int delete(Long id);
}

