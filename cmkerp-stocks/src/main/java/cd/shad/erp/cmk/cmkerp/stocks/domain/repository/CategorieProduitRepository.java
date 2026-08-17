package cd.shad.erp.cmk.cmkerp.stocks.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.domain.model.CategorieProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité CategorieProduit.
 * Utilise Spring Data JPA pour les opérations CRUD de base.
 */
@Repository
public interface CategorieProduitRepository extends JpaRepository<CategorieProduit, Long> {

    /**
     * Trouve toutes les catégories triées par désignation.
     *
     * @return liste des catégories triées par désignation
     */
    List<CategorieProduit> findAllByOrderByDesignationAsc();
}

