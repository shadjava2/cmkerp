package cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.Vente;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Vente.
 */
public interface VenteRepository {

    /**
     * Trouve une vente par son ID.
     */
    Optional<Vente> findById(Long id);

    /**
     * Récupère toutes les ventes avec pagination et filtres.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @param fkPharmacie filtre par pharmacie (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param fkPatient filtre par patient (optionnel)
     * @param dateFrom date de début pour le filtre par période (optionnel)
     * @param dateTo date de fin pour le filtre par période (optionnel)
     * @param searchText texte de recherche pour demandeur ou raisonsortie (optionnel, recherche partielle)
     * @return la liste des ventes
     */
    List<Vente> findAll(int offset, int limit, Long fkPharmacie, String statut, Long fkPatient, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText);

    /**
     * Compte le nombre total de ventes avec filtres.
     *
     * @param fkPharmacie filtre par pharmacie (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param fkPatient filtre par patient (optionnel)
     * @param dateFrom date de début pour le filtre par période (optionnel)
     * @param dateTo date de fin pour le filtre par période (optionnel)
     * @param searchText texte de recherche pour demandeur ou raisonsortie (optionnel, recherche partielle)
     * @return le nombre total de ventes
     */
    long count(Long fkPharmacie, String statut, Long fkPatient, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText);

    /**
     * Sauvegarde une nouvelle vente.
     *
     * @param vente la vente à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Vente vente);

    /**
     * Met à jour une vente existante.
     *
     * @param vente la vente à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Vente vente);
}

