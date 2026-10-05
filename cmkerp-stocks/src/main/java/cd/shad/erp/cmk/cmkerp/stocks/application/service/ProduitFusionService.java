package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
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
 * Fusion produits CMKERP ↔ CLINIQUE via colonne produits.CODECLINIQUE.
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
    ensureCodeCliniqueColumn();

    int safeSize = Math.max(1, Math.min(size, 100));
    int safePage = Math.max(0, page);
    int offset = safePage * safeSize;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> args = new ArrayList<>();

    if (query != null && !query.isBlank()) {
      where.append(
          " AND (LOWER(p.nomcommercial) LIKE ? OR LOWER(p.nomscientifique) LIKE ?"
              + " OR LOWER(IFNULL(p.codebarre,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.CODECLINIQUE,'')) LIKE ?)");
      String like = "%" + query.trim().toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if ("linked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND p.CODECLINIQUE IS NOT NULL AND TRIM(p.CODECLINIQUE) <> ''");
    } else if ("unlinked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND (p.CODECLINIQUE IS NULL OR TRIM(p.CODECLINIQUE) = '')");
    }

    Long total = jdbc.queryForObject(
        "SELECT COUNT(*) FROM produits p" + where,
        Long.class,
        args.toArray());

    String sql =
        "SELECT p.id, p.codebarre, p.nomcommercial, p.nomscientifique, p.prixachat, p.CODECLINIQUE"
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
            .codeClinique(rs.getString("CODECLINIQUE"))
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

  @Transactional
  public ProduitFusionItemResponse link(Long produitId, String codeClinique, Long userId) {
    ensureCodeCliniqueColumn();

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
      Map<String, Object> clearParams = new HashMap<>();
      clearParams.put("code", code);
      clearParams.put("id", produitId);
      clearParams.put("userId", uid);
      namedJdbc.update(
          "UPDATE produits SET CODECLINIQUE = NULL, dateupdate = NOW(), userupdatedid = :userId"
              + " WHERE CODECLINIQUE = :code AND id <> :id",
          clearParams);
    }

    Map<String, Object> params = new HashMap<>();
    params.put("id", produitId);
    params.put("code", code);
    params.put("userId", uid);
    namedJdbc.update(
        "UPDATE produits SET CODECLINIQUE = :code, dateupdate = NOW(), userupdatedid = :userId WHERE id = :id",
        params);

    log.info("Produit {} lié à CODECLINIQUE={}", produitId, code);

    return jdbc.query(
            "SELECT id, codebarre, nomcommercial, nomscientifique, prixachat, CODECLINIQUE"
                + " FROM produits WHERE id = ?",
            (rs, rowNum) -> ProduitFusionItemResponse.builder()
                .id(rs.getLong("id"))
                .codebarre(rs.getString("codebarre"))
                .nomcommercial(rs.getString("nomcommercial"))
                .nomscientifique(rs.getString("nomscientifique"))
                .prixachat(rs.getBigDecimal("prixachat"))
                .codeClinique(rs.getString("CODECLINIQUE"))
                .build(),
            produitId)
        .get(0);
  }

  /**
   * Vérifie que la colonne existe. Ne crée rien automatiquement (prod).
   * Message d'erreur explicite si absente sur la base réellement connectée.
   */
  private void ensureCodeCliniqueColumn() {
    try {
      Integer count = jdbc.queryForObject(
          "SELECT COUNT(*) FROM information_schema.COLUMNS"
              + " WHERE TABLE_SCHEMA = DATABASE()"
              + " AND TABLE_NAME = 'produits'"
              + " AND UPPER(COLUMN_NAME) = 'CODECLINIQUE'",
          Integer.class);
      if (count == null || count == 0) {
        throw missingColumn();
      }
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (DataAccessException ex) {
      log.warn("Vérification CODECLINIQUE via information_schema: {}", ex.getMessage());
      try {
        jdbc.queryForList("SELECT CODECLINIQUE FROM produits LIMIT 1");
      } catch (DataAccessException probe) {
        throw missingColumn(probe);
      }
    }
  }

  private static IllegalStateException missingColumn() {
    return missingColumn(null);
  }

  private static IllegalStateException missingColumn(Throwable cause) {
    String msg =
        "Colonne produits.CODECLINIQUE introuvable sur la base MySQL connectée par l'API. "
            + "Exécuter sur cmkerp-v24prod : "
            + "ALTER TABLE produits ADD COLUMN CODECLINIQUE VARCHAR(255) NULL;";
    return cause == null ? new IllegalStateException(msg) : new IllegalStateException(msg, cause);
  }
}
