package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.sql.ResultSet;
import java.sql.SQLException;
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
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
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
    int safeSize = Math.max(1, Math.min(size, 100));
    int safePage = Math.max(0, page);
    int offset = safePage * safeSize;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> args = new ArrayList<>();

    if (query != null && !query.isBlank()) {
      where.append(
          " AND (LOWER(IFNULL(p.nomcommercial,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.nomscientifique,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.codebarre,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.CODECLINIQUE,'')) LIKE ?)");
      String like = "%" + query.trim().toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if ("linked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND p.CODECLINIQUE IS NOT NULL AND p.CODECLINIQUE <> ''");
    } else if ("unlinked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND (p.CODECLINIQUE IS NULL OR p.CODECLINIQUE = '')");
    }

    try {
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

      List<ProduitFusionItemResponse> items = jdbc.query(sql, this::mapErpRow, args.toArray());

      Map<String, Object> result = new HashMap<>();
      result.put("content", items);
      result.put("totalElements", total != null ? total : 0L);
      result.put("page", safePage);
      result.put("size", safeSize);
      return result;
    } catch (DataAccessException ex) {
      throw sqlFailure("listCmkerp", ex);
    }
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
    try {
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
            "UPDATE produits SET CODECLINIQUE = NULL, dateupdate = NOW(), userupdateid = :userId"
                + " WHERE CODECLINIQUE = :code AND id <> :id",
            clearParams);
      }

      Map<String, Object> params = new HashMap<>();
      params.put("id", produitId);
      params.put("code", code);
      params.put("userId", uid);
      namedJdbc.update(
          "UPDATE produits SET CODECLINIQUE = :code, dateupdate = NOW(), userupdateid = :userId WHERE id = :id",
          params);

      log.info("Produit {} lié à CODECLINIQUE={}", produitId, code);

      return jdbc.query(
              "SELECT id, codebarre, nomcommercial, nomscientifique, prixachat, CODECLINIQUE"
                  + " FROM produits WHERE id = ?",
              this::mapErpRow,
              produitId)
          .get(0);
    } catch (NotFoundException ex) {
      throw ex;
    } catch (DataAccessException ex) {
      throw sqlFailure("link", ex);
    }
  }

  private ProduitFusionItemResponse mapErpRow(ResultSet rs, int rowNum) throws SQLException {
    return ProduitFusionItemResponse.builder()
        .id(rs.getLong("id"))
        .codebarre(rs.getString("codebarre"))
        .nomcommercial(rs.getString("nomcommercial"))
        .nomscientifique(rs.getString("nomscientifique"))
        .prixachat(rs.getBigDecimal("prixachat"))
        .codeClinique(readCodeClinique(rs))
        .build();
  }

  private static String readCodeClinique(ResultSet rs) throws SQLException {
    try {
      return rs.getString("CODECLINIQUE");
    } catch (SQLException first) {
      try {
        return rs.getString("codeclinique");
      } catch (SQLException second) {
        return rs.getString(6);
      }
    }
  }

  private static BusinessException sqlFailure(String op, DataAccessException ex) {
    Throwable root = ex;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String detail = root.getMessage() != null ? root.getMessage() : ex.getMessage();
    log.error("Fusion ERP {} failed: {}", op, detail, ex);
    return new BusinessException(
        "Erreur SQL fusion ERP (" + op + "): " + detail
            + " — vérifier que la colonne produits.CODECLINIQUE existe sur la base du .env gateway.");
  }
}
