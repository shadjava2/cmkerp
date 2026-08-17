package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertLineDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertQualityFlagsDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.TransfertSearchCriteria;

@Repository
public class TransfertAnalyticsRepository {

  private static final String DATE_OPERATION = "DATE(ts.datecreate)";

  private static final String AGG_JOIN = """
      LEFT JOIN (
        SELECT lts.fkTransfertStock,
          COUNT(*) AS lignes_count,
          COUNT(DISTINCT lts.fkStock) AS produits_distinct,
          COALESCE(SUM(COALESCE(lts.quantite, 0)), 0) AS quantite_totale
        FROM lignes_transferts_stock lts
        GROUP BY lts.fkTransfertStock
      ) agg ON agg.fkTransfertStock = ts.id
      """;

  private static final String FROM_BASE = """
      FROM transferts_stock ts
      INNER JOIN requisitions r ON ts.fkRequisition = r.id
      INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
      INNER JOIN pharmacies ph_dst ON r.fkPharmacie = ph_dst.id
      LEFT JOIN utilisateurs uc ON ts.usercreateid = uc.id
      """ + AGG_JOIN + "\nWHERE 1=1\n";

  private final NamedParameterJdbcTemplate jdbc;

  public TransfertAnalyticsRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public TransfertKpiDTO computeKpis(TransfertSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = """
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN ts.statut = 'TRANSFEREE' THEN 1 ELSE 0 END) AS transferes,
          SUM(CASE WHEN ts.statut = 'RECEPTIONNEE' THEN 1 ELSE 0 END) AS receptionnes,
          SUM(CASE WHEN ts.statut IN ('TRANSFEREE', 'RECEPTIONNEE') THEN 1 ELSE 0 END) AS sorties_validees,
          SUM(CASE WHEN ts.statut = 'ANNULEE' THEN 1 ELSE 0 END) AS annules,
          SUM(CASE WHEN ts.statut = 'EN ATTENTE' THEN 1 ELSE 0 END) AS en_attente,
          COUNT(DISTINCT r.fkPharmacieStock) AS pharmacies_source,
          COUNT(DISTINCT r.fkPharmacie) AS pharmacies_destination,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite_total,
          COALESCE(AVG(agg.quantite_totale), 0) AS quantite_moyenne,
        """
        + "MAX(" + DATE_OPERATION + ") AS dernier,\n"
        + "MIN(" + DATE_OPERATION + ") AS premier\n"
        + FROM_BASE + where;
    return jdbc.queryForObject(sql, params, (rs, rowNum) -> new TransfertKpiDTO(
        rs.getLong("total"),
        rs.getLong("transferes"),
        rs.getLong("receptionnes"),
        rs.getLong("sorties_validees"),
        rs.getLong("annules"),
        rs.getLong("en_attente"),
        rs.getLong("pharmacies_source"),
        rs.getLong("pharmacies_destination"),
        rs.getLong("produits"),
        rs.getBigDecimal("quantite_total"),
        rs.getBigDecimal("quantite_moyenne"),
        formatDate(rs.getDate("dernier")),
        formatDate(rs.getDate("premier")),
        stringParam(params, "dateDebut"),
        stringParam(params, "dateFin")));
  }

  public List<TransfertListItemDTO> searchList(TransfertSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 500));
    params.put("offset", Math.max(c.offset(), 0));
    String sql = """
        SELECT ts.id,
          CONCAT('TR-', ts.id) AS reference,
          r.id AS requisition_id,
          ts.statut,
          r.fkPharmacieStock AS pharmacie_source_id,
          ph_src.designation AS pharmacie_source,
          r.fkPharmacie AS pharmacie_destination_id,
          ph_dst.designation AS pharmacie_destination,
        """
        + DATE_OPERATION + " AS date_transfert,\n"
        + """
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          ts.datecreate,
          ts.dateupdate
        """
        + FROM_BASE + where
        + "ORDER BY " + DATE_OPERATION + " DESC, ts.id DESC\n"
        + "LIMIT :limit OFFSET :offset";
    return jdbc.query(sql, params, listMapper());
  }

  public TransfertDetailDTO findDetail(long id) {
    Map<String, Object> params = Map.of("id", id);
    String headerSql = """
        SELECT ts.id,
          CONCAT('TR-', ts.id) AS reference,
          r.id AS requisition_id,
          ts.statut,
          r.fkPharmacieStock AS pharmacie_source_id,
          ph_src.designation AS pharmacie_source,
          r.fkPharmacie AS pharmacie_destination_id,
          ph_dst.designation AS pharmacie_destination,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          uc.username AS encodeur_username,
          ts.datecreate,
          ts.dateupdate,
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale
        """ + FROM_BASE + " AND ts.id = :id";
    List<TransfertDetailDTO> headers = jdbc.query(headerSql, params, (rs, rowNum) -> {
      List<TransfertLineDetailDTO> lignes = findLines(id);
      boolean qteOk = lignes.stream().noneMatch(l ->
          l.quantite() == null || l.quantite().signum() <= 0);
      boolean prodOk = lignes.stream().noneMatch(l -> l.produitId() == null);
      boolean complete = rs.getString("pharmacie_source") != null
          && rs.getString("pharmacie_destination") != null
          && rs.getInt("lignes_count") > 0;
      boolean risque = !complete || !qteOk || !prodOk;
      String topQty = lignes.stream()
          .filter(l -> l.quantite() != null)
          .max((a, b) -> a.quantite().compareTo(b.quantite()))
          .map(TransfertLineDetailDTO::produit)
          .orElse(null);
      return new TransfertDetailDTO(
          rs.getLong("id"),
          rs.getString("reference"),
          rs.getLong("requisition_id"),
          rs.getString("statut"),
          rs.getLong("pharmacie_source_id"),
          rs.getString("pharmacie_source"),
          rs.getLong("pharmacie_destination_id"),
          rs.getString("pharmacie_destination"),
          formatDateFromTs(rs.getTimestamp("datecreate")),
          rs.getString("encodeur"),
          rs.getString("encodeur_username"),
          formatTs(rs.getTimestamp("datecreate")),
          formatTs(rs.getTimestamp("dateupdate")),
          rs.getInt("lignes_count"),
          rs.getInt("produits_distinct"),
          rs.getBigDecimal("quantite_totale"),
          topQty,
          lignes,
          new TransfertQualityFlagsDTO(complete, qteOk, prodOk, risque));
    });
    return headers.isEmpty() ? null : headers.get(0);
  }

  public List<TransfertLineDetailDTO> findLines(long transfertId) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY lts.id) AS line_num,
          lts.fkStock AS stock_id,
          p.id AS produit_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
          p.nomscientifique,
          fo.designation AS forme,
          d.designation AS dosage,
          cat.designation AS categorie,
          lts.quantiteDemandee AS quantite_demandee,
          lts.quantite,
          ph_src.designation AS pharmacie_source
        FROM lignes_transferts_stock lts
        INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
        LEFT JOIN stock_produits sp ON lts.fkStock = sp.id
        LEFT JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes fo ON p.fkForme = fo.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        LEFT JOIN categorie_produit cat ON p.fkCategorie = cat.id
        WHERE lts.fkTransfertStock = :id
        ORDER BY lts.id
        """, Map.of("id", transfertId), lineMapper());
  }

  public List<TransfertGroupStatDTO> groupByPharmacieDestination(TransfertSearchCriteria c) {
    return groupBy(c, """
        ph_dst.id AS group_id,
        ph_dst.designation AS group_label,
        COUNT(DISTINCT r.fkPharmacieStock) AS info_extra
        """, "ph_dst.id", "ph_dst.id, ph_dst.designation");
  }

  public List<TransfertGroupStatDTO> groupByPharmacieSource(TransfertSearchCriteria c) {
    return groupBy(c, """
        ph_src.id AS group_id,
        ph_src.designation AS group_label,
        COUNT(DISTINCT r.fkPharmacie) AS info_extra
        """, "ph_src.id", "ph_src.id, ph_src.designation");
  }

  public List<TransfertGroupStatDTO> groupByStatut(TransfertSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        ts.statut AS group_label,
        NULL AS info_extra
        """, "ts.statut", "ts.statut");
  }

  private static final String UTILISATEUR_LABEL_EXPR =
      "COALESCE(NULLIF(CONCAT_WS(' ', uc.prenom, uc.nom), ''), uc.username)";

  public List<TransfertGroupStatDTO> groupByUtilisateur(TransfertSearchCriteria c) {
    return groupBy(c, """
        uc.id AS group_id,
        """
        + UTILISATEUR_LABEL_EXPR + " AS group_label,\n"
        + """
        NULL AS info_extra
        """, "uc.id", "uc.id, " + UTILISATEUR_LABEL_EXPR);
  }

  public List<TransfertGroupStatDTO> topProduits(TransfertSearchCriteria c, boolean ascending) {
    Map<String, Object> params = buildParams(c);
    StringBuilder w = new StringBuilder(buildWhere(c, params, false));
    params.put("limit", Math.min(Math.max(c.limit(), 1), 100));
    String sql = """
        SELECT
          CAST(p.id AS CHAR) AS group_key,
          p.id AS group_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS group_label,
          COUNT(DISTINCT ts.id) AS nb_transferts,
          COUNT(DISTINCT lts.fkStock) AS produits_distinct,
          COALESCE(SUM(lts.quantite), 0) AS quantite_totale,
        """
        + "MAX(" + DATE_OPERATION + ") AS derniere,\n"
        + "MIN(" + DATE_OPERATION + ") AS premiere,\n"
        + """
          NULL AS info_extra
        FROM lignes_transferts_stock lts
        INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
        INNER JOIN pharmacies ph_dst ON r.fkPharmacie = ph_dst.id
        LEFT JOIN utilisateurs uc ON ts.usercreateid = uc.id
        INNER JOIN stock_produits sp ON lts.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        WHERE 1=1
        """
        + w
        + """
        GROUP BY p.id, group_label
        ORDER BY nb_transferts """
        + (ascending ? "ASC" : "DESC")
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  public List<TransfertPeriodStatDTO> synthèseMensuelle(TransfertSearchCriteria c) {
    return synthèsePeriode(c, "%Y-%m", 36);
  }

  public List<TransfertPeriodStatDTO> synthèseAnnuelle(TransfertSearchCriteria c) {
    return synthèsePeriode(c, "%Y", 10);
  }

  public List<TransfertProduitHistoryDTO> historiqueProduit(long produitId, TransfertSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    params.put("produitId", produitId);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String sql = """
        SELECT ts.id AS transfert_id,
          CONCAT('TR-', ts.id) AS reference,
          r.id AS requisition_id,
          CONCAT('REQ-', r.id) AS reference_requisition,
          DATE(r.datecreate) AS date_demande,
        """
        + DATE_OPERATION + " AS date_transfert,\n"
        + """
          CASE
            WHEN rs.id IS NOT NULL THEN DATE(COALESCE(rs.dateupdate, rs.datecreate))
            WHEN ts.statut = 'RECEPTIONNEE' THEN DATE(COALESCE(ts.dateupdate, ts.datecreate))
            ELSE NULL
          END AS date_reception,
          ph_src.designation AS pharmacie_source,
          ph_dst.designation AS pharmacie_destination,
          ts.statut,
          lts.quantiteDemandee AS quantite_demandee,
          lts.quantite,
          CONCAT_WS(' ', NULLIF(TRIM(ud.prenom), ''), NULLIF(TRIM(ud.nom), '')) AS demandeur,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          CONCAT_WS(' ', NULLIF(TRIM(ur.prenom), ''), NULLIF(TRIM(ur.nom), '')) AS receptionneur
        FROM lignes_transferts_stock lts
        INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
        INNER JOIN pharmacies ph_dst ON r.fkPharmacie = ph_dst.id
        LEFT JOIN utilisateurs ud ON r.usercreateid = ud.id
        LEFT JOIN utilisateurs uc ON ts.usercreateid = uc.id
        LEFT JOIN reception_stock rs ON rs.id = (
          SELECT MAX(rs2.id) FROM reception_stock rs2
          WHERE rs2.fkTransfert = ts.id AND rs2.statut = 'RECEPTIONNEE'
        )
        LEFT JOIN utilisateurs ur ON ur.id = COALESCE(rs.userupdateid, rs.usercreateid)
        INNER JOIN stock_produits sp ON lts.fkStock = sp.id
        WHERE sp.fkProduits = :produitId
        """
        + where
        + """
        ORDER BY date_transfert DESC, ts.id DESC
        LIMIT :limit
        """;
    return jdbc.query(sql, params, (rs, rowNum) -> new TransfertProduitHistoryDTO(
        rs.getLong("transfert_id"),
        rs.getString("reference"),
        toLongObject(rs.getObject("requisition_id")),
        rs.getString("reference_requisition"),
        formatDate(rs.getDate("date_demande")),
        formatDate(rs.getDate("date_transfert")),
        formatDate(rs.getDate("date_reception")),
        rs.getString("pharmacie_source"),
        rs.getString("pharmacie_destination"),
        rs.getString("statut"),
        rs.getBigDecimal("quantite_demandee"),
        rs.getBigDecimal("quantite"),
        blankToNull(rs.getString("demandeur")),
        blankToNull(rs.getString("encodeur")),
        blankToNull(rs.getString("receptionneur"))));
  }

  public List<TransfertAnomalyDTO> findAnomalies(TransfertSearchCriteria c) {
    List<TransfertAnomalyDTO> result = new ArrayList<>();
    Map<String, Object> params = buildParams(c);
    String scopeWhere = buildWhere(c, params).replace("WHERE 1=1\n", "");

    result.addAll(queryAnomaly("""
        SELECT ts.id, CONCAT('TR-', ts.id), 'Sans lignes', ts.statut,
          ph_src.designation, ph_dst.designation,
        """
        + DATE_OPERATION + ",\n"
        + """
          'Aucune ligne de transfert'
        """
        + FROM_BASE + scopeWhere + """
         AND (agg.lignes_count IS NULL OR agg.lignes_count = 0)
        ORDER BY ts.id DESC LIMIT 100
        """, params));

    String lineWhere = buildWhere(c, params);
    result.addAll(queryAnomaly("""
        SELECT DISTINCT ts.id, CONCAT('TR-', ts.id), 'Ligne sans stock', ts.statut,
          ph_src.designation, ph_dst.designation,
        """
        + DATE_OPERATION + ",\n"
        + """
          CONCAT('Ligne #', lts.id, ' sans référence stock')
        FROM transferts_stock ts
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
        INNER JOIN pharmacies ph_dst ON r.fkPharmacie = ph_dst.id
        INNER JOIN lignes_transferts_stock lts ON lts.fkTransfertStock = ts.id
        WHERE lts.fkStock IS NULL
        """
        + lineWhere + """
        ORDER BY ts.id DESC LIMIT 100
        """, params));

    result.addAll(queryAnomaly("""
        SELECT ts.id, CONCAT('TR-', ts.id), 'En attente prolongée', ts.statut,
          ph_src.designation, ph_dst.designation,
        """
        + DATE_OPERATION + ",\n"
        + """
          CONCAT('En attente depuis ', DATEDIFF(CURDATE(), DATE(ts.datecreate)), ' jours')
        """
        + FROM_BASE + scopeWhere + """
         AND ts.statut = 'EN ATTENTE' AND DATEDIFF(CURDATE(), DATE(ts.datecreate)) > 30
        ORDER BY ts.id DESC LIMIT 100
        """, params));

    return result.stream().limit(200).toList();
  }

  public List<Map<String, Object>> lookupPharmaciesSource(String q, int limit, Long pharmacieId, String scope) {
    return lookupPharmacies(q, limit, pharmacieId, scope, true);
  }

  public List<Map<String, Object>> lookupPharmaciesDestination(String q, int limit, Long pharmacieId, String scope) {
    return lookupPharmacies(q, limit, pharmacieId, scope, false);
  }

  public List<Map<String, Object>> lookupUtilisateurs(String q, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieId != null) {
      params.put("pharmacieSourceId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT u.id,
          COALESCE(NULLIF(CONCAT_WS(' ', u.prenom, u.nom), ''), u.username) AS label
        FROM utilisateurs u
        INNER JOIN transferts_stock ts ON ts.usercreateid = u.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
        WHERE u.id IS NOT NULL
        """
        + scopeFilter(resolvedScope, pharmacieId);
    if (q != null && !q.isBlank()) {
      sql += " AND (u.username LIKE :q OR u.nom LIKE :q OR u.prenom LIKE :q)";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY label LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  public List<Map<String, Object>> lookupProduits(String q, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 200));
    if (pharmacieId != null) {
      params.put("pharmacieSourceId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT p.id,
          COALESCE(NULLIF(TRIM(p.nomcommercial), ''), p.nomscientifique) AS label
        FROM produits p
        INNER JOIN stock_produits sp ON sp.fkProduits = p.id
        INNER JOIN lignes_transferts_stock lts ON lts.fkStock = sp.id
        INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
        WHERE p.id IS NOT NULL
        """
        + scopeFilter(resolvedScope, pharmacieId);
    if (q != null && !q.isBlank()) {
      sql += " AND (p.nomcommercial LIKE :q OR p.nomscientifique LIKE :q)";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY label LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  private List<Map<String, Object>> lookupPharmacies(
      String q, int limit, Long pharmacieId, String scope, boolean source) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieId != null) {
      params.put("pharmacieSourceId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String joinCol = source ? "r.fkPharmacieStock" : "r.fkPharmacie";
    // Ne pas terminer un text block par "ON " : les espaces de fin sont stripés → "ONr.xxx"
    String sql = """
        SELECT DISTINCT ph.id, ph.designation AS label
        FROM pharmacies ph
        INNER JOIN requisitions r ON %s = ph.id
        INNER JOIN transferts_stock ts ON ts.fkRequisition = r.id
        INNER JOIN pharmacies ph_src ON r.fkPharmacieStock = ph_src.id
        WHERE ph.id IS NOT NULL
        """.formatted(joinCol)
        + scopeFilter(resolvedScope, pharmacieId);
    if (q != null && !q.isBlank()) {
      sql += " AND ph.designation LIKE :q";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY ph.designation LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  private List<TransfertPeriodStatDTO> synthèsePeriode(TransfertSearchCriteria c, String dateFormat, int maxRows) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = "SELECT DATE_FORMAT(" + DATE_OPERATION + ", '" + dateFormat + "') AS periode,\n"
        + """
          COUNT(*) AS nb,
          COUNT(DISTINCT r.fkPharmacieStock) AS pharmacies_source,
          COUNT(DISTINCT r.fkPharmacie) AS pharmacies_destination,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite
        """
        + FROM_BASE + where
        + """
        GROUP BY periode
        ORDER BY periode DESC
        LIMIT """
        + maxRows;
    return jdbc.query(sql, params, (rs, rowNum) -> new TransfertPeriodStatDTO(
        rs.getString("periode"),
        rs.getLong("nb"),
        rs.getLong("pharmacies_source"),
        rs.getLong("pharmacies_destination"),
        rs.getLong("produits"),
        rs.getBigDecimal("quantite"),
        null,
        null));
  }

  private static boolean isSimpleColumnRef(String expr) {
    return expr.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$");
  }

  private static String groupKeySelect(String groupKeyExpr) {
    if (isSimpleColumnRef(groupKeyExpr)) {
      return "CAST(" + groupKeyExpr + " AS CHAR(64)) AS group_key";
    }
    return groupKeyExpr + " AS group_key";
  }

  private List<TransfertGroupStatDTO> groupBy(
      TransfertSearchCriteria c, String selectExtra, String groupKeyExpr, String groupByClause) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String sql = "SELECT "
        + groupKeySelect(groupKeyExpr) + ", "
        + selectExtra + ", "
        + "COUNT(*) AS nb_transferts, "
        + "COALESCE(SUM(agg.produits_distinct), 0) AS produits_distinct, "
        + "COALESCE(SUM(agg.quantite_totale), 0) AS quantite_totale, "
        + "MAX(" + DATE_OPERATION + ") AS derniere, "
        + "MIN(" + DATE_OPERATION + ") AS premiere "
        + FROM_BASE + where
        + " GROUP BY " + groupByClause
        + " ORDER BY nb_transferts DESC"
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  private List<TransfertAnomalyDTO> queryAnomaly(String sql, Map<String, Object> params) {
    return jdbc.query(sql, params, (rs, rowNum) -> new TransfertAnomalyDTO(
        rs.getLong(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        rs.getString(6),
        formatDate(rs.getDate(7)),
        rs.getString(8)));
  }

  private Map<String, Object> buildParams(TransfertSearchCriteria c) {
    Map<String, Object> params = new HashMap<>();
    applyPresetDates(c, params);
    bindPharmacieScope(c, params);
    if (c.dateDebut() != null) {
      params.put("dateDebut", c.dateDebut());
    }
    if (c.dateFin() != null) {
      params.put("dateFin", c.dateFin());
    }
    if (c.pharmacieDestinationId() != null) {
      params.put("pharmacieDestinationId", c.pharmacieDestinationId());
    }
    if (c.utilisateurId() != null) {
      params.put("utilisateurId", c.utilisateurId());
    }
    if (c.produitId() != null) {
      params.put("produitId", c.produitId());
    }
    if (c.statut() != null && !c.statut().isBlank()) {
      params.put("statut", c.statut());
    }
    if (c.tousStatuts()) {
      params.put("tousStatuts", true);
    }
    if (c.reference() != null && !c.reference().isBlank()) {
      params.put("reference", "%" + c.reference().trim() + "%");
    }
    if (c.produitQ() != null && !c.produitQ().isBlank()) {
      params.put("produitQ", "%" + c.produitQ().trim() + "%");
    }
    if (c.quantiteMin() != null) {
      params.put("quantiteMin", c.quantiteMin());
    }
    if (c.quantiteMax() != null) {
      params.put("quantiteMax", c.quantiteMax());
    }
    return params;
  }

  private void applyPresetDates(TransfertSearchCriteria c, Map<String, Object> params) {
    if (c.preset() == null || c.preset().isBlank()) {
      return;
    }
    LocalDate today = LocalDate.now();
    switch (c.preset().toUpperCase()) {
      case "TODAY" -> {
        params.put("dateDebut", today);
        params.put("dateFin", today);
      }
      case "THIS_WEEK" -> {
        params.put("dateDebut", today.with(DayOfWeek.MONDAY));
        params.put("dateFin", today);
      }
      case "THIS_MONTH" -> {
        params.put("dateDebut", today.withDayOfMonth(1));
        params.put("dateFin", today);
      }
      case "LAST_MONTH" -> {
        LocalDate first = today.minusMonths(1).withDayOfMonth(1);
        params.put("dateDebut", first);
        params.put("dateFin", first.with(TemporalAdjusters.lastDayOfMonth()));
      }
      case "THIS_YEAR" -> {
        params.put("dateDebut", today.withDayOfYear(1));
        params.put("dateFin", today);
      }
      case "LAST_30_DAYS" -> {
        params.put("dateDebut", today.minusDays(30));
        params.put("dateFin", today);
      }
      case "LAST_90_DAYS" -> {
        params.put("dateDebut", today.minusDays(90));
        params.put("dateFin", today);
      }
      default -> { /* ignore */ }
    }
  }

  private void bindPharmacieScope(TransfertSearchCriteria c, Map<String, Object> params) {
    if (c.pharmacieSourceId() != null) {
      params.put("pharmacieSourceId", c.pharmacieSourceId());
    } else {
      params.remove("pharmacieSourceId");
    }
  }

  private String buildWhere(TransfertSearchCriteria c, Map<String, Object> params) {
    return buildWhere(c, params, true);
  }

  private String buildWhere(TransfertSearchCriteria c, Map<String, Object> params, boolean withAgg) {
    StringBuilder w = new StringBuilder();
    Long pharmacieScope = c.pharmacieSourceId();
    String scope = c.scope() != null ? c.scope() : "CENTRALE";
    w.append(scopeFilter(scope, pharmacieScope));
    if (!params.containsKey("statut") && !params.containsKey("tousStatuts")) {
      w.append(" AND ts.statut IN ('TRANSFEREE', 'RECEPTIONNEE')\n");
    }
    if (params.containsKey("dateDebut")) {
      w.append(" AND ").append(DATE_OPERATION).append(" >= :dateDebut\n");
    }
    if (params.containsKey("dateFin")) {
      w.append(" AND ").append(DATE_OPERATION).append(" <= :dateFin\n");
    }
    if (params.containsKey("pharmacieDestinationId")) {
      w.append(" AND r.fkPharmacie = :pharmacieDestinationId\n");
    }
    if (params.containsKey("utilisateurId")) {
      w.append(" AND ts.usercreateid = :utilisateurId\n");
    }
    if (params.containsKey("statut")) {
      w.append(" AND ts.statut = :statut\n");
    }
    if (params.containsKey("reference")) {
      w.append(" AND (CONCAT('TR-', ts.id) LIKE :reference OR CAST(r.id AS CHAR) LIKE :reference)\n");
    }
    if (withAgg && params.containsKey("quantiteMin")) {
      w.append(" AND COALESCE(agg.quantite_totale, 0) >= :quantiteMin\n");
    }
    if (withAgg && params.containsKey("quantiteMax")) {
      w.append(" AND COALESCE(agg.quantite_totale, 0) <= :quantiteMax\n");
    }
    if (params.containsKey("produitId") || params.containsKey("produitQ")) {
      w.append("""
           AND EXISTS (
            SELECT 1 FROM lignes_transferts_stock lts2
            INNER JOIN stock_produits sp2 ON lts2.fkStock = sp2.id
            INNER JOIN produits p2 ON sp2.fkProduits = p2.id
            WHERE lts2.fkTransfertStock = ts.id
          """);
      if (params.containsKey("produitId")) {
        w.append(" AND p2.id = :produitId");
      }
      if (params.containsKey("produitQ")) {
        w.append(" AND (p2.nomcommercial LIKE :produitQ OR p2.nomscientifique LIKE :produitQ)");
      }
      w.append(")\n");
    }
    return w.toString();
  }

  private static String scopeFilter(String scope, Long pharmacieSourceId) {
    if (pharmacieSourceId != null) {
      return " AND r.fkPharmacieStock = :pharmacieSourceId\n";
    }
    boolean centrale = scope == null || !"CLIENT".equalsIgnoreCase(scope.trim());
    return centrale
        ? " AND UPPER(TRIM(ph_src.typepharmacie)) = 'CENTRALE'\n"
        : " AND UPPER(TRIM(ph_dst.typepharmacie)) IN ('CLIENTE','URGENCE','HOSPITALISATION')\n";
  }

  private static RowMapper<TransfertListItemDTO> listMapper() {
    return (rs, rowNum) -> new TransfertListItemDTO(
        rs.getLong("id"),
        rs.getString("reference"),
        toLongObject(rs.getObject("requisition_id")),
        rs.getString("statut"),
        rs.getLong("pharmacie_source_id"),
        rs.getString("pharmacie_source"),
        rs.getLong("pharmacie_destination_id"),
        rs.getString("pharmacie_destination"),
        formatDate(rs.getDate("date_transfert")),
        rs.getInt("lignes_count"),
        rs.getInt("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        rs.getString("encodeur"),
        formatTs(rs.getTimestamp("datecreate")),
        formatTs(rs.getTimestamp("dateupdate")));
  }

  private static RowMapper<TransfertLineDetailDTO> lineMapper() {
    return (rs, rowNum) -> new TransfertLineDetailDTO(
        rs.getInt("line_num"),
        toLongObject(rs.getObject("stock_id")),
        toLongObject(rs.getObject("produit_id")),
        rs.getString("produit"),
        rs.getString("nomscientifique"),
        rs.getString("forme"),
        rs.getString("dosage"),
        rs.getString("categorie"),
        rs.getBigDecimal("quantite_demandee"),
        rs.getBigDecimal("quantite"),
        rs.getString("pharmacie_source"));
  }

  private static RowMapper<TransfertGroupStatDTO> groupMapper() {
    return (rs, rowNum) -> new TransfertGroupStatDTO(
        rs.getString("group_key"),
        toLongObject(rs.getObject("group_id")),
        rs.getString("group_label"),
        rs.getLong("nb_transferts"),
        rs.getLong("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        formatDate(rs.getDate("derniere")),
        formatDate(rs.getDate("premiere")),
        rs.getString("info_extra"));
  }

  private static Long toLongObject(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Long l) {
      return l;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    return Long.valueOf(value.toString());
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static String formatDate(Date d) {
    return d != null ? d.toLocalDate().toString() : null;
  }

  private static String formatDateFromTs(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime().toLocalDate().toString() : null;
  }

  private static String formatTs(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime().toString() : null;
  }

  private static String stringParam(Map<String, Object> params, String key) {
    Object v = params.get(key);
    return v != null ? v.toString() : null;
  }
}
