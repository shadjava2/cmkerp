package cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat TransfertInterne.
 */
public interface TransfertInterneRepository {

    /**
     * Trouve un transfert interne par son ID.
     */
    Optional<TransfertInterne> findById(Long id);

    /**
     * Récupère tous les transferts internes avec pagination et filtres.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @param fkPharmacieSource filtre par pharmacie source (optionnel)
     * @param fkPharmacieDestination filtre par pharmacie destination (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param searchText texte de recherche pour commentaire (optionnel, recherche partielle)
     * @return la liste des transferts internes
     */
    List<TransfertInterne> findAll(int offset, int limit, Long fkPharmacieSource, Long fkPharmacieDestination, String statut, String searchText);

    /**
     * Compte le nombre total de transferts internes avec filtres.
     *
     * @param fkPharmacieSource filtre par pharmacie source (optionnel)
     * @param fkPharmacieDestination filtre par pharmacie destination (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param searchText texte de recherche pour commentaire (optionnel, recherche partielle)
     * @return le nombre total de transferts internes
     */
    long count(Long fkPharmacieSource, Long fkPharmacieDestination, String statut, String searchText);

    /**
     * Sauvegarde un nouveau transfert interne.
     *
     * @param transfertInterne le transfert interne à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(TransfertInterne transfertInterne);

    /**
     * Met à jour un transfert interne existant.
     *
     * @param transfertInterne le transfert interne à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(TransfertInterne transfertInterne);
}

