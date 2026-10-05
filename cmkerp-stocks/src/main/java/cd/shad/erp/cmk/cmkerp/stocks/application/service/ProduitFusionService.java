package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueProduit;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitFusionItemResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Fusion produits CMKERP ↔ CLINIQUE via {@code produits.CODECLINIQUE}.
 * Écrit uniquement dans cmkerp-v24prod. CLINIQUE reste en lecture seule.
 */
@Service
@Slf4j
public class ProduitFusionService {

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;
  private final CliniqueLookupRepository cliniqueLookupRepository;

  public ProduitFusionService(
      @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc,
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbc,
      CliniqueLookupRepository cliniqueLookupRepository) {
    this.jdbc = jdbc;
    this.namedJdbc = namedJdbc;
    this.cliniqueLookupRepository = cliniqueLookupRepository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listCmkerp(String query, String linkFilter, int page, int size) {
    int safeSize = Math.max(1, Math.min(size, 100));
    int safePage = Math.max(0, page);
    int offset = safePage * safeSize;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    java.util.ArrayList<Object> args = new java.util.ArrayList<>();

    if (query != null && !query.isBlank()) {
      where.append(
          " AND (LOWER(p.nomcommercial) LIKE ? OR LOWER(p.nomscientifique) LIKE ?"
              + " OR LOWER(IFNULL(p.codebarre,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.`CODECLINIQUE`,'')) LIKE ?)");
      String like = "%" + query.trim().toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if ("linked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND p.`CODECLINIQUE` IS NOT NULL AND TRIM(p.`CODECLINIQUE`) <> ''");
    } else if ("unlinked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND (p.`CODECLINIQUE` IS NULL OR TRIM(p.`CODECLINIQUE`) = '')");
    }

    Long total = jdbc.queryForObject(
        "SELECT COUNT(*) FROM produits p" + where,
        Long.class,
        args.toArray());

    // LIMIT/OFFSET en littéraux bornés (évite soucis PreparedStatement MySQL)
    String sql =
        "SELECT p.id, p.codebarre, p.nomcommercial, p.nomscientifique, p.prixachat,"
            + " p.`CODECLINIQUE` AS code_clinique"
            + " FROM produits p"
            + where
            + " ORDER BY p.nomcommercial ASC"
            + " LIMIT "
            + safeSize
            + " OFFSET "
            + offset;

    List<ProduitFusionItemResponse> items = jdbc.query(
        sql,
        (rs, rowNum) -> ProduitFusionItemResponse.builder()
            .id(rs.getLong("id"))
            .codebarre(rs.getString("codebarre"))
            .nomcommercial(rs.getString("nomcommercial"))
            .nomscientifique(rs.getString("nomscientifique"))
            .prixachat(rs.getBigDecimal("prixachat"))
            .codeClinique(rs.getString("code_clinique"))
            .build(),
        args.toArray());

    Map<String, Object> result = new HashMap<>();
    result.put("content", items);
    result.put("totalElements", total != null ? total : 0L);
    result.put("page", safePage);
    result.put("size", safeSize);
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listClinique(String query, int page, int size) {
    int safeSize = Math.max(1, Math.min(size, 100));
    int safePage = Math.max(0, page);
    int offset = safePage * safeSize;

    long total = cliniqueLookupRepository.countProduits(query);
    List<CliniqueProduit> items = cliniqueLookupRepository.searchProduits(query, offset, safeSize);

    Map<String, Object> result = new HashMap<>();
    result.put("content", items);
    result.put("totalElements", total);
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("available", cliniqueLookupRepository.isAvailable());
    return result;
  }

  /**
   * Attribue {@code CODECLINIQUE} au produit CMKERP. Si le code est déjà lié à un autre produit,
   * l'ancien lien est effacé (1 code CLINIQUE → 1 produit ERP).
   */
  @Transactional
  public ProduitFusionItemResponse link(Long produitId, String codeClinique, Long userId) {
    Integer exists = namedJdbc.queryForObject(
        "SELECT COUNT(*) FROM produits WHERE id = :id",
        Map.of("id", produitId),
        Integer.class);
    if (exists == null || exists == 0) {
      throw NotFoundException.entity("Produit", produitId);
    }

    String code = codeClinique == null || codeClinique.isBlank() ? null : codeClinique.trim();
    long uid = userId != null ? userId : 0L;

    if (code != null) {
      namedJdbc.update(
          """
          UPDATE produits
          SET `CODECLINIQUE` = NULL, dateupdate = NOW(), userupdatedid = :userId
          WHERE `CODECLINIQUE` = :code AND id <> :id
          """,
          Map.of("code", code, "id", produitId, "userId", uid));
    }

    Map<String, Object> params = new HashMap<>();
    params.put("id", produitId);
    params.put("code", code);
    params.put("userId", uid);
    namedJdbc.update(
        """
        UPDATE produits
        SET `CODECLINIQUE` = :code, dateupdate = NOW(), userupdatedid = :userId
        WHERE id = :id
        """,
        params);

    log.info("Produit {} lié à CODECLINIQUE={}", produitId, code);

    return jdbc.query(
            """
            SELECT id, codebarre, nomcommercial, nomscientifique, prixachat,
                   `CODECLINIQUE` AS code_clinique
            FROM produits WHERE id = ?
            """,
            (rs, rowNum) -> ProduitFusionItemResponse.builder()
                .id(rs.getLong("id"))
                .codebarre(rs.getString("codebarre"))
                .nomcommercial(rs.getString("nomcommercial"))
                .nomscientifique(rs.getString("nomscientifique"))
                .prixachat(rs.getBigDecimal("prixachat"))
                .codeClinique(rs.getString("code_clinique"))
                .build(),
            produitId)
        .get(0);
  }
}
