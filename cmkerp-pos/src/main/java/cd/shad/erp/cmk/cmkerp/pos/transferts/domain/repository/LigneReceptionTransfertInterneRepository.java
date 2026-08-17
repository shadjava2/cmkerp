package cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneReceptionTransfertInterne;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour les lignes de réception de transfert interne (module POS).
 */
public interface LigneReceptionTransfertInterneRepository {

    /**
     * Trouve une ligne de réception de transfert interne par son ID.
     */
    Optional<LigneReceptionTransfertInterne> findById(Long id);

    /**
     * Récupère toutes les lignes d'une réception de transfert interne.
     *
     * @param fkReceptionTransfertInterne l'ID de la réception de transfert interne
     * @return la liste des lignes
     */
    List<LigneReceptionTransfertInterne> findByFkReceptionTransfertInterne(Long fkReceptionTransfertInterne);

    /**
     * Sauvegarde une nouvelle ligne de réception de transfert interne.
     *
     * @param ligneReceptionTransfertInterne la ligne à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(LigneReceptionTransfertInterne ligneReceptionTransfertInterne);

    /**
     * Met à jour une ligne de réception de transfert interne existante.
     *
     * @param ligneReceptionTransfertInterne la ligne à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(LigneReceptionTransfertInterne ligneReceptionTransfertInterne);

    /**
     * Supprime une ligne de réception de transfert interne.
     *
     * @param id l'ID de la ligne à supprimer
     * @return le nombre de lignes affectées
     */
    int delete(Long id);

    /**
     * Supprime toutes les lignes d'une réception de transfert interne.
     *
     * @param fkReceptionTransfertInterne l'ID de la réception de transfert interne
     * @return le nombre de lignes affectées
     */
    int deleteByFkReceptionTransfertInterne(Long fkReceptionTransfertInterne);
}

