package cd.shad.erp.cmk.cmkerp.stocks.domain.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Produit;

/**
 * Repository pour l'entité Produit. Utilise Spring Data JPA avec des requêtes personnalisées pour
 * les JOINs MySQL 8.
 */
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

  /**
   * Trouve un produit par son code-barres.
   *
   * @param codebarre le code-barres
   * @return Optional contenant le produit s'il existe
   */
  Optional<Produit> findByCodebarre(String codebarre);

  /**
   * Vérifie si un produit existe avec le code-barres donné.
   *
   * @param codebarre le code-barres
   * @return true si un produit existe avec ce code-barres
   */
  boolean existsByCodebarre(String codebarre);

  /**
   * Trouve tous les produits avec pagination, triés par nom commercial.
   *
   * @param pageable paramètres de pagination
   * @return page de produits
   */
  Page<Produit> findAllByOrderByNomcommercialAsc(Pageable pageable);

  /**
   * Recherche des produits par nom commercial (contient, insensible à la casse).
   *
   * @param nomcommercial le nom commercial à rechercher
   * @param pageable paramètres de pagination
   * @return page de produits correspondants
   */
  Page<Produit> findByNomcommercialContainingIgnoreCase(String nomcommercial, Pageable pageable);

  /**
   * Trouve un produit par son ID avec toutes les relations (JOINs MySQL 8). Cette requête utilise
   * les JOINs pour récupérer les désignations des tables de référence en une seule requête, évitant
   * le problème N+1.
   *
   * <p>
   * Note: Cette méthode retourne le produit de base. Les désignations sont récupérées via une
   * requête SQL native optimisée dans ProduitQueryService.
   *
   * @param id l'ID du produit
   * @return Optional contenant le produit
   */
  Optional<Produit> findById(Long id);

  /**
   * Trouve tous les produits avec leurs relations (JOINs MySQL 8). Cette requête utilise les JOINs
   * pour récupérer les désignations des tables de référence en une seule requête, évitant le
   * problème N+1.
   *
   * @param pageable paramètres de pagination
   * @return page de produits avec leurs relations
   */
  @Query("""
      SELECT DISTINCT p FROM Produit p
      ORDER BY p.nomcommercial ASC
      """)
  Page<Produit> findAllWithRelations(Pageable pageable);
}

