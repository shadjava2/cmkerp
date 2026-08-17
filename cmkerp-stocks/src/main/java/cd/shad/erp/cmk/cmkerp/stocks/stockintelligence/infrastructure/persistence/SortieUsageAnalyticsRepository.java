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

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageLineDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsagePeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageQualityFlagsDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageSearchCriteria;

@Repository
public class SortieUsageAnalyticsRepository {

  private static final String DATE_OPERATION = "DATE(v.datecreate)";

  private static final String AGG_JOIN = """
      LEFT JOIN (
        SELECT lv.fkVente,
          COUNT(*) AS lignes_count,
          COUNT(DISTINCT lv.fkStock) AS produits_distinct,
          COALESCE(SUM(COALESCE(lv.qt, 0)), 0) AS quantite_totale,
          COALESCE(SUM(COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0)), 0) AS montant_total
        FROM lignes_vente lv
        GROUP BY lv.fkVente
      ) agg ON agg.fkVente = v.id
      """;

  private static final String FROM_BASE = """
      FROM ventes v
      INNER JOIN pharmacies ph ON v.fkPharmacie = ph.id
      LEFT JOIN utilisateurs uc ON v.usercreateid = uc.id
      """ + AGG_JOIN + "\nWHERE 1=1\n";

  private final NamedParameterJdbcTemplate jdbc;

  public SortieUsageAnalyticsRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public SortieUsageKpiDTO computeKpis(SortieUsageSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = """
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN 1 ELSE 0 END) AS sorties_usage,
          SUM(CASE WHEN v.statut = 'EN ATTENTE' OR v.statut = 'ORDONNANCE EN ATTENTE' THEN 1 ELSE 0 END) AS en_attente,
          SUM(CASE WHEN v.statut = 'ANNULEE' OR v.statut = 'ANNULEE-REMBOURSE' THEN 1 ELSE 0 END) AS annules,
          SUM(CASE WHEN v.statut = 'PAYEE' THEN 1 ELSE 0 END) AS payees,
          SUM(CASE WHEN v.statut = 'FACTUREE' THEN 1 ELSE 0 END) AS facturees,
          COUNT(DISTINCT v.fkPharmacie) AS pharmacies,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite_total,
          COALESCE(AVG(agg.quantite_totale), 0) AS quantite_moyenne,
          COALESCE(SUM(agg.montant_total), 0) AS montant_total,
          COALESCE(AVG(agg.montant_total), 0) AS montant_moyen,
        """
        + "MAX(" + DATE_OPERATION + ") AS dernier,\n"
        + "MIN(" + DATE_OPERATION + ") AS premier\n"
        + FROM_BASE + where;
    return jdbc.queryForObject(sql, params, (rs, rowNum) -> new SortieUsageKpiDTO(
        rs.getLong("total"),
        rs.getLong("sorties_usage"),
        rs.getLong("en_attente"),
        rs.getLong("annules"),
        rs.getLong("payees"),
        rs.getLong("facturees"),
        rs.getLong("pharmacies"),
        rs.getLong("produits"),
        rs.getBigDecimal("quantite_total"),
        rs.getBigDecimal("quantite_moyenne"),
        rs.getBigDecimal("montant_total"),
        rs.getBigDecimal("montant_moyen"),
        formatDate(rs.getDate("dernier")),
        formatDate(rs.getDate("premier")),
        stringParam(params, "dateDebut"),
        stringParam(params, "dateFin")));
  }

  public List<SortieUsageListItemDTO> searchList(SortieUsageSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 500));
    params.put("offset", Math.max(c.offset(), 0));
    String sql = """
        SELECT v.id,
          CONCAT('SU-', v.id) AS reference,
          v.statut,
          v.fkPharmacie AS pharmacie_id,
          ph.designation AS pharmacie,
          v.demandeur,
          v.raisonsortie AS raison_sortie,
        """
        + DATE_OPERATION + " AS date_sortie,\n"
        + """
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale,
          COALESCE(agg.montant_total, 0) AS montant_total,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          v.datecreate,
          v.dateupdate
        """
        + FROM_BASE + where
        + "ORDER BY " + DATE_OPERATION + " DESC, v.id DESC\n"
        + "LIMIT :limit OFFSET :offset";
    return jdbc.query(sql, params, listMapper());
  }

  public SortieUsageDetailDTO findDetail(long id) {
    Map<String, Object> params = Map.of("id", id);
    String headerSql = """
        SELECT v.id,
          CONCAT('SU-', v.id) AS reference,
          v.statut,
          v.fkPharmacie AS pharmacie_id,
          ph.designation AS pharmacie,
          v.demandeur,
          v.raisonsortie AS raison_sortie,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          uc.username AS encodeur_username,
          v.datecreate,
          v.dateupdate,
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale,
          COALESCE(agg.montant_total, 0) AS montant_total
        """ + FROM_BASE + " AND v.id = :id";
    List<SortieUsageDetailDTO> headers = jdbc.query(headerSql, params, (rs, rowNum) -> {
      List<SortieUsageLineDetailDTO> lignes = findLines(id);
      boolean qteOk = lignes.stream().noneMatch(l ->
          l.quantite() == null || l.quantite().signum() <= 0);
      boolean prodOk = lignes.stream().noneMatch(l -> l.produitId() == null);
      boolean complete = rs.getString("pharmacie") != null && rs.getInt("lignes_count") > 0;
      boolean risque = !complete || !qteOk || !prodOk;
      String topQty = lignes.stream()
          .filter(l -> l.quantite() != null)
          .max((a, b) -> a.quantite().compareTo(b.quantite()))
          .map(SortieUsageLineDetailDTO::produit)
          .orElse(null);
      return new SortieUsageDetailDTO(
          rs.getLong("id"),
          rs.getString("reference"),
          rs.getString("statut"),
          rs.getLong("pharmacie_id"),
          rs.getString("pharmacie"),
          rs.getString("demandeur"),
          rs.getString("raison_sortie"),
          formatDateFromTs(rs.getTimestamp("datecreate")),
          rs.getString("encodeur"),
          rs.getString("encodeur_username"),
          formatTs(rs.getTimestamp("datecreate")),
          formatTs(rs.getTimestamp("dateupdate")),
          rs.getInt("lignes_count"),
          rs.getInt("produits_distinct"),
          rs.getBigDecimal("quantite_totale"),
          rs.getBigDecimal("montant_total"),
          topQty,
          lignes,
          new SortieUsageQualityFlagsDTO(complete, qteOk, prodOk, risque));
    });
    return headers.isEmpty() ? null : headers.get(0);
  }

  public List<SortieUsageLineDetailDTO> findLines(long venteId) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY lv.id) AS line_num,
          lv.fkStock AS stock_id,
          p.id AS produit_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
          p.nomscientifique,
          fo.designation AS forme,
          d.designation AS dosage,
          cat.designation AS categorie,
          lv.qt AS quantite,
          lv.prixventes AS prix_unitaire,
          (COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0)) AS montant_ligne
        FROM lignes_vente lv
        LEFT JOIN stock_produits sp ON lv.fkStock = sp.id
        LEFT JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes fo ON p.fkForme = fo.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        LEFT JOIN categorie_produit cat ON p.fkCategorie = cat.id
        WHERE lv.fkVente = :id
        ORDER BY lv.id
        """, Map.of("id", venteId), lineMapper());
  }

  private static final String DEMANDEUR_EXPR =
      "COALESCE(NULLIF(TRIM(v.demandeur), ''), '(Sans demandeur)')";
  private static final String RAISON_EXPR =
      "COALESCE(NULLIF(TRIM(v.raisonsortie), ''), '(Sans raison)')";
  private static final String UTILISATEUR_LABEL_EXPR =
      "COALESCE(NULLIF(CONCAT_WS(' ', uc.prenom, uc.nom), ''), uc.username)";

  public List<SortieUsageGroupStatDTO> groupByPharmacie(SortieUsageSearchCriteria c) {
    return groupBy(c, """
        ph.id AS group_id,
        ph.designation AS group_label,
        COUNT(DISTINCT v.demandeur) AS info_extra
        """, "ph.id", "ph.id, ph.designation");
  }

  public List<SortieUsageGroupStatDTO> groupByDemandeur(SortieUsageSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        """
        + DEMANDEUR_EXPR + " AS group_label,\n"
        + """
        COUNT(DISTINCT v.fkPharmacie) AS info_extra
        """, DEMANDEUR_EXPR, DEMANDEUR_EXPR);
  }

  public List<SortieUsageGroupStatDTO> groupByRaisonSortie(SortieUsageSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        """
        + RAISON_EXPR + " AS group_label,\n"
        + """
        COUNT(DISTINCT v.demandeur) AS info_extra
        """, RAISON_EXPR, RAISON_EXPR);
  }

  public List<SortieUsageGroupStatDTO> groupByStatut(SortieUsageSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        v.statut AS group_label,
        NULL AS info_extra
        """, "v.statut", "v.statut");
  }

  public List<SortieUsageGroupStatDTO> groupByUtilisateur(SortieUsageSearchCriteria c) {
    return groupBy(c, """
        uc.id AS group_id,
        """
        + UTILISATEUR_LABEL_EXPR + " AS group_label,\n"
        + """
        NULL AS info_extra
        """, "uc.id", "uc.id, " + UTILISATEUR_LABEL_EXPR);
  }

  public List<SortieUsageGroupStatDTO> topProduits(SortieUsageSearchCriteria c, boolean ascending) {
    Map<String, Object> params = buildParams(c);
    StringBuilder w = new StringBuilder(buildWhere(c, params, false));
    params.put("limit", Math.min(Math.max(c.limit(), 1), 100));
    String sql = """
        SELECT
          CAST(p.id AS CHAR) AS group_key,
          p.id AS group_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS group_label,
          COUNT(DISTINCT v.id) AS nb_sorties,
          COUNT(DISTINCT lv.fkStock) AS produits_distinct,
          COALESCE(SUM(lv.qt), 0) AS quantite_totale,
          COALESCE(SUM(lv.qt * COALESCE(lv.prixventes, 0)), 0) AS montant_total,
        """
        + "MAX(" + DATE_OPERATION + ") AS derniere,\n"
        + "MIN(" + DATE_OPERATION + ") AS premiere,\n"
        + """
          NULL AS info_extra
        FROM lignes_vente lv
        INNER JOIN ventes v ON lv.fkVente = v.id
        INNER JOIN pharmacies ph ON v.fkPharmacie = ph.id
        LEFT JOIN utilisateurs uc ON v.usercreateid = uc.id
        INNER JOIN stock_produits sp ON lv.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        WHERE 1=1
        """
        + w
        + """
        GROUP BY p.id, COALESCE(p.nomcommercial, p.nomscientifique)
        ORDER BY nb_sorties """
        + (ascending ? "ASC" : "DESC")
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  public List<SortieUsagePeriodStatDTO> synthèseMensuelle(SortieUsageSearchCriteria c) {
    return synthèsePeriode(c, "%Y-%m", 36);
  }

  public List<SortieUsagePeriodStatDTO> synthèseAnnuelle(SortieUsageSearchCriteria c) {
    return synthèsePeriode(c, "%Y", 10);
  }

  public List<SortieUsageProduitHistoryDTO> historiqueProduit(long produitId, SortieUsageSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    params.put("produitId", produitId);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String sql = """
        SELECT v.id AS sortie_id,
          CONCAT('SU-', v.id) AS reference,
        """
        + DATE_OPERATION + " AS date_sortie,\n"
        + """
          ph.designation AS pharmacie,
          v.demandeur,
          v.raisonsortie AS raison_sortie,
          v.statut,
          lv.qt AS quantite,
          lv.prixventes AS prix_unitaire,
          (COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0)) AS montant_ligne,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur
        FROM lignes_vente lv
        INNER JOIN ventes v ON lv.fkVente = v.id
        INNER JOIN pharmacies ph ON v.fkPharmacie = ph.id
        LEFT JOIN utilisateurs uc ON v.usercreateid = uc.id
        INNER JOIN stock_produits sp ON lv.fkStock = sp.id
        WHERE sp.fkProduits = :produitId
        """
        + where
        + """
        ORDER BY date_sortie DESC, v.id DESC
        LIMIT :limit
        """;
    return jdbc.query(sql, params, (rs, rowNum) -> new SortieUsageProduitHistoryDTO(
        rs.getLong("sortie_id"),
        rs.getString("reference"),
        formatDate(rs.getDate("date_sortie")),
        rs.getString("pharmacie"),
        rs.getString("demandeur"),
        rs.getString("raison_sortie"),
        rs.getString("statut"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("prix_unitaire"),
        rs.getBigDecimal("montant_ligne"),
        rs.getString("encodeur")));
  }

  public List<SortieUsageAnomalyDTO> findAnomalies(SortieUsageSearchCriteria c) {
    List<SortieUsageAnomalyDTO> result = new ArrayList<>();
    Map<String, Object> params = buildParams(c);
    String scopeWhere = buildWhere(c, params).replace("WHERE 1=1\n", "");

    result.addAll(queryAnomaly("""
        SELECT v.id, CONCAT('SU-', v.id), 'Sans lignes', v.statut,
          ph.designation, v.demandeur,
        """
        + DATE_OPERATION + ",\n"
        + """
          'Aucune ligne de sortie'
        """
        + FROM_BASE + scopeWhere + """
         AND (agg.lignes_count IS NULL OR agg.lignes_count = 0)
        ORDER BY v.id DESC LIMIT 100
        """, params));

    String lineWhere = buildWhere(c, params);
    result.addAll(queryAnomaly("""
        SELECT DISTINCT v.id, CONCAT('SU-', v.id), 'Ligne sans stock', v.statut,
          ph.designation, v.demandeur,
        """
        + DATE_OPERATION + ",\n"
        + """
          CONCAT('Ligne #', lv.id, ' sans référence stock')
        FROM ventes v
        INNER JOIN pharmacies ph ON v.fkPharmacie = ph.id
        INNER JOIN lignes_vente lv ON lv.fkVente = v.id
        WHERE lv.fkStock IS NULL
        """
        + lineWhere + """
        ORDER BY v.id DESC LIMIT 100
        """, params));

    result.addAll(queryAnomaly("""
        SELECT v.id, CONCAT('SU-', v.id), 'Sans demandeur', v.statut,
          ph.designation, v.demandeur,
        """
        + DATE_OPERATION + ",\n"
        + """
          'Sortie usage sans demandeur renseigné'
        """
        + FROM_BASE + scopeWhere + """
         AND v.statut = 'SORTIE-USAGE'
         AND (v.demandeur IS NULL OR TRIM(v.demandeur) = '')
        ORDER BY v.id DESC LIMIT 100
        """, params));

    return result.stream().limit(200).toList();
  }

  public List<Map<String, Object>> lookupPharmacies(String q, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieId != null) {
      params.put("pharmacieId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT ph.id, ph.designation AS label
        FROM pharmacies ph
        INNER JOIN ventes v ON v.fkPharmacie = ph.id
        WHERE ph.id IS NOT NULL
        """
        + scopeFilter(resolvedScope, pharmacieId);
    if (q != null && !q.isBlank()) {
      sql += " AND ph.designation LIKE :q";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY ph.designation LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  public List<Map<String, Object>> lookupUtilisateurs(String q, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieId != null) {
      params.put("pharmacieId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT u.id,
          COALESCE(NULLIF(CONCAT_WS(' ', u.prenom, u.nom), ''), u.username) AS label
        FROM utilisateurs u
        INNER JOIN ventes v ON v.usercreateid = u.id
        INNER JOIN pharmacies ph ON v.fkPharmacie = ph.id
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
      params.put("pharmacieId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT p.id,
          COALESCE(NULLIF(TRIM(p.nomcommercial), ''), p.nomscientifique) AS label
        FROM produits p
        INNER JOIN stock_produits sp ON sp.fkProduits = p.id
        INNER JOIN lignes_vente lv ON lv.fkStock = sp.id
        INNER JOIN ventes v ON lv.fkVente = v.id
        INNER JOIN pharmacies ph ON v.fkPharmacie = ph.id
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

  public List<Map<String, Object>> lookupDemandeurs(String q, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieId != null) {
      params.put("pharmacieId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT TRIM(v.demandeur) AS label
        FROM ventes v
        INNER JOIN pharmacies ph ON v.fkPharmacie = ph.id
        WHERE v.demandeur IS NOT NULL AND TRIM(v.demandeur) <> ''
        """
        + scopeFilter(resolvedScope, pharmacieId);
    if (q != null && !q.isBlank()) {
      sql += " AND v.demandeur LIKE :q";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY label LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  private List<SortieUsagePeriodStatDTO> synthèsePeriode(SortieUsageSearchCriteria c, String dateFormat, int maxRows) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = "SELECT DATE_FORMAT(" + DATE_OPERATION + ", '" + dateFormat + "') AS periode,\n"
        + """
          COUNT(*) AS nb,
          COUNT(DISTINCT v.fkPharmacie) AS pharmacies,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite,
          COALESCE(SUM(agg.montant_total), 0) AS montant
        """
        + FROM_BASE + where
        + """
        GROUP BY periode
        ORDER BY periode DESC
        LIMIT """
        + maxRows;
    return jdbc.query(sql, params, (rs, rowNum) -> new SortieUsagePeriodStatDTO(
        rs.getString("periode"),
        rs.getLong("nb"),
        rs.getLong("pharmacies"),
        rs.getLong("produits"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("montant"),
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

  private List<SortieUsageGroupStatDTO> groupBy(
      SortieUsageSearchCriteria c, String selectExtra, String groupKeyExpr, String groupByClause) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String sql = "SELECT "
        + groupKeySelect(groupKeyExpr) + ", "
        + selectExtra + ", "
        + "COUNT(*) AS nb_sorties, "
        + "COALESCE(SUM(agg.produits_distinct), 0) AS produits_distinct, "
        + "COALESCE(SUM(agg.quantite_totale), 0) AS quantite_totale, "
        + "COALESCE(SUM(agg.montant_total), 0) AS montant_total, "
        + "MAX(" + DATE_OPERATION + ") AS derniere, "
        + "MIN(" + DATE_OPERATION + ") AS premiere "
        + FROM_BASE + where
        + " GROUP BY " + groupByClause
        + " ORDER BY nb_sorties DESC"
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  private List<SortieUsageAnomalyDTO> queryAnomaly(String sql, Map<String, Object> params) {
    return jdbc.query(sql, params, (rs, rowNum) -> new SortieUsageAnomalyDTO(
        rs.getLong(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        rs.getString(6),
        formatDate(rs.getDate(7)),
        rs.getString(8)));
  }

  private Map<String, Object> buildParams(SortieUsageSearchCriteria c) {
    Map<String, Object> params = new HashMap<>();
    applyPresetDates(c, params);
    bindPharmacieScope(c, params);
    if (c.dateDebut() != null) {
      params.put("dateDebut", c.dateDebut());
    }
    if (c.dateFin() != null) {
      params.put("dateFin", c.dateFin());
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
    if (c.demandeur() != null && !c.demandeur().isBlank()) {
      if ("(Sans demandeur)".equals(c.demandeur().trim())) {
        params.put("demandeurVide", true);
      } else {
        params.put("demandeur", "%" + c.demandeur().trim() + "%");
      }
    }
    if (c.raisonSortie() != null && !c.raisonSortie().isBlank()) {
      if ("(Sans raison)".equals(c.raisonSortie().trim())) {
        params.put("raisonVide", true);
      } else {
        params.put("raisonSortie", "%" + c.raisonSortie().trim() + "%");
      }
    }
    if (c.quantiteMin() != null) {
      params.put("quantiteMin", c.quantiteMin());
    }
    if (c.quantiteMax() != null) {
      params.put("quantiteMax", c.quantiteMax());
    }
    if (c.montantMin() != null) {
      params.put("montantMin", c.montantMin());
    }
    if (c.montantMax() != null) {
      params.put("montantMax", c.montantMax());
    }
    return params;
  }

  private void applyPresetDates(SortieUsageSearchCriteria c, Map<String, Object> params) {
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

  private void bindPharmacieScope(SortieUsageSearchCriteria c, Map<String, Object> params) {
    if (c.pharmacieId() != null) {
      params.put("pharmacieId", c.pharmacieId());
    } else {
      params.remove("pharmacieId");
    }
  }

  private String buildWhere(SortieUsageSearchCriteria c, Map<String, Object> params) {
    return buildWhere(c, params, true);
  }

  private String buildWhere(SortieUsageSearchCriteria c, Map<String, Object> params, boolean withAgg) {
    StringBuilder w = new StringBuilder();
    Long pharmacieScope = c.pharmacieId();
    String scope = c.scope() != null ? c.scope() : "CENTRALE";
    w.append(scopeFilter(scope, pharmacieScope));
    if (!params.containsKey("statut") && !params.containsKey("tousStatuts")) {
      w.append(" AND v.statut = 'SORTIE-USAGE'\n");
    }
    if (params.containsKey("dateDebut")) {
      w.append(" AND ").append(DATE_OPERATION).append(" >= :dateDebut\n");
    }
    if (params.containsKey("dateFin")) {
      w.append(" AND ").append(DATE_OPERATION).append(" <= :dateFin\n");
    }
    if (params.containsKey("utilisateurId")) {
      w.append(" AND v.usercreateid = :utilisateurId\n");
    }
    if (params.containsKey("statut")) {
      w.append(" AND v.statut = :statut\n");
    }
    if (params.containsKey("reference")) {
      w.append(" AND CONCAT('SU-', v.id) LIKE :reference\n");
    }
    if (params.containsKey("demandeurVide")) {
      w.append(" AND (v.demandeur IS NULL OR TRIM(v.demandeur) = '')\n");
    } else if (params.containsKey("demandeur")) {
      w.append(" AND v.demandeur LIKE :demandeur\n");
    }
    if (params.containsKey("raisonVide")) {
      w.append(" AND (v.raisonsortie IS NULL OR TRIM(v.raisonsortie) = '')\n");
    } else if (params.containsKey("raisonSortie")) {
      w.append(" AND v.raisonsortie LIKE :raisonSortie\n");
    }
    if (withAgg && params.containsKey("quantiteMin")) {
      w.append(" AND COALESCE(agg.quantite_totale, 0) >= :quantiteMin\n");
    }
    if (withAgg && params.containsKey("quantiteMax")) {
      w.append(" AND COALESCE(agg.quantite_totale, 0) <= :quantiteMax\n");
    }
    if (withAgg && params.containsKey("montantMin")) {
      w.append(" AND COALESCE(agg.montant_total, 0) >= :montantMin\n");
    }
    if (withAgg && params.containsKey("montantMax")) {
      w.append(" AND COALESCE(agg.montant_total, 0) <= :montantMax\n");
    }
    if (params.containsKey("produitId") || params.containsKey("produitQ")) {
      w.append("""
           AND EXISTS (
            SELECT 1 FROM lignes_vente lv2
            INNER JOIN stock_produits sp2 ON lv2.fkStock = sp2.id
            INNER JOIN produits p2 ON sp2.fkProduits = p2.id
            WHERE lv2.fkVente = v.id
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

  private static String scopeFilter(String scope, Long pharmacieId) {
    if (pharmacieId != null) {
      return " AND v.fkPharmacie = :pharmacieId\n";
    }
    boolean centrale = scope == null || !"CLIENT".equalsIgnoreCase(scope.trim());
    return centrale
        ? " AND UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'\n"
        : " AND UPPER(TRIM(ph.typepharmacie)) IN ('CLIENTE','URGENCE','HOSPITALISATION')\n";
  }

  private static RowMapper<SortieUsageListItemDTO> listMapper() {
    return (rs, rowNum) -> new SortieUsageListItemDTO(
        rs.getLong("id"),
        rs.getString("reference"),
        rs.getString("statut"),
        rs.getLong("pharmacie_id"),
        rs.getString("pharmacie"),
        rs.getString("demandeur"),
        rs.getString("raison_sortie"),
        formatDate(rs.getDate("date_sortie")),
        rs.getInt("lignes_count"),
        rs.getInt("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        rs.getBigDecimal("montant_total"),
        rs.getString("encodeur"),
        formatTs(rs.getTimestamp("datecreate")),
        formatTs(rs.getTimestamp("dateupdate")));
  }

  private static RowMapper<SortieUsageLineDetailDTO> lineMapper() {
    return (rs, rowNum) -> new SortieUsageLineDetailDTO(
        rs.getInt("line_num"),
        toLongObject(rs.getObject("stock_id")),
        toLongObject(rs.getObject("produit_id")),
        rs.getString("produit"),
        rs.getString("nomscientifique"),
        rs.getString("forme"),
        rs.getString("dosage"),
        rs.getString("categorie"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("prix_unitaire"),
        rs.getBigDecimal("montant_ligne"));
  }

  private static RowMapper<SortieUsageGroupStatDTO> groupMapper() {
    return (rs, rowNum) -> new SortieUsageGroupStatDTO(
        rs.getString("group_key"),
        toLongObject(rs.getObject("group_id")),
        rs.getString("group_label"),
        rs.getLong("nb_sorties"),
        rs.getLong("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        rs.getBigDecimal("montant_total"),
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
