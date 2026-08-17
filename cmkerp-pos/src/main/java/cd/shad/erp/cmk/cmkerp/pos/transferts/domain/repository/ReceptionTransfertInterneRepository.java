package cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.ReceptionTransfertInterne;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat ReceptionTransfertInterne (module POS).
 */
public interface ReceptionTransfertInterneRepository {

    /**
     * Trouve une réception de transfert interne par son ID.
     */
    Optional<ReceptionTransfertInterne> findById(Long id);

    /**
     * Trouve une réception de transfert interne par fkTransfertInterne.
     */
    Optional<ReceptionTransfertInterne> findByFkTransfertInterne(Long fkTransfertInterne);

    /**
     * Récupère toutes les réceptions de transferts internes avec pagination et filtres.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @param fkPharmacieDestination filtre par pharmacie destination (optionnel)
     * @param statut filtre par statut (optionnel)
     * @return la liste des réceptions de transferts internes
     */
    List<ReceptionTransfertInterne> findAll(int offset, int limit, Long fkPharmacieDestination, String statut);

    /**
     * Compte le nombre total de réceptions de transferts internes avec filtres.
     *
     * @param fkPharmacieDestination filtre par pharmacie destination (optionnel)
     * @param statut filtre par statut (optionnel)
     * @return le nombre total de réceptions de transferts internes
     */
    long count(Long fkPharmacieDestination, String statut);

    /**
     * Sauvegarde une nouvelle réception de transfert interne.
     *
     * @param receptionTransfertInterne la réception de transfert interne à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(ReceptionTransfertInterne receptionTransfertInterne);

    /**
     * Met à jour une réception de transfert interne existante.
     *
     * @param receptionTransfertInterne la réception de transfert interne à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(ReceptionTransfertInterne receptionTransfertInterne);
}

