package cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.Inventaire;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Inventaire.
 */
public interface InventaireRepository {

    /**
     * Trouve un inventaire par son ID.
     */
    Optional<Inventaire> findById(Long id);

    /**
     * Récupère tous les inventaires avec pagination et filtres.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @param fkPharmacie filtre par pharmacie (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param typeinventaire filtre par type d'inventaire (optionnel)
     * @param dateFrom date de début pour le filtre par période (optionnel)
     * @param dateTo date de fin pour le filtre par période (optionnel)
     * @param searchText texte de recherche pour commentaire (optionnel, recherche partielle)
     * @return la liste des inventaires
     */
    List<Inventaire> findAll(int offset, int limit, Long fkPharmacie, String statut, String typeinventaire, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText);

    /**
     * Compte le nombre total d'inventaires avec filtres.
     *
     * @param fkPharmacie filtre par pharmacie (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param typeinventaire filtre par type d'inventaire (optionnel)
     * @param dateFrom date de début pour le filtre par période (optionnel)
     * @param dateTo date de fin pour le filtre par période (optionnel)
     * @param searchText texte de recherche pour commentaire (optionnel, recherche partielle)
     * @return le nombre total d'inventaires
     */
    long count(Long fkPharmacie, String statut, String typeinventaire, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText);

    /**
     * Sauvegarde un nouvel inventaire.
     *
     * @param inventaire l'inventaire à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Inventaire inventaire);

    /**
     * Met à jour un inventaire existant.
     *
     * @param inventaire l'inventaire à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Inventaire inventaire);
}

