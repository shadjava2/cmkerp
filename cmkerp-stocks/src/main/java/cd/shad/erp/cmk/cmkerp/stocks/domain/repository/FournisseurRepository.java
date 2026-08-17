package cd.shad.erp.cmk.cmkerp.stocks.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Fournisseur;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des fournisseurs.
 */
public interface FournisseurRepository {

    /**
     * Trouve un fournisseur par son ID.
     */
    Optional<Fournisseur> findById(Long id);

    /**
     * Trouve tous les fournisseurs avec pagination et recherche.
     */
    List<Fournisseur> findAll(int offset, int limit, String nom);

    /**
     * Compte le nombre total de fournisseurs correspondant aux critères.
     */
    long count(String nom);

    /**
     * Sauvegarde un nouveau fournisseur.
     * @return L'ID du fournisseur créé
     */
    Long save(Fournisseur fournisseur);

    /**
     * Met à jour un fournisseur existant.
     */
    void update(Fournisseur fournisseur);

    /**
     * Supprime un fournisseur par son ID.
     */
    void deleteById(Long id);
}

