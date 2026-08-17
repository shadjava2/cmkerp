package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.Approvisionnement;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Approvisionnement.
 */
public interface ApprovisionnementRepository {

    /**
     * Trouve un approvisionnement par son ID.
     */
    Optional<Approvisionnement> findById(Long id);

    /**
     * Récupère tous les approvisionnements avec pagination et filtres.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @param fkPharmacie filtre par pharmacie (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param fkFournisseur filtre par fournisseur (optionnel)
     * @param dateFrom date de début pour le filtre par période (optionnel)
     * @param dateTo date de fin pour le filtre par période (optionnel)
     * @param searchText texte de recherche pour le numéro de bon (optionnel, recherche partielle)
     * @param produitQ nom de produit (optionnel) — bons contenant ce produit
     * @param produitId ID produit (optionnel) — bons contenant ce produit
     * @return la liste des approvisionnements
     */
    List<Approvisionnement> findAll(int offset, int limit, Long fkPharmacie, String statut, Long fkFournisseur, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText, String produitQ, Long produitId);

    /**
     * Compte le nombre total d'approvisionnements avec filtres.
     *
     * @param fkPharmacie filtre par pharmacie (optionnel)
     * @param statut filtre par statut (optionnel)
     * @param fkFournisseur filtre par fournisseur (optionnel)
     * @param dateFrom date de début pour le filtre par période (optionnel)
     * @param dateTo date de fin pour le filtre par période (optionnel)
     * @param searchText texte de recherche pour le numéro de bon (optionnel, recherche partielle)
     * @param produitQ nom de produit (optionnel) — bons contenant ce produit
     * @param produitId ID produit (optionnel) — bons contenant ce produit
     * @return le nombre total d'approvisionnements
     */
    long count(Long fkPharmacie, String statut, Long fkFournisseur, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText, String produitQ, Long produitId);

    /**
     * Sauvegarde un nouvel approvisionnement.
     *
     * @param approvisionnement l'approvisionnement à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Approvisionnement approvisionnement);

    /**
     * Met à jour un approvisionnement existant.
     *
     * @param approvisionnement l'approvisionnement à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Approvisionnement approvisionnement);

    /**
     * Recherche un approvisionnement par pharmacie et numéro de bon saisi (année courante).
     */
    Optional<Approvisionnement> findByPharmacieAndNumeroBon(
            Long fkPharmacie, String numeroSaisi, int year);
}

