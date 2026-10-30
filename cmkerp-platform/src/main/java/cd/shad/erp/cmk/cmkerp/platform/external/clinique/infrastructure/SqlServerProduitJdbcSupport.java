package cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniquePrixInfo;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueProduit;

/**
 * Accès JDBC partagé TPRODUIT / TSTOCK pour Beach (CLINIQUE) et CH (même schéma).
 */
public final class SqlServerProduitJdbcSupport {

  private static final Logger log = LoggerFactory.getLogger(SqlServerProduitJdbcSupport.class);

  private static final RowMapper<CliniqueProduit> PRODUIT_MAPPER = (rs, rowNum) -> {
    Double pau = null;
    try {
      double v = rs.getDouble("PAU");
      if (!rs.wasNull()) {
        pau = v;
      }
    } catch (Exception ignored) {
      // ignore
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

  private final JdbcTemplate jdbc;
  private final String label;

  public SqlServerProduitJdbcSupport(JdbcTemplate jdbc, String label) {
    this.jdbc = jdbc;
    this.label = label;
  }

  public boolean isAvailable() {
    try {
      Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
      return one != null && one == 1;
    } catch (DataAccessException ex) {
      log.warn("{} indisponible: {}", label, ex.getMessage());
      return false;
    }
  }

  public List<CliniqueProduit> searchProduits(String query, int offset, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    int safeOffset = Math.max(0, offset);
    final String baseSelect =
        """
        SELECT p.CODE, p.LIBELLE, p.FORME, p.DOSAGE, p.DESIGNATION, s.PAU
        FROM dbo.TPRODUIT p
        LEFT JOIN dbo.TSTOCK s ON RTRIM(s.CODE) = RTRIM(p.CODE)
        """;
    try {
      if (query == null || query.isBlank()) {
        return jdbc.query(
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
      return jdbc.query(
          baseSelect
              + """
               WHERE RTRIM(p.CODE) LIKE ?
                  OR p.LIBELLE LIKE ?
                  OR p.DESIGNATION LIKE ?
                  OR p.FORME LIKE ?
                  OR p.DOSAGE LIKE ?
               ORDER BY COALESCE(NULLIF(RTRIM(p.DESIGNATION), ''), RTRIM(p.LIBELLE), RTRIM(p.CODE))
               OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
              """,
          PRODUIT_MAPPER,
          like,
          like,
          like,
          like,
          like,
          safeOffset,
          safeLimit);
    } catch (DataAccessException ex) {
      log.warn("searchProduits {}: {}", label, ex.getMessage());
      return Collections.emptyList();
    }
  }

  public long countProduits(String query) {
    try {
      if (query == null || query.isBlank()) {
        Long count = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.TPRODUIT", Long.class);
        return count != null ? count : 0L;
      }
      String like = "%" + query.trim() + "%";
      Long count = jdbc.queryForObject(
          """
          SELECT COUNT_BIG(*) FROM dbo.TPRODUIT p
          WHERE RTRIM(p.CODE) LIKE ? OR p.LIBELLE LIKE ? OR p.DESIGNATION LIKE ?
          """,
          Long.class,
          like,
          like,
          like);
      return count != null ? count : 0L;
    } catch (DataAccessException ex) {
      log.warn("countProduits {}: {}", label, ex.getMessage());
      return 0L;
    }
  }

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
        out.addAll(jdbc.query(
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
        log.warn("findPrixByCodes {}: {}", label, ex.getMessage());
      }
    }
    return out;
  }

  public boolean updatePau(String code, double pau) {
    if (code == null || code.isBlank()) {
      return false;
    }
    String trimmed = code.trim();
    try {
      int updated = jdbc.update(
          "UPDATE dbo.TSTOCK SET PAU = ? WHERE RTRIM(CODE) = ?",
          pau,
          trimmed);
      if (updated > 0) {
        return true;
      }
      String padded = padCode(trimmed);
      int inserted = jdbc.update(
          "INSERT INTO dbo.TSTOCK (CODE, PAU) VALUES (?, ?)",
          padded,
          pau);
      return inserted > 0;
    } catch (DataAccessException ex) {
      log.warn("updatePau {} code={}: {}", label, trimmed, ex.getMessage());
      return false;
    }
  }

  public boolean existsProduit(String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    try {
      Integer n = jdbc.queryForObject(
          "SELECT COUNT(*) FROM dbo.TPRODUIT WHERE RTRIM(CODE) = ?",
          Integer.class,
          code.trim());
      return n != null && n > 0;
    } catch (DataAccessException ex) {
      return false;
    }
  }

  /** Charge une fiche TPRODUIT + PAU / STINV pour copie inter-serveurs. */
  public Map<String, Object> loadProduitForCopy(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    try {
      List<Map<String, Object>> rows = jdbc.query(
          """
          SELECT RTRIM(p.CODE) AS CODE, p.LIBELLE, p.FORME, p.DOSAGE, p.DESIGNATION,
                 s.PAU, s.STINV, s.DESIGNATION AS STOCK_DESIGNATION
          FROM dbo.TPRODUIT p
          LEFT JOIN dbo.TSTOCK s ON RTRIM(s.CODE) = RTRIM(p.CODE)
          WHERE RTRIM(p.CODE) = ?
          """,
          (rs, i) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("CODE", trim(rs.getString("CODE")));
            m.put("LIBELLE", rs.getString("LIBELLE"));
            m.put("FORME", rs.getString("FORME"));
            m.put("DOSAGE", rs.getString("DOSAGE"));
            m.put("DESIGNATION", rs.getString("DESIGNATION"));
            double pau = rs.getDouble("PAU");
            m.put("PAU", rs.wasNull() ? null : pau);
            double st = rs.getDouble("STINV");
            m.put("STINV", rs.wasNull() ? null : st);
            m.put("STOCK_DESIGNATION", rs.getString("STOCK_DESIGNATION"));
            return m;
          },
          code.trim());
      return rows.isEmpty() ? null : rows.get(0);
    } catch (DataAccessException ex) {
      log.warn("loadProduitForCopy {} {}: {}", label, code, ex.getMessage());
      return null;
    }
  }

  /**
   * Insère TPRODUIT + TSTOCK si absents (mêmes colonnes métier utilisées par la fusion).
   *
   * @return true si produit créé ou déjà présent avec stock synchronisé
   */
  public boolean upsertProduitFromRow(Map<String, Object> row) {
    if (row == null || row.get("CODE") == null) {
      return false;
    }
    String code = String.valueOf(row.get("CODE")).trim();
    if (code.isBlank()) {
      return false;
    }
    String padded = padCode(code);
    try {
      if (!existsProduit(code)) {
        jdbc.update(
            """
            INSERT INTO dbo.TPRODUIT (CODE, LIBELLE, FORME, DOSAGE, DESIGNATION)
            VALUES (?, ?, ?, ?, ?)
            """,
            padded,
            row.get("LIBELLE"),
            row.get("FORME"),
            row.get("DOSAGE"),
            row.get("DESIGNATION"));
      }
      Object pauObj = row.get("PAU");
      Double pau = pauObj instanceof Number n ? n.doubleValue() : null;
      Object stObj = row.get("STINV");
      Double stinv = stObj instanceof Number n ? n.doubleValue() : null;
      String stockDes =
          row.get("STOCK_DESIGNATION") != null
              ? String.valueOf(row.get("STOCK_DESIGNATION"))
              : (row.get("DESIGNATION") != null ? String.valueOf(row.get("DESIGNATION")) : null);

      Integer stockExists = jdbc.queryForObject(
          "SELECT COUNT(*) FROM dbo.TSTOCK WHERE RTRIM(CODE) = ?",
          Integer.class,
          code);
      if (stockExists != null && stockExists > 0) {
        if (pau != null) {
          jdbc.update("UPDATE dbo.TSTOCK SET PAU = ? WHERE RTRIM(CODE) = ?", pau, code);
        }
      } else {
        try {
          jdbc.update(
              "INSERT INTO dbo.TSTOCK (CODE, PAU, STINV, DESIGNATION) VALUES (?, ?, ?, ?)",
              padded,
              pau != null ? pau : 0d,
              stinv != null ? stinv : 0d,
              stockDes);
        } catch (DataAccessException ex) {
          jdbc.update(
              "INSERT INTO dbo.TSTOCK (CODE, PAU) VALUES (?, ?)",
              padded,
              pau != null ? pau : 0d);
        }
      }
      return true;
    } catch (DataAccessException ex) {
      log.warn("upsertProduitFromRow {} {}: {}", label, code, ex.getMessage());
      return false;
    }
  }

  /** Tous les codes TPRODUIT (pour comparaison inter-serveurs, plafonné). */
  public List<String> listAllCodes(int max) {
    int safe = Math.max(1, Math.min(max, 50_000));
    try {
      return jdbc.query(
          """
          SELECT TOP (?) RTRIM(CODE) AS CODE FROM dbo.TPRODUIT
          ORDER BY CODE
          """,
          (rs, i) -> trim(rs.getString("CODE")),
          safe);
    } catch (DataAccessException ex) {
      log.warn("listAllCodes {}: {}", label, ex.getMessage());
      return List.of();
    }
  }

  public List<CliniquePrixInfo> listAllPrix(int max) {
    int safe = Math.max(1, Math.min(max, 50_000));
    try {
      return jdbc.query(
          """
          SELECT TOP (?) p.CODE, p.DESIGNATION, p.FORME, p.DOSAGE, s.PAU, s.STINV
          FROM dbo.TPRODUIT p
          LEFT JOIN dbo.TSTOCK s ON RTRIM(s.CODE) = RTRIM(p.CODE)
          ORDER BY p.CODE
          """,
          PRIX_MAPPER,
          safe);
    } catch (DataAccessException ex) {
      log.warn("listAllPrix {}: {}", label, ex.getMessage());
      return List.of();
    }
  }

  private static String padCode(String trimmed) {
    if (trimmed.length() >= 10) {
      return trimmed.substring(0, 10);
    }
    return String.format("%-10s", trimmed);
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }
}
