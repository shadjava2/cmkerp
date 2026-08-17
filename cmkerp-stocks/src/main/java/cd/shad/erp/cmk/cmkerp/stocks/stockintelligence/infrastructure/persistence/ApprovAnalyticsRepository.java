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

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovLineDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovQualityFlagsDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovSearchCriteria;

@Repository
public class ApprovAnalyticsRepository {

  /** Date métier fiable : encodage système (pas datebonliv, souvent saisie à la main). */
  private static final String DATE_OPERATION = "DATE(a.datecreate)";

  private static final String AGG_JOIN = """
      LEFT JOIN (
        SELECT la.fkApprov,
          COUNT(*) AS lignes_count,
          COUNT(DISTINCT la.fkStock) AS produits_distinct,
          COALESCE(SUM(la.qt), 0) AS quantite_totale,
          COALESCE(SUM(COALESCE(la.prixachattotal, la.qt * la.prixachat, 0)), 0) AS montant_total
        FROM lignes_approv la
        GROUP BY la.fkApprov
      ) agg ON agg.fkApprov = a.id
      """;

  private static final String FROM_BASE = """
      FROM approvsionnements a
      INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
      LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
      LEFT JOIN utilisateurs uc ON a.usercreateid = uc.id
      """ + AGG_JOIN + "\nWHERE 1=1\n";

  private final NamedParameterJdbcTemplate jdbc;

  public ApprovAnalyticsRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public ApprovKpiDTO computeKpis(ApprovSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = """
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN a.statut = 'VALIDEE' THEN 1 ELSE 0 END) AS valides,
          SUM(CASE WHEN a.statut = 'ANNULEE' THEN 1 ELSE 0 END) AS annules,
          SUM(CASE WHEN a.statut = 'EN ATTENTE' THEN 1 ELSE 0 END) AS en_attente,
          COUNT(DISTINCT a.fkFournisseur) AS fournisseurs,
          COUNT(DISTINCT a.fkPharmacie) AS pharmacies,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.montant_total), 0) AS montant_total,
          COALESCE(AVG(agg.montant_total), 0) AS montant_moyen,
        """
        + "MAX(" + DATE_OPERATION + ") AS dernier,\n"
        + "MIN(" + DATE_OPERATION + ") AS premier\n"
        + FROM_BASE + where;
    return jdbc.queryForObject(sql, params, (rs, rowNum) -> new ApprovKpiDTO(
        rs.getLong("total"),
        rs.getLong("valides"),
        rs.getLong("annules"),
        rs.getLong("en_attente"),
        rs.getLong("fournisseurs"),
        rs.getLong("pharmacies"),
        rs.getLong("produits"),
        rs.getBigDecimal("montant_total"),
        rs.getBigDecimal("montant_moyen"),
        formatDate(rs.getDate("dernier")),
        formatDate(rs.getDate("premier")),
        stringParam(params, "dateDebut"),
        stringParam(params, "dateFin")));
  }

  public List<ApprovListItemDTO> searchList(ApprovSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 500));
    params.put("offset", Math.max(c.offset(), 0));
    String sql = """
        SELECT a.id,
          COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
          a.statut,
          a.fkFournisseur AS fournisseur_id,
          f.nom AS fournisseur,
          a.fkPharmacie AS pharmacie_id,
          ph.designation AS pharmacie,
        """
        + DATE_OPERATION + " AS date_approv,\n"
        + """
          a.datebonliv AS date_bonliv,
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale,
          COALESCE(agg.montant_total, 0) AS montant_total,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          a.datecreate,
          a.dateupdate
        """
        + FROM_BASE + where
        + "ORDER BY " + DATE_OPERATION + " DESC, a.id DESC\n"
        + """
        LIMIT :limit OFFSET :offset
        """;
    return jdbc.query(sql, params, listMapper());
  }

  public ApprovDetailDTO findDetail(long id) {
    Map<String, Object> params = Map.of("id", id);
    String headerSql = """
        SELECT a.id,
          COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
          a.statut,
          a.fkFournisseur AS fournisseur_id,
          f.nom AS fournisseur,
          a.fkPharmacie AS pharmacie_id,
          ph.designation AS pharmacie,
          a.datebonliv,
          a.numbonliv,
          a.taux,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur,
          uc.username AS encodeur_username,
          a.datecreate,
          a.dateupdate,
          COALESCE(agg.lignes_count, 0) AS lignes_count,
          COALESCE(agg.produits_distinct, 0) AS produits_distinct,
          COALESCE(agg.quantite_totale, 0) AS quantite_totale,
          COALESCE(agg.montant_total, 0) AS montant_total
        """ + FROM_BASE + " AND a.id = :id";
    List<ApprovDetailDTO> headers = jdbc.query(headerSql, params, (rs, rowNum) -> {
      List<ApprovLineDetailDTO> lignes = findLines(id);
      BigDecimal montantTotal = rs.getBigDecimal("montant_total");
      boolean prixOk = lignes.stream().noneMatch(l ->
          l.quantite() != null && l.quantite().signum() > 0
              && (l.prixUnitaire() == null || l.prixUnitaire().signum() == 0));
      boolean qteOk = lignes.stream().noneMatch(l ->
          l.quantite() == null || l.quantite().signum() <= 0);
      boolean prodOk = lignes.stream().noneMatch(l -> l.produitId() == null);
      boolean complete = rs.getString("fournisseur") != null
          && rs.getInt("lignes_count") > 0;
      boolean risque = !complete || !prixOk || !qteOk || !prodOk;
      String topCher = lignes.stream()
          .filter(l -> l.montantLigne() != null)
          .max((a, b) -> a.montantLigne().compareTo(b.montantLigne()))
          .map(ApprovLineDetailDTO::produit)
          .orElse(null);
      String topQty = lignes.stream()
          .filter(l -> l.quantite() != null)
          .max((a, b) -> a.quantite().compareTo(b.quantite()))
          .map(ApprovLineDetailDTO::produit)
          .orElse(null);
      return new ApprovDetailDTO(
          rs.getLong("id"),
          rs.getString("reference"),
          rs.getString("statut"),
          rs.getLong("fournisseur_id"),
          rs.getString("fournisseur"),
          rs.getLong("pharmacie_id"),
          rs.getString("pharmacie"),
          formatDateFromTs(rs.getTimestamp("datecreate")),
          formatDate(rs.getDate("datebonliv")),
          rs.getString("numbonliv"),
          (Integer) rs.getObject("taux"),
          rs.getString("encodeur"),
          rs.getString("encodeur_username"),
          formatTs(rs.getTimestamp("datecreate")),
          formatTs(rs.getTimestamp("dateupdate")),
          rs.getInt("lignes_count"),
          rs.getInt("produits_distinct"),
          rs.getBigDecimal("quantite_totale"),
          montantTotal,
          topCher,
          topQty,
          lignes,
          new ApprovQualityFlagsDTO(complete, prixOk, qteOk, prodOk, risque));
    });
    return headers.isEmpty() ? null : headers.get(0);
  }

  public List<ApprovLineDetailDTO> findLines(long approvId) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY la.id) AS line_num,
          la.fkStock AS stock_id,
          p.id AS produit_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
          p.nomscientifique,
          fo.designation AS forme,
          d.designation AS dosage,
          cat.designation AS categorie,
          la.qt AS quantite,
          la.prixachat AS prix,
          COALESCE(la.prixachattotal, la.qt * la.prixachat, 0) AS montant,
          ph.designation AS pharmacie
        FROM lignes_approv la
        LEFT JOIN stock_produits sp ON la.fkStock = sp.id
        LEFT JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes fo ON p.fkForme = fo.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        LEFT JOIN categorie_produit cat ON p.fkCategorie = cat.id
        INNER JOIN approvsionnements a ON la.fkApprov = a.id
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        WHERE la.fkApprov = :id
        ORDER BY la.id
        """, Map.of("id", approvId), lineMapper());
  }

  public List<ApprovGroupStatDTO> groupByFournisseur(ApprovSearchCriteria c) {
    return groupBy(c, """
        f.id AS group_id,
        f.nom AS group_label,
        COUNT(DISTINCT a.fkPharmacie) AS info_extra
        """, "f.id, f.nom", "nb_approv");
  }

  public List<ApprovGroupStatDTO> groupByPharmacie(ApprovSearchCriteria c) {
    return groupBy(c, """
        ph.id AS group_id,
        ph.designation AS group_label,
        (SELECT f2.nom FROM approvsionnements a2
          LEFT JOIN fournisseurs f2 ON a2.fkFournisseur = f2.id
          WHERE a2.fkPharmacie = ph.id
          GROUP BY f2.nom ORDER BY COUNT(*) DESC LIMIT 1) AS info_extra
        """, "ph.id, ph.designation", "nb_approv");
  }

  public List<ApprovGroupStatDTO> groupByStatut(ApprovSearchCriteria c) {
    return groupBy(c, """
        NULL AS group_id,
        a.statut AS group_label,
        NULL AS info_extra
        """, "a.statut", "nb_approv");
  }

  public List<ApprovGroupStatDTO> groupByUtilisateur(ApprovSearchCriteria c) {
    return groupBy(c, """
        uc.id AS group_id,
        COALESCE(NULLIF(CONCAT_WS(' ', uc.prenom, uc.nom), ''), uc.username) AS group_label,
        NULL AS info_extra
        """, "uc.id, COALESCE(NULLIF(CONCAT_WS(' ', uc.prenom, uc.nom), ''), uc.username)",
        "nb_approv");
  }

  public List<ApprovGroupStatDTO> topProduits(ApprovSearchCriteria c, boolean ascending) {
    Map<String, Object> params = buildParams(c);
    StringBuilder w = new StringBuilder(buildWhere(c, params, false));
    params.put("limit", Math.min(Math.max(c.limit(), 1), 100));
    String sql = """
        SELECT
          CAST(p.id AS CHAR) AS group_key,
          p.id AS group_id,
          COALESCE(p.nomcommercial, p.nomscientifique) AS group_label,
          COUNT(DISTINCT a.id) AS nb_approv,
          COUNT(DISTINCT la.fkStock) AS produits_distinct,
          COALESCE(SUM(la.qt), 0) AS quantite_totale,
          COALESCE(SUM(COALESCE(la.prixachattotal, la.qt * la.prixachat, 0)), 0) AS montant_total,
        """
        + "MAX(" + DATE_OPERATION + ") AS derniere,\n"
        + "MIN(" + DATE_OPERATION + ") AS premiere,\n"
        + """
          NULL AS info_extra
        FROM lignes_approv la
        INNER JOIN approvsionnements a ON la.fkApprov = a.id
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
        LEFT JOIN utilisateurs uc ON a.usercreateid = uc.id
        INNER JOIN stock_produits sp ON la.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        WHERE 1=1
        """
        + w
        + """
        GROUP BY p.id, group_label
        """
        + " ORDER BY nb_approv "
        + (ascending ? "ASC" : "DESC")
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  public List<ApprovPeriodStatDTO> synthèseMensuelle(ApprovSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = "SELECT DATE_FORMAT(" + DATE_OPERATION + ", '%Y-%m') AS periode,\n"
        + """
          COUNT(*) AS nb,
          COUNT(DISTINCT a.fkFournisseur) AS fournisseurs,
          COUNT(DISTINCT a.fkPharmacie) AS pharmacies,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite,
          COALESCE(SUM(agg.montant_total), 0) AS montant
        """
        + FROM_BASE + where
        + """
        GROUP BY periode
        ORDER BY periode DESC
        LIMIT 36
        """;
    return jdbc.query(sql, params, periodMapper());
  }

  public List<ApprovPeriodStatDTO> synthèseAnnuelle(ApprovSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    String sql = "SELECT DATE_FORMAT(" + DATE_OPERATION + ", '%Y') AS periode,\n"
        + """
          COUNT(*) AS nb,
          COUNT(DISTINCT a.fkFournisseur) AS fournisseurs,
          COUNT(DISTINCT a.fkPharmacie) AS pharmacies,
          COALESCE(SUM(agg.produits_distinct), 0) AS produits,
          COALESCE(SUM(agg.quantite_totale), 0) AS quantite,
          COALESCE(SUM(agg.montant_total), 0) AS montant
        """
        + FROM_BASE + where
        + """
        GROUP BY periode
        ORDER BY periode DESC
        LIMIT 10
        """;
    return jdbc.query(sql, params, periodMapper());
  }

  public List<ApprovProduitHistoryDTO> historiqueProduit(long produitId, ApprovSearchCriteria c) {
    Map<String, Object> params = buildParams(c);
    params.put("produitId", produitId);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String sql = """
        SELECT a.id AS approv_id,
          COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
        """
        + DATE_OPERATION + " AS date_approv,\n"
        + """
          f.nom AS fournisseur,
          ph.designation AS pharmacie,
          a.statut,
          la.qt AS quantite,
          la.prixachat AS prix,
          COALESCE(la.prixachattotal, la.qt * la.prixachat, 0) AS montant,
          CONCAT_WS(' ', NULLIF(TRIM(uc.prenom), ''), NULLIF(TRIM(uc.nom), '')) AS encodeur
        FROM lignes_approv la
        INNER JOIN approvsionnements a ON la.fkApprov = a.id
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
        LEFT JOIN utilisateurs uc ON a.usercreateid = uc.id
        INNER JOIN stock_produits sp ON la.fkStock = sp.id
        WHERE sp.fkProduits = :produitId
        """
        + where
        + """
        ORDER BY date_approv DESC, a.id DESC
        LIMIT :limit
        """;
    return jdbc.query(sql, params, (rs, rowNum) -> new ApprovProduitHistoryDTO(
        rs.getLong("approv_id"),
        rs.getString("reference"),
        formatDate(rs.getDate("date_approv")),
        rs.getString("fournisseur"),
        rs.getString("pharmacie"),
        rs.getString("statut"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("prix"),
        rs.getBigDecimal("montant"),
        rs.getString("encodeur")));
  }

  public List<ApprovAnomalyDTO> findAnomalies(ApprovSearchCriteria c) {
    String type = c.anomalyType() != null ? c.anomalyType().toUpperCase() : "ALL";
    List<ApprovAnomalyDTO> result = new ArrayList<>();
    Map<String, Object> params = buildParams(c);
    String scopeWhere = buildWhere(c, params).replace("WHERE 1=1\n", "");

    if ("ALL".equals(type) || "SANS_LIGNES".equals(type)) {
      result.addAll(queryAnomaly("""
          SELECT a.id, COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
            'Sans lignes' AS type_anomalie, a.statut, f.nom, ph.designation,
          """
          + DATE_OPERATION + " AS dt,\n"
          + """
            'Aucune ligne d''approvisionnement' AS detail
          """
          + FROM_BASE + scopeWhere + """
           AND (agg.lignes_count IS NULL OR agg.lignes_count = 0)
          ORDER BY a.id DESC LIMIT 100
          """, params));
    }
    if ("ALL".equals(type) || "LIGNE_SANS_STOCK".equals(type)) {
      String lineWhere = buildWhere(c, params);
      result.addAll(queryAnomaly("""
          SELECT DISTINCT a.id, COALESCE(a.numbonliv, CONCAT('AP-', a.id)),
            'Ligne sans stock', a.statut, f.nom, ph.designation,
          """
          + DATE_OPERATION + ",\n"
          + """
            CONCAT('Ligne #', la.id, ' sans référence stock')
          FROM approvsionnements a
          INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
          LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
          INNER JOIN lignes_approv la ON la.fkApprov = a.id
          WHERE la.fkStock IS NULL
          """
          + lineWhere + """
          ORDER BY a.id DESC LIMIT 100
          """, params));
    }
    if ("ALL".equals(type) || "PRIX_ZERO".equals(type)) {
      String lineWhere = buildWhere(c, params);
      result.addAll(queryAnomaly("""
          SELECT DISTINCT a.id, COALESCE(a.numbonliv, CONCAT('AP-', a.id)),
            'Prix unitaire zéro', a.statut, f.nom, ph.designation,
          """
          + DATE_OPERATION + ",\n"
          + """
            CONCAT('Produit: ', COALESCE(p.nomcommercial, p.nomscientifique))
          FROM lignes_approv la
          INNER JOIN approvsionnements a ON la.fkApprov = a.id
          INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
          LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
          LEFT JOIN stock_produits sp ON la.fkStock = sp.id
          LEFT JOIN produits p ON sp.fkProduits = p.id
          WHERE (la.prixachat IS NULL OR la.prixachat = 0) AND la.qt > 0
          """
          + lineWhere + """
          ORDER BY a.id DESC LIMIT 100
          """, params));
    }
    if ("ALL".equals(type) || "DATE_FUTURE".equals(type)) {
      result.addAll(queryAnomaly("""
          SELECT a.id, COALESCE(a.numbonliv, CONCAT('AP-', a.id)),
            'Date future', a.statut, f.nom, ph.designation, a.datebonliv,
            'Date bon de livraison postérieure à aujourd''hui'
          """ + FROM_BASE + scopeWhere + """
           AND a.datebonliv > CURDATE()
          ORDER BY a.id DESC LIMIT 100
          """, params));
    }
    if ("ALL".equals(type) || "ATTENTE_LONGUE".equals(type)) {
      result.addAll(queryAnomaly("""
          SELECT a.id, COALESCE(a.numbonliv, CONCAT('AP-', a.id)),
            'En attente prolongée', a.statut, f.nom, ph.designation,
          """
          + DATE_OPERATION + ",\n"
          + """
            CONCAT('En attente depuis ', DATEDIFF(CURDATE(), DATE(a.datecreate)), ' jours')
          """
          + FROM_BASE + scopeWhere + """
           AND a.statut = 'EN ATTENTE' AND DATEDIFF(CURDATE(), DATE(a.datecreate)) > 30
          ORDER BY a.id DESC LIMIT 100
          """, params));
    }
    return result.stream().limit(200).toList();
  }

  public List<Map<String, Object>> lookupFournisseurs(String q, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = new HashMap<>();
    params.put("limit", Math.min(limit, 100));
    if (pharmacieId != null) {
      params.put("pharmacieId", pharmacieId);
    }
    String resolvedScope = scope != null && !scope.isBlank() ? scope : "CENTRALE";
    String sql = """
        SELECT DISTINCT f.id, f.nom AS label
        FROM fournisseurs f
        INNER JOIN approvsionnements a ON a.fkFournisseur = f.id
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        WHERE f.id IS NOT NULL
        """
        + approvScopeFilter(resolvedScope, pharmacieId);
    if (q != null && !q.isBlank()) {
      sql += " AND f.nom LIKE :q";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY f.nom LIMIT :limit";
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
        INNER JOIN approvsionnements a ON a.usercreateid = u.id
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        WHERE u.id IS NOT NULL
        """
        + approvScopeFilter(resolvedScope, pharmacieId);
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
        INNER JOIN lignes_approv la ON la.fkStock = sp.id
        INNER JOIN approvsionnements a ON la.fkApprov = a.id
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        WHERE p.id IS NOT NULL
        """
        + approvScopeFilter(resolvedScope, pharmacieId);
    if (q != null && !q.isBlank()) {
      sql += " AND (p.nomcommercial LIKE :q OR p.nomscientifique LIKE :q)";
      params.put("q", "%" + q.trim() + "%");
    }
    sql += " ORDER BY label LIMIT :limit";
    return jdbc.queryForList(sql, params);
  }

  private List<ApprovGroupStatDTO> groupBy(
      ApprovSearchCriteria c, String selectExtra, String groupBy, String orderBy) {
    Map<String, Object> params = buildParams(c);
    String where = buildWhere(c, params);
    params.put("limit", Math.min(Math.max(c.limit(), 1), 200));
    String firstGroupCol = groupBy.split(",")[0].trim();
    String sql = "SELECT "
        + "COALESCE(CAST(" + firstGroupCol + " AS CHAR), " + firstGroupCol + ") AS group_key, "
        + selectExtra + ", "
        + "COUNT(*) AS nb_approv, "
        + "COALESCE(SUM(agg.produits_distinct), 0) AS produits_distinct, "
        + "COALESCE(SUM(agg.quantite_totale), 0) AS quantite_totale, "
        + "COALESCE(SUM(agg.montant_total), 0) AS montant_total, "
        + "MAX(" + DATE_OPERATION + ") AS derniere, "
        + "MIN(" + DATE_OPERATION + ") AS premiere "
        + FROM_BASE + where
        + " GROUP BY " + groupBy
        + " ORDER BY " + orderBy + " DESC"
        + " LIMIT :limit";
    return jdbc.query(sql, params, groupMapper());
  }

  private List<ApprovAnomalyDTO> queryAnomaly(String sql, Map<String, Object> params) {
    return jdbc.query(sql, params, (rs, rowNum) -> new ApprovAnomalyDTO(
        rs.getLong(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        rs.getString(6),
        formatDate(rs.getDate(7)),
        rs.getString(8)));
  }

  private Map<String, Object> buildParams(ApprovSearchCriteria c) {
    Map<String, Object> params = new HashMap<>();
    applyPresetDates(c, params);
    bindPharmacieScope(c, params);
    if (c.dateDebut() != null) {
      params.put("dateDebut", c.dateDebut());
    }
    if (c.dateFin() != null) {
      params.put("dateFin", c.dateFin());
    }
    if (c.fournisseurId() != null) {
      params.put("fournisseurId", c.fournisseurId());
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
    if (c.reference() != null && !c.reference().isBlank()) {
      params.put("reference", "%" + c.reference().trim() + "%");
    }
    if (c.produitQ() != null && !c.produitQ().isBlank()) {
      params.put("produitQ", "%" + c.produitQ().trim() + "%");
    }
    if (c.montantMin() != null) {
      params.put("montantMin", c.montantMin());
    }
    if (c.montantMax() != null) {
      params.put("montantMax", c.montantMax());
    }
    return params;
  }

  private void applyPresetDates(ApprovSearchCriteria c, Map<String, Object> params) {
    if (c.preset() == null || c.preset().isBlank()) {
      return;
    }
    LocalDate today = LocalDate.now();
    switch (c.preset().toUpperCase()) {
      case "TODAY" -> {
        params.put("dateDebut", today);
        params.put("dateFin", today);
      }
      case "YESTERDAY" -> {
        params.put("dateDebut", today.minusDays(1));
        params.put("dateFin", today.minusDays(1));
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

  private void bindPharmacieScope(ApprovSearchCriteria c, Map<String, Object> params) {
    if (c.pharmacieId() != null) {
      params.put("pharmacieId", c.pharmacieId());
    } else {
      params.remove("pharmacieId");
    }
  }

  private String buildWhere(ApprovSearchCriteria c, Map<String, Object> params) {
    return buildWhere(c, params, true);
  }

  private String buildWhere(ApprovSearchCriteria c, Map<String, Object> params, boolean withAgg) {
    StringBuilder w = new StringBuilder();
    Long pharmacieScope = c.pharmacieId();
    String scope = c.scope() != null ? c.scope() : "CENTRALE";
    w.append(approvScopeFilter(scope, pharmacieScope));
    if (params.containsKey("dateDebut")) {
      w.append(" AND ").append(DATE_OPERATION).append(" >= :dateDebut\n");
    }
    if (params.containsKey("dateFin")) {
      w.append(" AND ").append(DATE_OPERATION).append(" <= :dateFin\n");
    }
    if (params.containsKey("fournisseurId")) {
      w.append(" AND a.fkFournisseur = :fournisseurId\n");
    }
    if (params.containsKey("utilisateurId")) {
      w.append(" AND a.usercreateid = :utilisateurId\n");
    }
    if (params.containsKey("statut")) {
      w.append(" AND a.statut = :statut\n");
    }
    if (params.containsKey("reference")) {
      w.append(" AND (a.numbonliv LIKE :reference OR CAST(a.id AS CHAR) LIKE :reference)\n");
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
            SELECT 1 FROM lignes_approv la2
            INNER JOIN stock_produits sp2 ON la2.fkStock = sp2.id
            INNER JOIN produits p2 ON sp2.fkProduits = p2.id
            WHERE la2.fkApprov = a.id
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

  private static String approvScopeFilter(String scope, Long pharmacieId) {
    if (pharmacieId != null) {
      return " AND a.fkPharmacie = :pharmacieId\n";
    }
    boolean centrale = scope == null || !"CLIENT".equalsIgnoreCase(scope.trim());
    return centrale
        ? " AND UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'\n"
        : " AND UPPER(TRIM(ph.typepharmacie)) IN ('CLIENTE','URGENCE','HOSPITALISATION')\n";
  }

  private static RowMapper<ApprovListItemDTO> listMapper() {
    return (rs, rowNum) -> new ApprovListItemDTO(
        rs.getLong("id"),
        rs.getString("reference"),
        rs.getString("statut"),
        rs.getLong("fournisseur_id"),
        rs.getString("fournisseur"),
        rs.getLong("pharmacie_id"),
        rs.getString("pharmacie"),
        formatDate(rs.getDate("date_approv")),
        rs.getInt("lignes_count"),
        rs.getInt("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        rs.getBigDecimal("montant_total"),
        rs.getString("encodeur"),
        formatTs(rs.getTimestamp("datecreate")),
        formatTs(rs.getTimestamp("dateupdate")));
  }

  private static RowMapper<ApprovLineDetailDTO> lineMapper() {
    return (rs, rowNum) -> new ApprovLineDetailDTO(
        rs.getInt("line_num"),
        toLongObject(rs.getObject("stock_id")),
        toLongObject(rs.getObject("produit_id")),
        rs.getString("produit"),
        rs.getString("nomscientifique"),
        rs.getString("forme"),
        rs.getString("dosage"),
        rs.getString("categorie"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("prix"),
        rs.getBigDecimal("montant"),
        rs.getString("pharmacie"));
  }

  private static RowMapper<ApprovGroupStatDTO> groupMapper() {
    return (rs, rowNum) -> new ApprovGroupStatDTO(
        rs.getString("group_key"),
        toLongObject(rs.getObject("group_id")),
        rs.getString("group_label"),
        rs.getLong("nb_approv"),
        rs.getLong("produits_distinct"),
        rs.getBigDecimal("quantite_totale"),
        rs.getBigDecimal("montant_total"),
        formatDate(rs.getDate("derniere")),
        formatDate(rs.getDate("premiere")),
        rs.getString("info_extra"));
  }

  private static RowMapper<ApprovPeriodStatDTO> periodMapper() {
    return (rs, rowNum) -> new ApprovPeriodStatDTO(
        rs.getString("periode"),
        rs.getLong("nb"),
        rs.getLong("fournisseurs"),
        rs.getLong("pharmacies"),
        rs.getLong("produits"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("montant"),
        null,
        null);
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
