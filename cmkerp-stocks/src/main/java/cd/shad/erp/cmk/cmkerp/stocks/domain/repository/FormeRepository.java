package cd.shad.erp.cmk.cmkerp.stocks.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Forme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité Forme.
 * Utilise Spring Data JPA pour les opérations CRUD de base.
 */
@Repository
public interface FormeRepository extends JpaRepository<Forme, Long> {

    /**
     * Trouve toutes les formes triées par désignation.
     *
     * @return liste des formes triées par désignation
     */
    List<Forme> findAllByOrderByDesignationAsc();
}

