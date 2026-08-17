package cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.LigneInventaire;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour les lignes d'inventaire.
 */
public interface LigneInventaireRepository {

    /**
     * Trouve une ligne d'inventaire par son ID.
     */
    Optional<LigneInventaire> findById(Long id);

    /**
     * Récupère les lignes d'un inventaire.
     *
     * @param fkInventaire ID de l'inventaire
     * @return la liste des lignes d'inventaire
     */
    List<LigneInventaire> findByFkInventaire(Long fkInventaire);

    /**
     * Récupère les lignes d'un inventaire, éventuellement filtrées par statut opérationnel du stock.
     *
     * @param fkInventaire ID de l'inventaire
     * @param operationnel {@code true} = actifs, {@code false} = inactifs, {@code null} = tous
     */
    List<LigneInventaire> findByFkInventaire(Long fkInventaire, Boolean operationnel);

    /**
     * Met à jour une ligne d'inventaire existante.
     *
     * @param ligne la ligne à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(LigneInventaire ligne);
}

