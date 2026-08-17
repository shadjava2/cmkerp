package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PharmacySummaryRowDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockMovementRowDTO;

@Repository
public class StockMovementAnalyticsRepository {

  private static final String ENTRIES_SUBQUERY = """
      SELECT
        la.fkStock,
        a.fkPharmacie,
        SUM(CASE
          WHEN a.datebonliv >= DATE_FORMAT(CURDATE(), '%%Y-%%m-01')
           AND a.datebonliv < CURDATE() + INTERVAL 1 DAY
          THEN la.qt ELSE 0 END
        ) AS entrees_mois_encours,
        SUM(CASE
          WHEN a.datebonliv >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%%Y-%%m-01')
           AND a.datebonliv < DATE_FORMAT(CURDATE(), '%%Y-%%m-01')
          THEN la.qt ELSE 0 END
        ) AS entrees_mois_precedent,
        MAX(a.datebonliv) AS date_derniere_entree
      FROM lignes_approv la
      INNER JOIN approvsionnements a ON la.fkApprov = a.id
      GROUP BY la.fkStock, a.fkPharmacie
      """;

  private static final String EXITS_SUBQUERY = """
      SELECT
        lts.fkStock,
        r.fkPharmacie,
        SUM(CASE
          WHEN ts.datecreate >= DATE_FORMAT(CURDATE(), '%%Y-%%m-01')
           AND ts.datecreate < CURDATE() + INTERVAL 1 DAY
          THEN lts.quantite ELSE 0 END
        ) AS sorties_mois_encours,
        SUM(CASE
          WHEN ts.datecreate >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%%Y-%%m-01')
           AND ts.datecreate < DATE_FORMAT(CURDATE(), '%%Y-%%m-01')
          THEN lts.quantite ELSE 0 END
        ) AS sorties_mois_precedent,
        MAX(DATE(ts.datecreate)) AS date_derniere_sortie
      FROM lignes_transferts_stock lts
      INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
      INNER JOIN requisitions r ON ts.fkRequisition = r.id
      WHERE ts.statut IN ('TRANSFEREE', 'RECEPTIONNEE')
      GROUP BY lts.fkStock, r.fkPharmacie
      """;

  private static final String BASE_FROM = """
      FROM stock_produits sp
      INNER JOIN pharmacies ph ON sp.fkPharmacies = ph.id
      INNER JOIN produits p ON sp.fkProduits = p.id
      LEFT JOIN formes f ON p.fkForme = f.id
      LEFT JOIN dosages d ON p.fkDosage = d.id
      LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
      LEFT JOIN (""" + ENTRIES_SUBQUERY + """
      ) e ON e.fkStock = sp.id AND e.fkPharmacie = ph.id
      LEFT JOIN (""" + EXITS_SUBQUERY + """
      ) s ON s.fkStock = sp.id AND s.fkPharmacie = ph.id
      WHERE UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'
        AND sp.operationnel = 1
      """ + CentralPharmacyExclusions.SQL_NOT_IN + """
      """;

  private static final String DETAIL_SQL = """
      SELECT
        sp.id AS id_stock,
        ph.id AS id_pharmacie,
        ph.designation AS pharmacie,
        p.nomcommercial AS nom_commercial,
        p.nomscientifique AS nom_scientifique,
        f.designation AS forme,
        d.designation AS dosage,
        c.designation AS conditionnement,
        sp.qte AS stock_actuel,
        p.qtcritique AS seuil_critique,
        COALESCE(e.entrees_mois_encours, 0) AS entrees_mois_encours,
        COALESCE(e.entrees_mois_precedent, 0) AS entrees_mois_precedent,
        COALESCE(s.sorties_mois_encours, 0) AS sorties_mois_encours,
        COALESCE(s.sorties_mois_precedent, 0) AS sorties_mois_precedent,
        e.date_derniere_entree,
        s.date_derniere_sortie
      """ + BASE_FROM;

  /**
   * Agrégation SQL par pharmacie (même règles métier que le service Java) — rapide pour l'aperçu.
   */
  private static final String SUMMARY_SELECT = """
      SELECT
        ph.id AS id_pharmacie,
        ph.designation AS pharmacie,
        SUM(CASE WHEN (
          COALESCE(e.entrees_mois_encours, 0) > 0
          OR COALESCE(s.sorties_mois_encours, 0) > 0
        ) THEN 1 ELSE 0 END) AS avec_mouvement,
        SUM(CASE WHEN (
          COALESCE(e.entrees_mois_encours, 0) = 0
          AND COALESCE(s.sorties_mois_encours, 0) = 0
          AND sp.qte > 0
          AND NOT (sp.qte <= 0 OR (p.qtcritique IS NOT NULL AND sp.qte <= p.qtcritique))
        ) THEN 1 ELSE 0 END) AS stock_sans_mouvement,
        SUM(CASE WHEN (
          COALESCE(e.entrees_mois_encours, 0) = 0
          AND COALESCE(s.sorties_mois_encours, 0) = 0
          AND (sp.qte <= 0 OR (p.qtcritique IS NOT NULL AND sp.qte <= p.qtcritique))
        ) THEN 1 ELSE 0 END) AS rupture_sans_mouvement,
        SUM(CASE WHEN (
          sp.qte <= 0 OR (p.qtcritique IS NOT NULL AND sp.qte <= p.qtcritique)
        ) THEN 1 ELSE 0 END) AS total_ruptures,
        COUNT(*) AS total_analyses
      """;

  private static final String SUMMARY_GROUP_ORDER =
      " GROUP BY ph.id, ph.designation ORDER BY ph.designation";

  private final NamedParameterJdbcTemplate jdbc;

  public StockMovementAnalyticsRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<PharmacySummaryRowDTO> findCentralPharmacySummaries(Long pharmacieId) {
    StringBuilder sql = new StringBuilder(SUMMARY_SELECT).append(BASE_FROM);
    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND ph.id = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }
    sql.append(SUMMARY_GROUP_ORDER);
    return jdbc.query(sql.toString(), params, summaryRowMapper());
  }

  public List<StockMovementRowDTO> findCentralStockMovements(Long pharmacieId) {
    StringBuilder sql = new StringBuilder(DETAIL_SQL);
    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND ph.id = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }
    sql.append(" ORDER BY ph.designation, p.nomcommercial");
    return jdbc.query(sql.toString(), params, detailRowMapper());
  }

  /** Recherche produit par mots-clés (nom commercial / scientifique) — chat WhatsApp. */
  public List<StockMovementRowDTO> searchProductsByKeywords(List<String> terms, int limit) {
    if (terms == null || terms.isEmpty()) {
      return List.of();
    }
    StringBuilder sql = new StringBuilder(DETAIL_SQL).append(" AND (");
    Map<String, Object> params = new HashMap<>();
    for (int i = 0; i < terms.size(); i++) {
      if (i > 0) {
        sql.append(" OR ");
      }
      String param = "term" + i;
      sql.append("(LOWER(p.nomcommercial) LIKE :").append(param)
          .append(" OR LOWER(p.nomscientifique) LIKE :").append(param)
          .append(" OR LOWER(CONCAT(IFNULL(p.nomcommercial,''), ' ', IFNULL(p.nomscientifique,''))) LIKE :")
          .append(param).append(")");
      params.put(param, "%" + terms.get(i).toLowerCase() + "%");
    }
    sql.append(") ORDER BY p.nomcommercial, ph.designation LIMIT :limit");
    params.put("limit", Math.min(Math.max(limit, 1), 30));
    return jdbc.query(sql.toString(), params, detailRowMapper());
  }

  private static RowMapper<PharmacySummaryRowDTO> summaryRowMapper() {
    return (rs, rowNum) -> new PharmacySummaryRowDTO(
        rs.getLong("id_pharmacie"),
        rs.getString("pharmacie"),
        rs.getInt("avec_mouvement"),
        rs.getInt("stock_sans_mouvement"),
        rs.getInt("rupture_sans_mouvement"),
        rs.getInt("total_ruptures"),
        rs.getInt("total_analyses"));
  }

  private static RowMapper<StockMovementRowDTO> detailRowMapper() {
    return (rs, rowNum) -> new StockMovementRowDTO(
        rs.getLong("id_stock"),
        rs.getLong("id_pharmacie"),
        rs.getString("pharmacie"),
        rs.getString("nom_commercial"),
        rs.getString("nom_scientifique"),
        rs.getString("forme"),
        rs.getString("dosage"),
        rs.getString("conditionnement"),
        toBigDecimal(rs.getObject("stock_actuel")),
        toBigDecimal(rs.getObject("seuil_critique")),
        toBigDecimal(rs.getObject("entrees_mois_encours")),
        toBigDecimal(rs.getObject("entrees_mois_precedent")),
        toBigDecimal(rs.getObject("sorties_mois_encours")),
        toBigDecimal(rs.getObject("sorties_mois_precedent")),
        toLocalDate(rs.getDate("date_derniere_entree")),
        toLocalDate(rs.getDate("date_derniere_sortie")));
  }

  private static BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    if (value instanceof BigDecimal bd) {
      return bd;
    }
    return new BigDecimal(value.toString());
  }

  private static LocalDate toLocalDate(Date date) {
    return date != null ? date.toLocalDate() : null;
  }
}
