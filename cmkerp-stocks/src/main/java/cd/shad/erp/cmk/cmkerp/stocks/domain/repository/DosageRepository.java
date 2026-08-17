package cd.shad.erp.cmk.cmkerp.stocks.domain.repository;

import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Dosage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité Dosage.
 * Utilise Spring Data JPA pour les opérations CRUD de base.
 */
@Repository
public interface DosageRepository extends JpaRepository<Dosage, Long> {

    /**
     * Trouve tous les dosages triés par désignation.
     *
     * @return liste des dosages triés par désignation
     */
    List<Dosage> findAllByOrderByDesignationAsc();
}

