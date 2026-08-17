package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.AlertSummaryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.OperationLineDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.OperationListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PendingOperationsDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ProductMovementEventDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockAlertMetricDTO;

@Repository
public class StockPilotageRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public StockPilotageRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public PendingOperationsDTO countPending(Long pharmacieId, String scope) {
    Map<String, Object> params = params(pharmacieId);
    int req = count("""
        SELECT COUNT(*) FROM requisitions r
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE r.statut = 'EN ATTENTE'
        """ + requisitionScopeFilter(scope, pharmacieId), params);
    int tr = count("""
        SELECT COUNT(*) FROM transferts_stock ts
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE ts.statut = 'EN ATTENTE'
        """ + requisitionScopeFilter(scope, pharmacieId), params);
    int ap = count("""
        SELECT COUNT(*) FROM approvsionnements a
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        WHERE a.statut = 'EN ATTENTE'
        """ + approvScopeFilter(scope, pharmacieId), params);
    int rc = count("""
        SELECT COUNT(DISTINCT rs.id)
        FROM reception_stock rs
        INNER JOIN transferts_stock ts ON rs.fkTransfert = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE rs.statut = 'EN ATTENTE'
        """ + requisitionScopeFilter(scope, pharmacieId), params);
    return new PendingOperationsDTO(req, tr, ap, rc);
  }

  public AlertSummaryDTO summarizeAlerts(Long pharmacieId) {
    Map<String, Object> params = params(pharmacieId);
    params.put("today", LocalDate.now());
    StringBuilder base = new StringBuilder("""
        SELECT niveau_alerte, COUNT(*) AS cnt
        FROM stock_alert_metrics
        WHERE date_calcul = :today
        """);
    if (pharmacieId != null) {
      base.append(" AND fkPharmacies = :pharmacieId");
    }
    base.append("\nGROUP BY niveau_alerte");
    Map<String, Integer> byLevel = new HashMap<>();
    jdbc.query(base.toString(), params, (rs, rowNum) -> {
      byLevel.put(rs.getString("niveau_alerte"), rs.getInt("cnt"));
      return null;
    });
    int total = byLevel.values().stream().mapToInt(Integer::intValue).sum();
    return new AlertSummaryDTO(
        byLevel.getOrDefault("RUPTURE", 0),
        byLevel.getOrDefault("CRITIQUE", 0),
        byLevel.getOrDefault("SURVEILLANCE", 0),
        byLevel.getOrDefault("DORMANT", 0),
        byLevel.getOrDefault("SURSTOCK", 0),
        byLevel.getOrDefault("NORMAL", 0),
        total);
  }

  public List<OperationListItemDTO> findRequisitions(String statut, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = params(pharmacieId);
    params.put("limit", Math.min(Math.max(limit, 1), 100));
    String sql = """
        SELECT r.id, r.statut,
               CONCAT('REQ-', r.id) AS reference,
               phs.designation AS pharmacie_source,
               phd.designation AS pharmacie_destination,
               NULL AS fournisseur,
               (SELECT COUNT(*) FROM lignes_requisitions lr WHERE lr.fkRequisition = r.id) AS lignes_count,
               r.datecreate
        FROM requisitions r
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE 1=1
        """ + (statut != null && !statut.isBlank() ? " AND r.statut = :statut" : "")
        + requisitionScopeFilter(scope, pharmacieId) + """
        ORDER BY r.datecreate DESC
        LIMIT :limit
        """;
    if (statut != null && !statut.isBlank()) {
      params.put("statut", statut);
    }
    return jdbc.query(sql, params, operationMapper("REQUISITION"));
  }

  public OperationListItemDTO findRequisitionHeader(Long id) {
    return findSingleOperation("""
        SELECT r.id, r.statut, CONCAT('REQ-', r.id) AS reference,
               phs.designation AS pharmacie_source, phd.designation AS pharmacie_destination,
               NULL AS fournisseur,
               (SELECT COUNT(*) FROM lignes_requisitions lr WHERE lr.fkRequisition = r.id) AS lignes_count,
               r.datecreate
        FROM requisitions r
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE r.id = :id
        """, id, "REQUISITION");
  }

  public List<OperationLineDTO> findRequisitionLines(Long id) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY lr.id) AS line_num,
               COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
               p.nomscientifique, sp.id AS stock_id, lr.quantite AS quantite,
               NULL AS qte_demandee, NULL AS qte_transferee, NULL AS prix,
               f.designation AS forme, d.designation AS dosage
        FROM lignes_requisitions lr
        INNER JOIN stock_produits sp ON lr.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes f ON p.fkForme = f.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        WHERE lr.fkRequisition = :id
        ORDER BY lr.id
        """, Map.of("id", id), lineMapper());
  }

  public List<OperationListItemDTO> findTransferts(String statut, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = params(pharmacieId);
    params.put("limit", Math.min(Math.max(limit, 1), 100));
    String sql = """
        SELECT ts.id, ts.statut,
               CONCAT('TRF-', ts.id) AS reference,
               phs.designation AS pharmacie_source,
               phd.designation AS pharmacie_destination,
               NULL AS fournisseur,
               (SELECT COUNT(*) FROM lignes_transferts_stock l WHERE l.fkTransfertStock = ts.id) AS lignes_count,
               ts.datecreate
        FROM transferts_stock ts
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE 1=1
        """ + (statut != null && !statut.isBlank() ? " AND ts.statut = :statut" : "")
        + requisitionScopeFilter(scope, pharmacieId) + """
        ORDER BY ts.datecreate DESC
        LIMIT :limit
        """;
    if (statut != null && !statut.isBlank()) {
      params.put("statut", statut);
    }
    return jdbc.query(sql, params, operationMapper("TRANSFERT"));
  }

  public OperationListItemDTO findTransfertHeader(Long id) {
    return findSingleOperation("""
        SELECT ts.id, ts.statut, CONCAT('TRF-', ts.id) AS reference,
               phs.designation AS pharmacie_source, phd.designation AS pharmacie_destination,
               NULL AS fournisseur,
               (SELECT COUNT(*) FROM lignes_transferts_stock l WHERE l.fkTransfertStock = ts.id) AS lignes_count,
               ts.datecreate
        FROM transferts_stock ts
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE ts.id = :id
        """, id, "TRANSFERT");
  }

  public List<OperationLineDTO> findTransfertLines(Long id) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY lts.id) AS line_num,
               COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
               p.nomscientifique, sp.id AS stock_id, lts.quantite AS quantite,
               lts.quantiteDemandee AS qte_demandee, NULL AS qte_transferee, NULL AS prix,
               f.designation AS forme, d.designation AS dosage
        FROM lignes_transferts_stock lts
        INNER JOIN stock_produits sp ON lts.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes f ON p.fkForme = f.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        WHERE lts.fkTransfertStock = :id
        ORDER BY lts.id
        """, Map.of("id", id), lineMapper());
  }

  public List<OperationListItemDTO> findApprovisionnements(String statut, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = params(pharmacieId);
    params.put("limit", Math.min(Math.max(limit, 1), 100));
    String sql = """
        SELECT a.id, a.statut,
               COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
               ph.designation AS pharmacie_source,
               NULL AS pharmacie_destination,
               f.nom AS fournisseur,
               (SELECT COUNT(*) FROM lignes_approv la WHERE la.fkApprov = a.id) AS lignes_count,
               a.datecreate
        FROM approvsionnements a
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
        WHERE 1=1
        """ + (statut != null && !statut.isBlank() ? " AND a.statut = :statut" : "")
        + approvScopeFilter(scope, pharmacieId) + """
        ORDER BY a.datecreate DESC
        LIMIT :limit
        """;
    if (statut != null && !statut.isBlank()) {
      params.put("statut", statut);
    }
    return jdbc.query(sql, params, operationMapper("APPROVISIONNEMENT"));
  }

  public OperationListItemDTO findApprovHeader(Long id) {
    return findSingleOperation("""
        SELECT a.id, a.statut, COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
               ph.designation AS pharmacie_source, NULL AS pharmacie_destination,
               f.nom AS fournisseur,
               (SELECT COUNT(*) FROM lignes_approv la WHERE la.fkApprov = a.id) AS lignes_count,
               a.datecreate
        FROM approvsionnements a
        INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
        LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
        WHERE a.id = :id
        """, id, "APPROVISIONNEMENT");
  }

  public List<OperationLineDTO> findApprovLines(Long id) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY la.id) AS line_num,
               COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
               p.nomscientifique, sp.id AS stock_id, la.qt AS quantite,
               NULL AS qte_demandee, NULL AS qte_transferee, la.prixachat AS prix,
               f.designation AS forme, d.designation AS dosage
        FROM lignes_approv la
        INNER JOIN stock_produits sp ON la.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes f ON p.fkForme = f.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        WHERE la.fkApprov = :id
        ORDER BY la.id
        """, Map.of("id", id), lineMapper());
  }

  public List<OperationListItemDTO> findReceptions(String statut, int limit, Long pharmacieId, String scope) {
    Map<String, Object> params = params(pharmacieId);
    params.put("limit", Math.min(Math.max(limit, 1), 100));
    String sql = """
        SELECT rs.id, rs.statut,
               CONCAT('REC-', rs.id) AS reference,
               phs.designation AS pharmacie_source,
               phd.designation AS pharmacie_destination,
               NULL AS fournisseur,
               (SELECT COUNT(*) FROM lignes_reception_stock l WHERE l.fkReceptionStock = rs.id) AS lignes_count,
               rs.datecreate
        FROM reception_stock rs
        INNER JOIN transferts_stock ts ON rs.fkTransfert = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE 1=1
        """ + (statut != null && !statut.isBlank() ? " AND rs.statut = :statut" : "")
        + requisitionScopeFilter(scope, pharmacieId) + """
        ORDER BY rs.datecreate DESC
        LIMIT :limit
        """;
    if (statut != null && !statut.isBlank()) {
      params.put("statut", statut);
    }
    return jdbc.query(sql, params, operationMapper("RECEPTION"));
  }

  public OperationListItemDTO findReceptionHeader(Long id) {
    return findSingleOperation("""
        SELECT rs.id, rs.statut, CONCAT('REC-', rs.id) AS reference,
               phs.designation AS pharmacie_source, phd.designation AS pharmacie_destination,
               NULL AS fournisseur,
               (SELECT COUNT(*) FROM lignes_reception_stock l WHERE l.fkReceptionStock = rs.id) AS lignes_count,
               rs.datecreate
        FROM reception_stock rs
        INNER JOIN transferts_stock ts ON rs.fkTransfert = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
        INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
        WHERE rs.id = :id
        """, id, "RECEPTION");
  }

  public List<OperationLineDTO> findReceptionLines(Long id) {
    return jdbc.query("""
        SELECT ROW_NUMBER() OVER (ORDER BY lrs.id) AS line_num,
               COALESCE(p.nomcommercial, p.nomscientifique) AS produit,
               p.nomscientifique, sp.id AS stock_id, lrs.quantite AS quantite,
               lrs.quantiteDemandee AS qte_demandee, lrs.quantiteTransferee AS qte_transferee, NULL AS prix,
               f.designation AS forme, d.designation AS dosage
        FROM lignes_reception_stock lrs
        INNER JOIN stock_produits sp ON lrs.fkStock = sp.id
        INNER JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN formes f ON p.fkForme = f.id
        LEFT JOIN dosages d ON p.fkDosage = d.id
        WHERE lrs.fkReceptionStock = :id
        ORDER BY lrs.id
        """, Map.of("id", id), lineMapper());
  }

  public List<ProductMovementEventDTO> findMovements(Long stockId, LocalDate from, LocalDate to, int limit) {
    Map<String, Object> params = new HashMap<>();
    params.put("stockId", stockId);
    params.put("from", from);
    params.put("to", to);
    params.put("limit", Math.min(Math.max(limit, 1), 200));
    String sql = """
        SELECT * FROM (
          SELECT 'ENTREE' AS type, a.datebonliv AS date_mouv, la.qt AS quantite,
                 NULL AS stock_apres, COALESCE(a.numbonliv, CONCAT('AP-', a.id)) AS reference,
                 CONCAT('Approvisionnement — ', COALESCE(f.nom, 'Fournisseur')) AS detail,
                 ph.designation AS pharmacie
          FROM lignes_approv la
          INNER JOIN approvsionnements a ON la.fkApprov = a.id
          INNER JOIN pharmacies ph ON a.fkPharmacie = ph.id
          LEFT JOIN fournisseurs f ON a.fkFournisseur = f.id
          WHERE la.fkStock = :stockId AND a.statut = 'VALIDEE'
            AND a.datebonliv BETWEEN :from AND :to
          UNION ALL
          SELECT 'SORTIE', ts.datecreate, lts.quantite, NULL,
                 CONCAT('TRF-', ts.id),
                 CONCAT('Transfert vers ', phd.designation),
                 phs.designation
          FROM lignes_transferts_stock lts
          INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
          INNER JOIN requisitions r ON ts.fkRequisition = r.id
          INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
          INNER JOIN pharmacies phs ON r.fkPharmacieStock = phs.id
          WHERE lts.fkStock = :stockId
            AND ts.statut IN ('TRANSFEREE', 'RECEPTIONNEE')
            AND DATE(ts.datecreate) BETWEEN :from AND :to
          UNION ALL
          SELECT 'ENTREE', rs.datecreate, lrs.quantite, NULL,
                 CONCAT('REC-', rs.id),
                 CONCAT('Réception transfert ', phd.designation),
                 phd.designation
          FROM lignes_reception_stock lrs
          INNER JOIN reception_stock rs ON lrs.fkReceptionStock = rs.id
          INNER JOIN transferts_stock ts ON rs.fkTransfert = ts.id
          INNER JOIN requisitions r ON ts.fkRequisition = r.id
          INNER JOIN pharmacies phd ON r.fkPharmacie = phd.id
          WHERE lrs.fkStock = :stockId AND rs.statut = 'RECEPTIONNEE'
            AND DATE(rs.datecreate) BETWEEN :from AND :to
        ) m
        ORDER BY date_mouv DESC
        LIMIT :limit
        """;
    return jdbc.query(sql, params, movementMapper());
  }

  public Long findStockIdBySearch(String q, Long pharmacieId) {
    Map<String, Object> params = new HashMap<>();
    params.put("q", "%" + q.trim() + "%");
    String sql = """
        SELECT sp.id FROM stock_produits sp
        INNER JOIN produits p ON sp.fkProduits = p.id
        WHERE sp.operationnel = 1
          AND (p.nomcommercial LIKE :q OR p.nomscientifique LIKE :q OR p.codebarre LIKE :q)
        """ + (pharmacieId != null ? " AND sp.fkPharmacies = :pharmacieId\n" : "") + """
        ORDER BY sp.id DESC LIMIT 1
        """;
    if (pharmacieId != null) {
      params.put("pharmacieId", pharmacieId);
    }
    List<Long> ids = jdbc.query(sql, params, (rs, rowNum) -> rs.getLong("id"));
    return ids.isEmpty() ? null : ids.get(0);
  }

  public List<StockAlertMetricDTO> findAlerts(String niveau, Long pharmacieId, int limit) {
    Map<String, Object> params = params(pharmacieId);
    params.put("today", LocalDate.now());
    params.put("limit", Math.min(Math.max(limit, 1), 200));
    String sql = """
        SELECT m.id, m.fkStock, m.fkProduits, m.fkPharmacies,
               COALESCE(p.nomcommercial, p.nomscientifique) AS produit_label,
               ph.designation AS pharmacie_label,
               m.stock_actuel, m.consommation_30j, m.consommation_moyenne_jour,
               m.jours_couverture, m.niveau_alerte, m.message_alerte, m.date_calcul
        FROM stock_alert_metrics m
        INNER JOIN produits p ON m.fkProduits = p.id
        INNER JOIN pharmacies ph ON m.fkPharmacies = ph.id
        WHERE m.date_calcul = :today
        """ + (niveau != null && !niveau.isBlank() ? " AND m.niveau_alerte = :niveau" : "")
        + (pharmacieId != null ? " AND m.fkPharmacies = :pharmacieId\n" : "") + """
        ORDER BY FIELD(m.niveau_alerte, 'RUPTURE','CRITIQUE','SURVEILLANCE','DORMANT','SURSTOCK','NORMAL'), m.jours_couverture ASC
        LIMIT :limit
        """;
    if (niveau != null && !niveau.isBlank()) {
      params.put("niveau", niveau);
    }
    return jdbc.query(sql, params, alertMapper());
  }

  public int recalculateMetrics(Long pharmacieId) {
    ensureDefaultSettings(pharmacieId);
    Map<String, Object> params = params(pharmacieId);
    params.put("today", LocalDate.now());
    String pharmacieClause = pharmacieId != null ? " AND sp.fkPharmacies = :pharmacieId\n" : "";
    String sql = """
        INSERT INTO stock_alert_metrics (
          fkStock, fkProduits, fkPharmacies, stock_actuel,
          consommation_30j, consommation_90j, consommation_365j, consommation_moyenne_jour,
          qte_min, qte_max, stock_securite, jours_couverture, delai_reappro_jour,
          date_derniere_sortie, date_derniere_entree, niveau_alerte, message_alerte, date_calcul
        )
        SELECT
          sp.id, sp.fkProduits, sp.fkPharmacies, sp.qte,
          COALESCE(s30.qty, 0), COALESCE(s90.qty, 0), COALESCE(s365.qty, 0),
          COALESCE(s30.qty, 0) / 30,
          p.qtcritique, p.qtealert, COALESCE(sas.jours_securite, 7) * COALESCE(s30.qty, 0) / 30,
          CASE WHEN COALESCE(s30.qty, 0) > 0 THEN sp.qte / (COALESCE(s30.qty, 0) / 30) ELSE NULL END,
          COALESCE(sas.delai_reappro_jour, 7),
          s365.last_sortie, e365.last_entree,
          CASE
            WHEN sp.qte <= 0 OR sp.qte <= p.qtcritique THEN 'RUPTURE'
            WHEN COALESCE(s30.qty, 0) = 0 AND sp.qte > 0 THEN 'DORMANT'
            WHEN COALESCE(s30.qty, 0) > 0 AND sp.qte / (COALESCE(s30.qty, 0) / 30) < COALESCE(sas.jours_securite, 7) THEN 'CRITIQUE'
            WHEN COALESCE(s30.qty, 0) > 0 AND sp.qte / (COALESCE(s30.qty, 0) / 30) < COALESCE(sas.jours_stock_max, 30) THEN 'SURVEILLANCE'
            WHEN COALESCE(s30.qty, 0) > 0 AND sp.qte / (COALESCE(s30.qty, 0) / 30) > COALESCE(sas.jours_stock_max, 30) * 1.5 THEN 'SURSTOCK'
            ELSE 'NORMAL'
          END,
          CONCAT('Stock ', sp.qte, ' — conso 30j ', COALESCE(s30.qty, 0)),
          :today
        FROM stock_produits sp
        INNER JOIN produits p ON sp.fkProduits = p.id
        LEFT JOIN stock_alert_settings sas ON sas.fkStock = sp.id
        LEFT JOIN (
          SELECT lts.fkStock, SUM(lts.quantite) AS qty
          FROM lignes_transferts_stock lts
          INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
          WHERE ts.statut IN ('TRANSFEREE','RECEPTIONNEE')
            AND ts.datecreate >= CURDATE() - INTERVAL 30 DAY
          GROUP BY lts.fkStock
        ) s30 ON s30.fkStock = sp.id
        LEFT JOIN (
          SELECT lts.fkStock, SUM(lts.quantite) AS qty
          FROM lignes_transferts_stock lts
          INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
          WHERE ts.statut IN ('TRANSFEREE','RECEPTIONNEE')
            AND ts.datecreate >= CURDATE() - INTERVAL 90 DAY
          GROUP BY lts.fkStock
        ) s90 ON s90.fkStock = sp.id
        LEFT JOIN (
          SELECT lts.fkStock, SUM(lts.quantite) AS qty, MAX(ts.datecreate) AS last_sortie
          FROM lignes_transferts_stock lts
          INNER JOIN transferts_stock ts ON lts.fkTransfertStock = ts.id
          WHERE ts.statut IN ('TRANSFEREE','RECEPTIONNEE')
            AND ts.datecreate >= CURDATE() - INTERVAL 365 DAY
          GROUP BY lts.fkStock
        ) s365 ON s365.fkStock = sp.id
        LEFT JOIN (
          SELECT la.fkStock, MAX(a.datebonliv) AS last_entree
          FROM lignes_approv la
          INNER JOIN approvsionnements a ON la.fkApprov = a.id
          WHERE a.statut = 'VALIDEE'
          GROUP BY la.fkStock
        ) e365 ON e365.fkStock = sp.id
        WHERE sp.operationnel = 1
        """ + pharmacieClause + """
        ON DUPLICATE KEY UPDATE
          stock_actuel = VALUES(stock_actuel),
          consommation_30j = VALUES(consommation_30j),
          consommation_90j = VALUES(consommation_90j),
          consommation_365j = VALUES(consommation_365j),
          consommation_moyenne_jour = VALUES(consommation_moyenne_jour),
          qte_min = VALUES(qte_min),
          qte_max = VALUES(qte_max),
          stock_securite = VALUES(stock_securite),
          jours_couverture = VALUES(jours_couverture),
          delai_reappro_jour = VALUES(delai_reappro_jour),
          date_derniere_sortie = VALUES(date_derniere_sortie),
          date_derniere_entree = VALUES(date_derniere_entree),
          niveau_alerte = VALUES(niveau_alerte),
          message_alerte = VALUES(message_alerte),
          dateupdate = CURRENT_TIMESTAMP
        """;
    return jdbc.update(sql, params);
  }

  private void ensureDefaultSettings(Long pharmacieId) {
    Map<String, Object> params = params(pharmacieId);
    String sql = """
        INSERT IGNORE INTO stock_alert_settings (fkStock)
        SELECT sp.id FROM stock_produits sp
        WHERE sp.operationnel = 1
        """ + (pharmacieId != null ? " AND sp.fkPharmacies = :pharmacieId" : "");
    jdbc.update(sql, params);
  }

  private static Map<String, Object> params(Long pharmacieId) {
    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      params.put("pharmacieId", pharmacieId);
    }
    return params;
  }

  private static String pharmacieFilter(String column, Long pharmacieId) {
    return pharmacieId != null ? " AND " + column + " = :pharmacieId" : "";
  }

  private static boolean isCentraleScope(String scope) {
    return scope == null || !"CLIENT".equalsIgnoreCase(scope.trim());
  }

  private static String requisitionScopeFilter(String scope, Long pharmacieId) {
    if (pharmacieId != null) {
      return isCentraleScope(scope)
          ? " AND r.fkPharmacieStock = :pharmacieId\n"
          : " AND r.fkPharmacie = :pharmacieId\n";
    }
    return isCentraleScope(scope)
        ? " AND UPPER(TRIM(phs.typepharmacie)) = 'CENTRALE'\n"
        : " AND UPPER(TRIM(phd.typepharmacie)) IN ('CLIENTE','URGENCE','HOSPITALISATION')\n";
  }

  private static String approvScopeFilter(String scope, Long pharmacieId) {
    if (pharmacieId != null) {
      return " AND a.fkPharmacie = :pharmacieId\n";
    }
    return isCentraleScope(scope)
        ? " AND UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'\n"
        : " AND UPPER(TRIM(ph.typepharmacie)) IN ('CLIENTE','URGENCE','HOSPITALISATION')\n";
  }

  private OperationListItemDTO findSingleOperation(String sql, Long id, String type) {
    List<OperationListItemDTO> rows = jdbc.query(sql, Map.of("id", id), operationMapper(type));
    return rows.isEmpty() ? null : rows.get(0);
  }

  private static RowMapper<OperationLineDTO> lineMapper() {
    return (rs, rowNum) -> new OperationLineDTO(
        rs.getInt("line_num"),
        rs.getString("produit"),
        rs.getString("nomscientifique"),
        rs.getLong("stock_id"),
        rs.getBigDecimal("quantite"),
        rs.getBigDecimal("qte_demandee"),
        rs.getBigDecimal("qte_transferee"),
        rs.getBigDecimal("prix"),
        rs.getString("forme"),
        rs.getString("dosage"));
  }

  private int count(String sql, Map<String, Object> params) {
    Long c = jdbc.queryForObject(sql, params, Long.class);
    return c != null ? c.intValue() : 0;
  }

  private static RowMapper<OperationListItemDTO> operationMapper(String type) {
    return (rs, rowNum) -> new OperationListItemDTO(
        rs.getLong("id"),
        type,
        rs.getString("statut"),
        rs.getString("reference"),
        rs.getString("pharmacie_source"),
        rs.getString("pharmacie_destination"),
        rs.getString("fournisseur"),
        rs.getInt("lignes_count"),
        toLocalDateTime(rs.getTimestamp("datecreate")));
  }

  private static RowMapper<ProductMovementEventDTO> movementMapper() {
    return (rs, rowNum) -> new ProductMovementEventDTO(
        rs.getString("type"),
        toLocalDateTime(rs.getTimestamp("date_mouv")),
        rs.getBigDecimal("quantite"),
        rs.getObject("stock_apres") != null ? rs.getBigDecimal("stock_apres") : null,
        rs.getString("reference"),
        rs.getString("detail"),
        rs.getString("pharmacie"));
  }

  private static RowMapper<StockAlertMetricDTO> alertMapper() {
    return (rs, rowNum) -> new StockAlertMetricDTO(
        rs.getLong("id"),
        rs.getLong("fkStock"),
        rs.getLong("fkProduits"),
        rs.getLong("fkPharmacies"),
        rs.getString("produit_label"),
        rs.getString("pharmacie_label"),
        rs.getBigDecimal("stock_actuel"),
        rs.getBigDecimal("consommation_30j"),
        rs.getBigDecimal("consommation_moyenne_jour"),
        rs.getObject("jours_couverture") != null ? rs.getBigDecimal("jours_couverture") : null,
        rs.getString("niveau_alerte"),
        rs.getString("message_alerte"),
        rs.getDate("date_calcul").toLocalDate());
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime() : null;
  }
}
