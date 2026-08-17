package cd.shad.erp.cmk.cmkerp.stocks.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Conditionnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité Conditionnement.
 * Utilise Spring Data JPA pour les opérations CRUD de base.
 */
@Repository
public interface ConditionnementRepository extends JpaRepository<Conditionnement, Long> {

    /**
     * Trouve tous les conditionnements triés par désignation.
     *
     * @return liste des conditionnements triés par désignation
     */
    List<Conditionnement> findAllByOrderByDesignationAsc();
}

