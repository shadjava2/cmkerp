package cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniquePrixInfo;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueProduit;

/**
 * JDBC CLINIQUE — SELECT + UPDATE ciblé de {@code TSTOCK.PAU}. Jamais de DELETE / DROP / DDL.
 */
@Repository
@ConditionalOnBean(name = "cliniqueDataSource")
public class CliniqueLookupJdbcRepository implements CliniqueLookupRepository {

  private static final Logger log = LoggerFactory.getLogger(CliniqueLookupJdbcRepository.class);

  private static final RowMapper<CliniqueProduit> PRODUIT_MAPPER = (rs, rowNum) -> {
    Double pau = null;
    try {
      double v = rs.getDouble("PAU");
      if (!rs.wasNull()) {
        pau = v;
      }
    } catch (Exception ignored) {
      // colonne absente / alias
    }
    return new CliniqueProduit(
        trim(rs.getString("CODE")),
        trim(rs.getString("LIBELLE")),
        trim(rs.getString("FORME")),
        trim(rs.getString("DOSAGE")),
        trim(rs.getString("DESIGNATION")),
        pau);
  };

  private static final RowMapper<CliniquePrixInfo> PRIX_MAPPER = (rs, rowNum) -> {
    Double pau = null;
    Double stock = null;
    try {
      double v = rs.getDouble("PAU");
      if (!rs.wasNull()) {
        pau = v;
      }
    } catch (Exception ignored) {
      // ignore
    }
    try {
      double v = rs.getDouble("STINV");
      if (!rs.wasNull()) {
        stock = v;
      }
    } catch (Exception ignored) {
      // ignore
    }
    return new CliniquePrixInfo(
        trim(rs.getString("CODE")),
        trim(rs.getString("DESIGNATION")),
        trim(rs.getString("FORME")),
        trim(rs.getString("DOSAGE")),
        pau,
        stock);
  };

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private final JdbcTemplate jdbcTemplate;

  public CliniqueLookupJdbcRepository(@Qualifier("cliniqueJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean isAvailable() {
    try {
      Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      return one != null && one == 1;
    } catch (DataAccessException ex) {
      log.warn("CLINIQUE indisponible: {}", ex.getMessage());
      return false;
    }
  }

  @Override
  public long countMedicaments() {
    try {
      Long count = jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.TMEDICAMENT", Long.class);
      return count != null ? count : 0L;
    } catch (DataAccessException ex) {
      log.warn("countMedicaments CLINIQUE: {}", ex.getMessage());
      return 0L;
    }
  }

  @Override
  public List<Map<String, Object>> findMedicaments(int limit) {
    return selectTop("dbo.TMEDICAMENT", limit);
  }

  @Override
  public List<Map<String, Object>> findTarifs(int limit) {
    return selectTop("dbo.TARIF", limit);
  }

  @Override
  public List<CliniqueProduit> searchProduits(String query, int offset, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    int safeOffset = Math.max(0, offset);
    // JOIN robuste : nchar paddé → RTRIM, LEFT JOIN si stock manquant
    final String baseSelect =
        """
        SELECT p.CODE, p.LIBELLE, p.FORME, p.DOSAGE, p.DESIGNATION, s.PAU
        FROM dbo.TPRODUIT p
        LEFT JOIN dbo.TSTOCK s ON RTRIM(s.CODE) = RTRIM(p.CODE)
        """;
    try {
      if (query == null || query.isBlank()) {
        return jdbcTemplate.query(
            baseSelect
                + """
                 ORDER BY COALESCE(NULLIF(RTRIM(p.DESIGNATION), ''), RTRIM(p.LIBELLE), RTRIM(p.CODE))
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """,
            PRODUIT_MAPPER,
            safeOffset,
            safeLimit);
      }
      String like = "%" + query.trim() + "%";
      return jdbcTemplate.query(
          baseSelect
              + """
               WHERE RTRIM(p.CODE) LIKE ?
                  OR p.LIBELLE LIKE ?
                  OR p.DESIGNATION LIKE ?
                  OR p.FORME LIKE ?
                  OR p.DOSAGE LIKE ?
                  OR s.DESIGNATION LIKE ?
               ORDER BY COALESCE(NULLIF(RTRIM(p.DESIGNATION), ''), RTRIM(p.LIBELLE), RTRIM(p.CODE))
               OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
              """,
          PRODUIT_MAPPER,
          like,
          like,
          like,
          like,
          like,
          like,
          safeOffset,
          safeLimit);
    } catch (DataAccessException ex) {
      log.warn("searchProduits CLINIQUE (avec TSTOCK): {}", ex.getMessage());
      // Fallback sans TSTOCK si la jointure échoue
      return searchProduitsFallback(query, safeOffset, safeLimit);
    }
  }

  private List<CliniqueProduit> searchProduitsFallback(String query, int offset, int limit) {
    try {
      if (query == null || query.isBlank()) {
        return jdbcTemplate.query(
            """
            SELECT CODE, LIBELLE, FORME, DOSAGE, DESIGNATION, CAST(NULL AS float) AS PAU
            FROM dbo.TPRODUIT
            ORDER BY LIBELLE
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """,
            PRODUIT_MAPPER,
            offset,
            limit);
      }
      String like = "%" + query.trim() + "%";
      return jdbcTemplate.query(
          """
          SELECT CODE, LIBELLE, FORME, DOSAGE, DESIGNATION, CAST(NULL AS float) AS PAU
          FROM dbo.TPRODUIT
          WHERE CODE LIKE ? OR LIBELLE LIKE ? OR DESIGNATION LIKE ?
          ORDER BY LIBELLE
          OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
          """,
          PRODUIT_MAPPER,
          like,
          like,
          like,
          offset,
          limit);
    } catch (DataAccessException ex) {
      log.warn("searchProduits CLINIQUE fallback: {}", ex.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public long countProduits(String query) {
    try {
      if (query == null || query.isBlank()) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.TPRODUIT", Long.class);
        return count != null ? count : 0L;
      }
      String like = "%" + query.trim() + "%";
      Long count = jdbcTemplate.queryForObject(
          """
          SELECT COUNT_BIG(*)
          FROM dbo.TPRODUIT p
          LEFT JOIN dbo.TSTOCK s ON RTRIM(s.CODE) = RTRIM(p.CODE)
          WHERE RTRIM(p.CODE) LIKE ?
             OR p.LIBELLE LIKE ?
             OR p.DESIGNATION LIKE ?
             OR p.FORME LIKE ?
             OR p.DOSAGE LIKE ?
             OR s.DESIGNATION LIKE ?
          """,
          Long.class,
          like,
          like,
          like,
          like,
          like,
          like);
      return count != null ? count : 0L;
    } catch (DataAccessException ex) {
      log.warn("countProduits CLINIQUE: {}", ex.getMessage());
      try {
        if (query == null || query.isBlank()) {
          Long count = jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.TPRODUIT", Long.class);
          return count != null ? count : 0L;
        }
        String like = "%" + query.trim() + "%";
        Long count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT_BIG(*) FROM dbo.TPRODUIT
            WHERE CODE LIKE ? OR LIBELLE LIKE ? OR DESIGNATION LIKE ?
            """,
            Long.class,
            like,
            like,
            like);
        return count != null ? count : 0L;
      } catch (DataAccessException ex2) {
        return 0L;
      }
    }
  }

  @Override
  public List<CliniquePrixInfo> findPrixByCodes(List<String> codes) {
    if (codes == null || codes.isEmpty()) {
      return List.of();
    }
    List<CliniquePrixInfo> out = new ArrayList<>();
    final int chunkSize = 80;
    for (int i = 0; i < codes.size(); i += chunkSize) {
      List<String> chunk = codes.subList(i, Math.min(i + chunkSize, codes.size())).stream()
          .filter(c -> c != null && !c.isBlank())
          .map(String::trim)
          .distinct()
          .toList();
      if (chunk.isEmpty()) {
        continue;
      }
      String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
      try {
        out.addAll(jdbcTemplate.query(
            """
            SELECT p.CODE, p.DESIGNATION, p.FORME, p.DOSAGE, s.PAU, s.STINV
            FROM dbo.TPRODUIT p
            LEFT JOIN dbo.TSTOCK s ON RTRIM(s.CODE) = RTRIM(p.CODE)
            WHERE RTRIM(p.CODE) IN ("""
                + placeholders
                + ")",
            PRIX_MAPPER,
            chunk.toArray()));
      } catch (DataAccessException ex) {
        log.warn("findPrixByCodes CLINIQUE: {}", ex.getMessage());
      }
    }
    return out;
  }

  @Override
  public boolean updatePau(String code, double pau) {
    if (code == null || code.isBlank()) {
      return false;
    }
    String trimmed = code.trim();
    try {
      int updated = jdbcTemplate.update(
          "UPDATE dbo.TSTOCK SET PAU = ? WHERE RTRIM(CODE) = ?",
          pau,
          trimmed);
      if (updated > 0) {
        return true;
      }
      String padded = trimmed.length() >= 10 ? trimmed.substring(0, 10) : String.format("%-10s", trimmed);
      int inserted = jdbcTemplate.update(
          "INSERT INTO dbo.TSTOCK (CODE, PAU) VALUES (?, ?)",
          padded,
          pau);
      return inserted > 0;
    } catch (DataAccessException ex) {
      log.warn("updatePau CLINIQUE code={}: {}", trimmed, ex.getMessage());
      return false;
    }
  }

  private List<Map<String, Object>> selectTop(String table, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    try {
      String sql = "SELECT TOP (" + safeLimit + ") * FROM " + table;
      return jdbcTemplate.queryForList(sql);
    } catch (DataAccessException ex) {
      log.warn("Lecture {} CLINIQUE: {}", table, ex.getMessage());
      return Collections.emptyList();
    }
  }
}
