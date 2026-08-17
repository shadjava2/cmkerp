package cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.LigneVente;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour les lignes de vente.
 */
public interface LigneVenteRepository {

    /**
     * Trouve une ligne de vente par son ID.
     */
    Optional<LigneVente> findById(Long id);

    /**
     * Récupère toutes les lignes d'une vente.
     *
     * @param fkVente l'ID de la vente
     * @return la liste des lignes de vente
     */
    List<LigneVente> findByFkVente(Long fkVente);

    /**
     * Sauvegarde une nouvelle ligne de vente.
     *
     * @param ligneVente la ligne de vente à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(LigneVente ligneVente);

    /**
     * Met à jour une ligne de vente existante.
     *
     * @param ligneVente la ligne de vente à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(LigneVente ligneVente);

    /**
     * Supprime une ligne de vente.
     *
     * @param id l'ID de la ligne de vente à supprimer
     * @return le nombre de lignes affectées
     */
    int delete(Long id);
}

