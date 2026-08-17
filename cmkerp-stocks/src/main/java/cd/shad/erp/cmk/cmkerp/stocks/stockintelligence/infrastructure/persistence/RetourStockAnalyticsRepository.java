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

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockLineDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockQualityFlagsDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockSearchCriteria;

@Repository
public class RetourStockAnalyticsRepository {

  private static final String DATE_OPERATION = "DATE(ti.datecreate)";

  private static final String AGG_JOIN = """
      LEFT JOIN (
        SELECT lti.fkTransfertInterne,
          COUNT(*) AS lignes_count,
          COUNT(DISTINCT lti.fkStock) AS produits_distinct,
          COALESCE(SUM(COALESCE(lti.quantite, 0)), 0) AS quantite_totale
        FROM lignes_transfert_interne lti
        GROUP BY lti.fkTransfertInterne
      ) agg ON agg.fkTransfertInterne = ti.id
      LEFT JOIN (
        SELECT rti2.fkTransfertInterne,
          COALESCE(SUM(COALESCE(lrti.quantite, 0)), 0) AS quantite_recue
        FROM reception_transfert_interne rti2
        INNER JOIN lignes_reception_transfert_interne lrti ON lrti.fkReceptionStock = rti2.id
        GROUP BY rti2.fkTransfertInterne
      ) agg_recv ON agg_recv.fkTransfertInterne = ti.id
      """;

  private static final String FROM_BASE = """
      FROM transfert_interne ti
      INNER JOIN pharmacies ph_src ON ti.fkPharmacieSource = ph_src.id
      INNER JOIN pharmacies ph_dst ON ti.fkPharmacieDestination = ph_dst.id
      LEFT JOIN reception_transfert_interne rti ON rti.fkTransfertInterne = ti.id
      LEFT JOIN utilisateurs uc ON ti.usercreateid = uc.id
      """ + AGG_JOIN + "\nWHERE 1=1\n";

  private final NamedParameterJdbcTemplate jdbc;

  public RetourStockAnalyticsRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public RetourStockKpiDTO computeKpis(RetourStockSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = """
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN ti.statut = 'TRANSFEREE' THEN 1 ELSE 0 END) AS transferes,
          SUM(CASE WHEN ti.statut = 'RECEPTIONNEE' THEN 1 ELSE 0 END) AS receptionnes,
          SUM(CASE WHEN ti.statut IN ('TRANSFEREE', 'RECEPTIONNEE') THEN 1 ELSE 0 END) AS retours_valides,
          SUM(CASE WHEN ti.statut = 'REJETEE' THEN 1 ELSE 0 END) AS rejetes,
          SUM(CASE WHEN ti.statut = 'EN ATTENTE' OR ti.statut = 'VALIDEE' THEN 1 ELSE 0 END) AS en_attente,
          SUM(CASE WHEN rti.statut = 'ANNULEE' THEN 1 ELSE 0 END) AS annules,
          SUM(CASE WHEN ti.perime = 1 OR rti.perime = 1 THEN 1 ELSE 0 END) AS perimes,
          COUNT(DISTINCT ti.fkPharmacieSource) AS pharmacies_source,
          COUNT(DISTINCT ti.fkPharmacieDestination) AS pharmacies_destination,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite_total,
          COALESCE(SUM(agg_recv.quantite_recue), 0) AS quantite_recue,
          COALESCE(AVG(agg.quantite_totale), 0) AS quantite_moyenne,
        """
        + "MAX(" + DATE_OPERATION + ") AS dernier,\n"
        + "MIN(" + DATE_OPERATION + ") AS premier\n"
        + FROM_BASE + where;
    return jdbc.queryForObject(sql, params, (rs, rowNum) -> new RetourStockKpiDTO(
        rs.getLong("total"),
        rs.getLong("transferes"),
        rs.getLong("receptionnes"),
        rs.getLong("retours_valides"),
        rs.getLong("rejetes"),
        rs.getLong("en_attente"),
        rs.getLong("annules"),
        rs.getLong("perimes"),
        rs.getLong("pharmacies_source"),
        rs.getLong("pharmacies_destination"),
        rs.getLong("produits"),
        rs.getBigDecimal("quantite_total"),
        rs.getBigDecimal("quantite_recue"),
        rs.getBigDecimal("quantite_moyenne"),
        formatDate(rs.getDate("dernier")),
        formatDate(rs.getDate("premier")),
        stringParam(params, "dateDebut"),
        stringParam(params, "dateFin")));
  }

  public List<RetourStockListItemDTO> searchList(RetourStockSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 500));
    params.put("offset", Math.max(c.offset(), 0));
    String sql = """
        SELECT ti.id,
          CONCAT('RS-', ti.id) AS reference,
          rti.id AS reception_id,
          ti.statut,
          rti.statut AS statut_reception,
          COALESCE(ti.perime, rti.perime) AS perime,
          ti.fkPharmacieSource AS pharmacie_source_id,
          ph_src.designation AS pharmacie_source,
          ti.fkPharmacieDestination AS pharmacie_destination_id,
          ph_dst.designation AS pharmacie_destination,
          ti.commentaire,
        """
        + DATE_OPERATION + " AS date_retour,\n"
        + """
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale,
          COALESCE(agg_recv.quantite_recue, 0) AS quantite_recue,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          ti.datecreate,
          ti.dateupdate
        """
        + FROM_BASE + where
        + "ORDER BY " + DATE_OPERATION + " DESC, ti.id DESC\n"
        + "LIMIT :limit OFFSET :offset";
    return jdbc.query(sql, params, listMapper());
  }

  public RetourStockDetailDTO findDetail(long id) {
    Map<String, Object> params = Map.of("id", id);
    String headerSql = """
        SELECT ti.id,
          CONCAT('RS-', ti.id) AS reference,
          rti.id AS reception_id,
          ti.statut,
          rti.statut AS statut_reception,
          COALESCE(ti.perime, rti.perime) AS perime,
          ti.fkPharmacieSource AS pharmacie_source_id,
          ph_src.designation AS pharmacie_source,
          ti.fkPharmacieDestination AS pharmacie_destination_id,
          ph_dst.designation AS pharmacie_destination,
          ti.commentaire,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          uc.username AS encodeur_username,
          ti.datecreate,
          ti.dateupdate,
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale,
          COALESCE(agg_recv.quantite_recue, 0) AS quantite_recue
        """ + FROM_BASE + " AND ti.id = :id";
    List<RetourStockDetailDTO> headers = jdbc.query(headerSql, params, (rs, rowNum) -> {
      List<RetourStockLineDetailDTO> lignes = findLines(id);
      boolean qteOk = lignes.stream().noneMatch(l ->
          l.quantiteTransferee() == null || l.quantiteTransferee().signum() <= 0);
      boolean prodOk = lignes.stream().noneMatch(l -> l.produitId() == null);
      boolean complete = rs.getString("pharmacie_source") != null
          && rs.getString("pharmacie_destination") != null
          && rs.getInt("lignes_count") > 0;
      boolean risque = !complete || !qteOk || !prodOk;
      String topQty = lignes.stream()
          .filter(l -> l.quantiteTransferee() != null)
          .max((a, b) -> a.quantiteTransferee().compareTo(b.quantiteTransferee()))
          .map(RetourStockLineDetailDTO::produit)
          .orElse(null);
      return new RetourStockDetailDTO(
          rs.getLong("id"),
          rs.getString("reference"),
          toLongObject(rs.getObject("reception_id")),
          rs.getString("statut"),
          rs.getString("statut_reception"),
          toBoolean(rs.getObject("perime")),
          rs.getLong("pharmacie_source_id"),
          rs.getString("pharmacie_source"),
          rs.getLong("pharmacie_destination_id"),
          rs.getString("pharmacie_destination"),
          rs.getString("commentaire"),
          formatDateFromTs(rs.getTimestamp("datecreate")),
          rs.getString("encodeur"),
          rs.getString("encodeur_username"),
          formatTs(rs.getTimestamp("datecreate")),
          formatTs(rs.getTimestamp("dateupdate")),
          rs.getInt("lignes_count"),
          rs.getInt("produits_distinct"),
          rs.getBigDecimal("quantite_totale"),
          rs.getBigDecimal("quantite_recue"),
          topQty,
          lignes,
          new RetourStockQualityFlagsDTO(complete, qteOk, prodOk, risque));
    });
    return headers.isEmpty() ? null : headers.get(0);
  }

  public List<RetourStockLineDetailDTO> findLines(long transfertInterneId) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY lti.id) AS line_num,
          lti.fkStock AS stock_id,
          p.id AS produit_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
          p.nomscientifique,
          fo.designation AS forme,
          d.designation AS dosage,
          cat.designation AS categorie,
          lti.quantite AS quantite_transferee,
          lrti.quantiteDemandee AS quantite_demandee,
          lrti.quantite AS quantite_recue,
          ph_src.designation AS pharmacie_source
        FROM lignes_transfert_interne lti
        INNER JOIN transfert_interne ti ON lti.fkTransfertInterne = ti.id
        INNER JOIN pharmacies ph_src ON ti.fkPharmacieSource = ph_src.id
        LEFT JOIN reception_transfert_interne rti ON rti.fkTransfertInterne = ti.id
        LEFT JOIN lignes_reception_transfert_interne lrti
          ON lrti.fkReceptionStock = rti.id AND lrti.fkStock = lti.fkStock
        LEFT JOIN stock_produits sp ON lti.fkStock = sp.id
        LEFT JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes fo ON p.fkForme = fo.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        LEFT JOIN categorie_produit cat ON p.fkCategorie = cat.id
        WHERE lti.fkTransfertInterne = :id
        ORDER BY lti.id
        """, Map.of("id", transfertInterneId), lineMapper());
  }

  private static final String UTILISATEUR_LABEL_EXPR =
      "COALESCE(NULLIF(CONCAT_WS(' ', uc.prenom, uc.nom), ''), uc.username)";
  private static final String STATUT_RECEPTION_EXPR =
      "COALESCE(rti.statut, '(Sans réception)')";
  private static final String PERIME_FLAG_EXPR = "IFNULL(COALESCE(ti.perime, rti.perime), 0)";
  private static final String PERIME_LABEL_EXPR =
      "CASE WHEN " + PERIME_FLAG_EXPR + " = 1 THEN 'Retour périmé' ELSE 'Retour stock' END";

  public List<RetourStockGroupStatDTO> groupByPharmacieSource(RetourStockSearchCriteria c) {
    return groupBy(c, """
        ph_src.id AS group_id,
        ph_src.designation AS group_label,
        COUNT(DISTINCT ti.fkPharmacieDestination) AS info_extra
        """, "ph_src.id", "ph_src.id, ph_src.designation");
  }

  public List<RetourStockGroupStatDTO> groupByPharmacieDestination(RetourStockSearchCriteria c) {
    return groupBy(c, """
        ph_dst.id AS group_id,
        ph_dst.designation AS group_label,
        COUNT(DISTINCT ti.fkPharmacieSource) AS info_extra
        """, "ph_dst.id", "ph_dst.id, ph_dst.designation");
  }

  public List<RetourStockGroupStatDTO> groupByStatut(RetourStockSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        ti.statut AS group_label,
        NULL AS info_extra
        """, "ti.statut", "ti.statut");
  }

  public List<RetourStockGroupStatDTO> groupByStatutReception(RetourStockSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        """
        + STATUT_RECEPTION_EXPR + " AS group_label,\n"
        + """
        NULL AS info_extra
        """, STATUT_RECEPTION_EXPR, STATUT_RECEPTION_EXPR);
  }

  public List<RetourStockGroupStatDTO> groupByUtilisateur(RetourStockSearchCriteria c) {
    return groupBy(c, """
        uc.id AS group_id,
        """
        + UTILISATEUR_LABEL_EXPR + " AS group_label,\n"
        + """
        NULL AS info_extra
        """, "uc.id", "uc.id, " + UTILISATEUR_LABEL_EXPR);
  }

  public List<RetourStockGroupStatDTO> groupByPerime(RetourStockSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        """
        + PERIME_LABEL_EXPR + " AS group_label,\n"
        + """
        NULL AS info_extra
        """, PERIME_FLAG_EXPR, PERIME_FLAG_EXPR);
  }

  public List<RetourStockGroupStatDTO> topProduits(RetourStockSearchCriteria c, boolean ascending) {
    Map<String, Object> params = buildParams(c);
    StringBuilder w = new StringBuilder(buildWhere(c, params, false));
    params.put("limit", Math.min(Math.max(c.limit(), 1), 100));
    String sql = """
        SELECT
          CAST(p.id AS CHAR) AS group_key,
          p.id AS group_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS group_label,
          COUNT(DISTINCT ti.id) AS nb_retours,
          COUNT(DISTINCT lti.fkStock) AS produits_distinct,
          COALESCE(SUM(lti.quantite), 0) AS quantite_totale,
          COALESCE(SUM(lrti.quantite), 0) AS quantite_recue,
        """
        + "MAX(" + DATE_OPERATION + ") AS derniere,\n"
        + "MIN(" + DATE_OPERATION + ") AS premiere,\n"
        + """
          NULL AS info_extra
        FROM lignes_transfert_interne lti
        INNER JOIN transfert_interne ti ON lti.fkTransfertInterne = ti.id
        INNER JOIN pharmacies ph_src ON ti.fkPharmacieSource = ph_src.id
        INNER JOIN pharmacies ph_dst ON ti.fkPharmacieDestination = ph_dst.id
        LEFT JOIN reception_transfert_interne rti ON rti.fkTransfertInterne = ti.id
        LEFT JOIN lignes_reception_transfert_interne lrti
          ON lrti.fkReceptionStock = rti.id AND lrti.fkStock = lti.fkStock
        LEFT JOIN utilisateurs uc ON ti.usercreateid = uc.id
        INNER JOIN stock_produits sp ON lti.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        WHERE 1=1
        """
        + w
        + """
        GROUP BY p.id, COALESCE(p.nomcommercial, p.nomscientifique)
        ORDER BY nb_retours """
        + (ascending ? "ASC" : "DESC")
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  public List<RetourStockPeriodStatDTO> synthèseMensuelle(RetourStockSearchCriteria c) {
    return synthèsePeriode(c, "%Y-%m", 36);
  }

  public List<RetourStockPeriodStatDTO> synthèseAnnuelle(RetourStockSearchCriteria c) {
    return synthèsePeriode(c, "%Y", 10);
  }

  public List<RetourStockProduitHistoryDTO> historiqueProduit(long produitId, RetourStockSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    params.put("produitId", produitId);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String sql = """
        SELECT ti.id AS retour_id,
          CONCAT('RS-', ti.id) AS reference,
        """
        + DATE_OPERATION + " AS date_retour,\n"
        + """
          ph_src.designation AS pharmacie_source,
          ph_dst.designation AS pharmacie_destination,
          ti.statut,
          rti.statut AS statut_reception,
          COALESCE(ti.perime, rti.perime) AS perime,
          lti.quantite AS quantite_transferee,
          lrti.quantite AS quantite_recue,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur
        FROM lignes_transfert_interne lti
        INNER JOIN transfert_interne ti ON lti.fkTransfertInterne = ti.id
        INNER JOIN pharmacies ph_src ON ti.fkPharmacieSource = ph_src.id
        INNER JOIN pharmacies ph_dst ON ti.fkPharmacieDestination = ph_dst.id
        LEFT JOIN reception_transfert_interne rti ON rti.fkTransfertInterne = ti.id
        LEFT JOIN lignes_reception_transfert_interne lrti
          ON lrti.fkReceptionStock = rti.id AND lrti.fkStock = lti.fkStock
        LEFT JOIN utilisateurs uc ON ti.usercreateid = uc.id
        INNER JOIN stock_produits sp ON lti.fkStock = sp.id
        WHERE sp.fkProduits = :produitId
        """
        + where
        + """
        ORDER BY date_retour DESC, ti.id DESC
        LIMIT :limit
        """;
    return jdbc.query(sql, params, (rs, rowNum) -> new RetourStockProduitHistoryDTO(
        rs.getLong("retour_id"),
        rs.getString("reference"),
        formatDate(rs.getDate("date_retour")),
        rs.getString("pharmacie_source"),
        rs.getString("pharmacie_destination"),
        rs.getString("statut"),
        rs.getString("statut_reception"),
        toBoolean(rs.getObject("perime")),
        rs.getBigDecimal("quantite_transferee"),
        rs.getBigDecimal("quantite_recue"),
        rs.getString("encodeur")));
  }

  public List<RetourStockAnomalyDTO> findAnomalies(RetourStockSearchCriteria c) {
    List<RetourStockAnomalyDTO> result = new ArrayList<>();
    Map<String, Object> params = buildParams(c);
    String scopeWhere = buildWhere(c, params).replace("WHERE 1=1\n", "");

    result.addAll(queryAnomaly("""
        SELECT ti.id, CONCAT('RS-', ti.id), 'Sans lignes', ti.statut,
          ph_src.designation, ph_dst.designation,
        """
        + DATE_OPERATION + ",\n"
        + """
          'Aucune ligne de retour'
        """
        + FROM_BASE + scopeWhere + """
         AND (agg.lignes_count IS NULL OR agg.lignes_count = 0)
        ORDER BY ti.id DESC LIMIT 100
        """, params));

    String lineWhere = buildWhere(c, params);
    result.addAll(queryAnomaly("""
        SELECT DISTINCT ti.id, CONCAT('RS-', ti.id), 'Écart réception', ti.statut,
          ph_src.designation, ph_dst.designation,
        """
        + DATE_OPERATION + ",\n"
        + """
          CONCAT('Transféré: ', lti.quantite, ' / Reçu: ', COALESCE(lrti.quantite, 0))
        FROM transfert_interne ti
        INNER JOIN pharmacies ph_src ON ti.fkPharmacieSource = ph_src.id
        INNER JOIN pharmacies ph_dst ON ti.fkPharmacieDestination = ph_dst.id
        INNER JOIN lignes_transfert_interne lti ON lti.fkTransfertInterne = ti.id
        LEFT JOIN reception_transfert_interne rti ON rti.fkTransfertInterne = ti.id
        LEFT JOIN lignes_reception_transfert_interne lrti
          ON lrti.fkReceptionStock = rti.id AND lrti.fkStock = lti.fkStock
        WHERE rti.statut = 'RECEPTIONNEE'
          AND COALESCE(lrti.quantite, 0) <> COALESCE(lti.quantite, 0)
        """
        + lineWhere + """
        ORDER BY ti.id DESC LIMIT 100
        """, params));

    result.addAll(queryAnomaly("""
        SELECT ti.id, CONCAT('RS-', ti.id), 'En attente prolongée', ti.statut,
          ph_src.designation, ph_dst.designation,
        """
        + DATE_OPERATION + ",\n"
        + """
          CONCAT('En attente depuis ', DATEDIFF(CURDATE(), DATE(ti.datecreate)), ' jours')
        """
        + FROM_BASE + scopeWhere + """
         AND ti.statut IN ('EN ATTENTE', 'VALIDEE', 'TRANSFEREE')
         AND DATEDIFF(CURDATE(), DATE(ti.datecreate)) > 30
        ORDER BY ti.id DESC LIMIT 100
        """, params));

    return result.stream().limit(200).toList();
  }

  public List<Map<String, Object>> lookupPharmaciesSource(String q, int limit, Long pharmacieDestinationId, String scope) {
    return lookupPharmacies(q, limit, pharmacieDestinationId, scope, true);
  }

  public List<Map<String, Object>> lookupPharmaciesDestination(String q, int limit, Long pharmacieDestinationId, String scope) {
    return lookupPharmacies(q, limit, pharmacieDestinationId, scope, false);
  }

  public List<Map<String, Object>> lookupUtilisateurs(String q, int limit, Long pharmacieDestinationId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieDestinationId != null) {
      params.put("pharmacieDestinationId", pharmacieDestinationId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT u.id,
          COALESCE(NULLIF(CONCAT_WS(' ', u.prenom, u.nom), ''), u.username) AS label
        FROM utilisateurs u
        INNER JOIN transfert_interne ti ON ti.usercreateid = u.id
        INNER JOIN pharmacies ph_dst ON ti.fkPharmacieDestination = ph_dst.id
        WHERE u.id IS NOT NULL
        """
        + scopeFilter(resolvedScope, pharmacieDestinationId);
    if (q != null && !q.isBlank()) {
      sql += " AND (u.username LIKE :q OR u.nom LIKE :q OR u.prenom LIKE :q)";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY label LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  public List<Map<String, Object>> lookupProduits(String q, int limit, Long pharmacieDestinationId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 200));
    if (pharmacieDestinationId != null) {
      params.put("pharmacieDestinationId", pharmacieDestinationId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT p.id,
          COALESCE(NULLIF(TRIM(p.nomcommercial), ''), p.nomscientifique) AS label
        FROM produits p
        INNER JOIN stock_produits sp ON sp.fkProduits = p.id
        INNER JOIN lignes_transfert_interne lti ON lti.fkStock = sp.id
        INNER JOIN transfert_interne ti ON lti.fkTransfertInterne = ti.id
        INNER JOIN pharmacies ph_dst ON ti.fkPharmacieDestination = ph_dst.id
        WHERE p.id IS NOT NULL
        """
        + scopeFilter(resolvedScope, pharmacieDestinationId);
    if (q != null && !q.isBlank()) {
      sql += " AND (p.nomcommercial LIKE :q OR p.nomscientifique LIKE :q)";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY label LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  private List<Map<String, Object>> lookupPharmacies(
      String q, int limit, Long pharmacieDestinationId, String scope, boolean source) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieDestinationId != null) {
      params.put("pharmacieDestinationId", pharmacieDestinationId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String joinCol = source ? "ti.fkPharmacieSource" : "ti.fkPharmacieDestination";
    // Ne pas terminer un text block par "ON " : les espaces de fin sont stripés → "ONti.xxx"
    String sql = """
        SELECT DISTINCT ph.id, ph.designation AS label
        FROM pharmacies ph
        INNER JOIN transfert_interne ti ON %s = ph.id
        INNER JOIN pharmacies ph_dst ON ti.fkPharmacieDestination = ph_dst.id
        WHERE ph.id IS NOT NULL
        """.formatted(joinCol)
        + scopeFilter(resolvedScope, pharmacieDestinationId);
    if (q != null && !q.isBlank()) {
      sql += " AND ph.designation LIKE :q";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY ph.designation LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  private List<RetourStockPeriodStatDTO> synthèsePeriode(RetourStockSearchCriteria c, String dateFormat, int maxRows) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = "SELECT DATE_FORMAT(" + DATE_OPERATION + ", '" + dateFormat + "') AS periode,\n"
        + """
          COUNT(*) AS nb,
          COUNT(DISTINCT ti.fkPharmacieSource) AS pharmacies_source,
          COUNT(DISTINCT ti.fkPharmacieDestination) AS pharmacies_destination,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite,
          COALESCE(SUM(agg_recv.quantite_recue), 0) AS quantite_recue
        """
        + FROM_BASE + where
        + """
        GROUP BY periode
        ORDER BY periode DESC
        LIMIT """
        + maxRows;
    return jdbc.query(sql, params, (rs, rowNum) -> new RetourStockPeriodStatDTO(
        rs.getString("periode"),
        rs.getLong("nb"),
        rs.getLong("pharmacies_source"),
        rs.getLong("pharmacies_destination"),
        rs.getLong("produits"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("quantite_recue"),
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

  private List<RetourStockGroupStatDTO> groupBy(
      RetourStockSearchCriteria c, String selectExtra, String groupKeyExpr, String groupByClause) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String sql = "SELECT "
        + groupKeySelect(groupKeyExpr) + ", "
        + selectExtra + ", "
        + "COUNT(*) AS nb_retours, "
        + "COALESCE(SUM(agg.produits_distinct), 0) AS produits_distinct, "
        + "COALESCE(SUM(agg.quantite_totale), 0) AS quantite_totale, "
        + "COALESCE(SUM(agg_recv.quantite_recue), 0) AS quantite_recue, "
        + "MAX(" + DATE_OPERATION + ") AS derniere, "
        + "MIN(" + DATE_OPERATION + ") AS premiere "
        + FROM_BASE + where
        + " GROUP BY " + groupByClause
        + " ORDER BY nb_retours DESC"
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  private List<RetourStockAnomalyDTO> queryAnomaly(String sql, Map<String, Object> params) {
    return jdbc.query(sql, params, (rs, rowNum) -> new RetourStockAnomalyDTO(
        rs.getLong(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        rs.getString(6),
        formatDate(rs.getDate(7)),
        rs.getString(8)));
  }

  private Map<String, Object> buildParams(RetourStockSearchCriteria c) {
    Map<String, Object> params = new HashMap<>();
    applyPresetDates(c, params);
    bindPharmacieScope(c, params);
    if (c.dateDebut() != null) {
      params.put("dateDebut", c.dateDebut());
    }
    if (c.dateFin() != null) {
      params.put("dateFin", c.dateFin());
    }
    if (c.pharmacieSourceId() != null) {
      params.put("pharmacieSourceId", c.pharmacieSourceId());
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
    if (c.statutReception() != null && !c.statutReception().isBlank()) {
      params.put("statutReception", c.statutReception());
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
    if (c.perime() != null) {
      params.put("perime", c.perime() ? 1 : 0);
    }
    if (c.quantiteMin() != null) {
      params.put("quantiteMin", c.quantiteMin());
    }
    if (c.quantiteMax() != null) {
      params.put("quantiteMax", c.quantiteMax());
    }
    return params;
  }

  private void applyPresetDates(RetourStockSearchCriteria c, Map<String, Object> params) {
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

  private void bindPharmacieScope(RetourStockSearchCriteria c, Map<String, Object> params) {
    if (c.pharmacieDestinationId() != null) {
      params.put("pharmacieDestinationId", c.pharmacieDestinationId());
    } else {
      params.remove("pharmacieDestinationId");
    }
  }

  private String buildWhere(RetourStockSearchCriteria c, Map<String, Object> params) {
    return buildWhere(c, params, true);
  }

  private String buildWhere(RetourStockSearchCriteria c, Map<String, Object> params, boolean withAgg) {
    StringBuilder w = new StringBuilder();
    Long pharmacieScope = c.pharmacieDestinationId();
    String scope = c.scope() != null ? c.scope() : "CENTRALE";
    w.append(scopeFilter(scope, pharmacieScope));
    if (!params.containsKey("statut") && !params.containsKey("tousStatuts")) {
      w.append(" AND ti.statut IN ('TRANSFEREE', 'RECEPTIONNEE')\n");
    }
    if (params.containsKey("dateDebut")) {
      w.append(" AND ").append(DATE_OPERATION).append(" >= :dateDebut\n");
    }
    if (params.containsKey("dateFin")) {
      w.append(" AND ").append(DATE_OPERATION).append(" <= :dateFin\n");
    }
    if (params.containsKey("pharmacieSourceId")) {
      w.append(" AND ti.fkPharmacieSource = :pharmacieSourceId\n");
    }
    if (params.containsKey("utilisateurId")) {
      w.append(" AND ti.usercreateid = :utilisateurId\n");
    }
    if (params.containsKey("statut")) {
      w.append(" AND ti.statut = :statut\n");
    }
    if (params.containsKey("statutReception")) {
      w.append(" AND rti.statut = :statutReception\n");
    }
    if (params.containsKey("reference")) {
      w.append(" AND CONCAT('RS-', ti.id) LIKE :reference\n");
    }
    if (params.containsKey("perime")) {
      w.append(" AND ").append(PERIME_FLAG_EXPR).append(" = :perime\n");
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
            SELECT 1 FROM lignes_transfert_interne lti2
            INNER JOIN stock_produits sp2 ON lti2.fkStock = sp2.id
            INNER JOIN produits p2 ON sp2.fkProduits = p2.id
            WHERE lti2.fkTransfertInterne = ti.id
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

  private static String scopeFilter(String scope, Long pharmacieDestinationId) {
    if (pharmacieDestinationId != null) {
      return " AND ti.fkPharmacieDestination = :pharmacieDestinationId\n";
    }
    boolean centrale = scope == null || !"CLIENT".equalsIgnoreCase(scope.trim());
    return centrale
        ? " AND UPPER(TRIM(ph_dst.typepharmacie)) = 'CENTRALE'\n"
        : " AND UPPER(TRIM(ph_src.typepharmacie)) IN ('CLIENTE','URGENCE','HOSPITALISATION')\n";
  }

  private static RowMapper<RetourStockListItemDTO> listMapper() {
    return (rs, rowNum) -> new RetourStockListItemDTO(
        rs.getLong("id"),
        rs.getString("reference"),
        toLongObject(rs.getObject("reception_id")),
        rs.getString("statut"),
        rs.getString("statut_reception"),
        toBoolean(rs.getObject("perime")),
        rs.getLong("pharmacie_source_id"),
        rs.getString("pharmacie_source"),
        rs.getLong("pharmacie_destination_id"),
        rs.getString("pharmacie_destination"),
        rs.getString("commentaire"),
        formatDate(rs.getDate("date_retour")),
        rs.getInt("lignes_count"),
        rs.getInt("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        rs.getBigDecimal("quantite_recue"),
        rs.getString("encodeur"),
        formatTs(rs.getTimestamp("datecreate")),
        formatTs(rs.getTimestamp("dateupdate")));
  }

  private static RowMapper<RetourStockLineDetailDTO> lineMapper() {
    return (rs, rowNum) -> new RetourStockLineDetailDTO(
        rs.getInt("line_num"),
        toLongObject(rs.getObject("stock_id")),
        toLongObject(rs.getObject("produit_id")),
        rs.getString("produit"),
        rs.getString("nomscientifique"),
        rs.getString("forme"),
        rs.getString("dosage"),
        rs.getString("categorie"),
        rs.getBigDecimal("quantite_transferee"),
        rs.getBigDecimal("quantite_demandee"),
        rs.getBigDecimal("quantite_recue"),
        rs.getString("pharmacie_source"));
  }

  private static RowMapper<RetourStockGroupStatDTO> groupMapper() {
    return (rs, rowNum) -> new RetourStockGroupStatDTO(
        rs.getString("group_key"),
        toLongObject(rs.getObject("group_id")),
        rs.getString("group_label"),
        rs.getLong("nb_retours"),
        rs.getLong("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        rs.getBigDecimal("quantite_recue"),
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

  private static Boolean toBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof Number n) {
      return n.intValue() != 0;
    }
    return Boolean.parseBoolean(value.toString());
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
