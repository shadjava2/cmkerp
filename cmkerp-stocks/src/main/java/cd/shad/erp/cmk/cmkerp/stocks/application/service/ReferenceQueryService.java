package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.CategorieProduitResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ConditionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.DosageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FormeResponse;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.CategorieProduitRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.ConditionnementRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.DosageRepository;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.FormeRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Service pour la gestion des références (formes, dosages, conditionnements, catégories).
 *
 * <p>
 * Ce service contient toutes les opérations de lecture (queries) liées aux tables de référence.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class ReferenceQueryService {

  private final FormeRepository formeRepository;
  private final DosageRepository dosageRepository;
  private final ConditionnementRepository conditionnementRepository;
  private final CategorieProduitRepository categorieProduitRepository;
  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  public ReferenceQueryService(FormeRepository formeRepository, DosageRepository dosageRepository,
      ConditionnementRepository conditionnementRepository,
      CategorieProduitRepository categorieProduitRepository,
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
    this.formeRepository = formeRepository;
    this.dosageRepository = dosageRepository;
    this.conditionnementRepository = conditionnementRepository;
    this.categorieProduitRepository = categorieProduitRepository;
    this.namedJdbcTemplate = namedJdbcTemplate;
  }

  /**
   * Récupère toutes les formes triées par désignation. Cache activé car les données de référence
   * changent rarement.
   */
  @Cacheable(value = "formes", unless = "#result.isEmpty()")
  public List<FormeResponse> findAllFormes() {
    log.debug("Récupération de toutes les formes (cache activé)");
    return formeRepository.findAllByOrderByDesignationAsc().stream().map(forme -> FormeResponse
        .builder().id(forme.getId()).designation(forme.getDesignation()).build()).toList();
  }

  /**
   * Récupère tous les dosages triés par désignation. Cache activé car les données de référence
   * changent rarement.
   */
  @Cacheable(value = "dosages", unless = "#result.isEmpty()")
  public List<DosageResponse> findAllDosages() {
    log.debug("Récupération de tous les dosages (cache activé)");
    return dosageRepository.findAllByOrderByDesignationAsc().stream().map(dosage -> DosageResponse
        .builder().id(dosage.getId()).designation(dosage.getDesignation()).build()).toList();
  }

  /**
   * Récupère tous les conditionnements triés par désignation. Cache activé car les données de
   * référence changent rarement.
   */
  @Cacheable(value = "conditionnements", unless = "#result.isEmpty()")
  public List<ConditionnementResponse> findAllConditionnements() {
    log.debug("Récupération de tous les conditionnements (cache activé)");
    return conditionnementRepository.findAllByOrderByDesignationAsc().stream()
        .map(conditionnement -> ConditionnementResponse.builder().id(conditionnement.getId())
            .designation(conditionnement.getDesignation()).build())
        .toList();
  }

  /**
   * Récupère toutes les catégories de produits triées par désignation. Cache activé car les données
   * de référence changent rarement.
   */
  @Cacheable(value = "categorieProduits", unless = "#result.isEmpty()")
  public List<CategorieProduitResponse> findAllCategorieProduits() {
    log.debug("Récupération de toutes les catégories de produits (cache activé)");
    return categorieProduitRepository.findAllByOrderByDesignationAsc().stream()
        .map(categorie -> CategorieProduitResponse.builder().id(categorie.getId())
            .designation(categorie.getDesignation()).abbreviation(categorie.getAbbreviation())
            .build())
        .toList();
  }

  /**
   * Récupère les catégories de produits auxquelles une pharmacie a droit. Utilise une requête SQL
   * optimisée avec JOIN sur droits_categorie. Cache activé par pharmacie car les droits changent
   * rarement.
   *
   * @param pharmacieId ID de la pharmacie
   * @return Liste des catégories triées par désignation
   */
  @Cacheable(value = "categorieProduitsByPharmacie", key = "#pharmacieId",
      unless = "#result.isEmpty()")
  public List<CategorieProduitResponse> findCategorieProduitsByPharmacie(Long pharmacieId) {
    log.debug("Récupération des catégories de produits pour pharmacieId: {} (cache activé)",
        pharmacieId);

    String sql = """
        SELECT c.id, c.designation, c.abbreviation
        FROM categorie_produit c
        INNER JOIN droits_categorie d ON d.fkCategorie = c.id
        WHERE d.fkPharmacie = :pharmacieId
        ORDER BY c.designation ASC
        """;

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);

    RowMapper<CategorieProduitResponse> mapper = (rs, rowNum) -> CategorieProduitResponse.builder()
        .id(rs.getLong("id")).designation(rs.getString("designation"))
        .abbreviation(getStringOrNull(rs, "abbreviation")).build();

    return namedJdbcTemplate.query(sql, params, mapper);
  }

  private static String getStringOrNull(ResultSet rs, String column) throws SQLException {
    String value = rs.getString(column);
    return rs.wasNull() ? null : value;
  }
}

