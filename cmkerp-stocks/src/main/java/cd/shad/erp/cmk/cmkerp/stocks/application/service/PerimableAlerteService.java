package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.AddPerimableAlerteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.RetirerStockExpireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.InventaireLotEntryRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.PerimableAlerteStockResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour gérer les alertes de péremption. Désactive automatiquement les alertes quand le
 * stock passe à 0.
 */
@Service
@Slf4j
@Transactional
public class PerimableAlerteService {

  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  public PerimableAlerteService(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
    this.namedJdbcTemplate = namedJdbcTemplate;
  }

  /**
   * Désactive toutes les alertes de péremption actives pour un stock donné. Utilisé quand le stock
   * passe à 0.
   *
   * @param stockId ID du stock (stock_produits.id)
   */
  public void desactiverAlertesPourStock(Long stockId) {
    if (stockId == null) {
      log.warn("Tentative de désactivation des alertes avec stockId null");
      return;
    }

    log.debug("Désactivation des alertes de péremption pour stockId: {}", stockId);

    String sql = """
        UPDATE perimable_alerte_stock
        SET notifactif = FALSE,
            dateupdate = CURRENT_TIMESTAMP
        WHERE fkStock = :stockId
          AND notifactif = TRUE
        """;

    Map<String, Object> params = new HashMap<>();
    params.put("stockId", stockId);

    int rowsUpdated = namedJdbcTemplate.update(sql, params);

    if (rowsUpdated > 0) {
      log.info("{} alerte(s) de péremption désactivée(s) pour stockId: {}", rowsUpdated, stockId);
    } else {
      log.debug("Aucune alerte active trouvée pour stockId: {}", stockId);
    }
  }

  /**
   * Désactive toutes les alertes de péremption actives pour tous les stocks avec quantité = 0.
   * Méthode utilitaire pour nettoyer les alertes orphelines.
   */
  public void desactiverAlertesPourStocksZero() {
    log.debug("Désactivation des alertes de péremption pour tous les stocks à zéro");

    String sql = """
        UPDATE perimable_alerte_stock pas
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        SET pas.notifactif = FALSE,
            pas.dateupdate = CURRENT_TIMESTAMP
        WHERE st.qte = 0
          AND pas.notifactif = TRUE
        """;

    int rowsUpdated = namedJdbcTemplate.update(sql, new HashMap<>());

    if (rowsUpdated > 0) {
      log.info("{} alerte(s) de péremption désactivée(s) pour les stocks à zéro", rowsUpdated);
    } else {
      log.debug("Aucune alerte active trouvée pour les stocks à zéro");
    }
  }

  /**
   * Ajoute une alerte de péremption avec approv = false. Le fkStock dans la requête peut être soit
   * l'ID du stock, soit l'ID du produit. Si c'est l'ID du produit, trouve automatiquement le
   * stockId correspondant.
   *
   * @param request DTO contenant fkStock (peut être produit.id ou stock.id), fkAprov (optionnel) et
   *        dateperemtion
   * @param userId ID de l'utilisateur qui effectue l'action
   * @return ID de l'alerte créée
   */
  public Long addPerimableAlerte(AddPerimableAlerteRequest request, Long userId) {
    Long stockId = request.getFkStock();

    // Vérifier si fkStock est un ID de stock ou un ID de produit
    // Si c'est un ID de produit, trouver le stockId correspondant
    String checkStockSql = """
        SELECT id FROM stock_produits WHERE id = :id
        """;
    Map<String, Object> checkParams = new HashMap<>();
    checkParams.put("id", stockId);

    try {
      Long foundStockId = namedJdbcTemplate.queryForObject(checkStockSql, checkParams, Long.class);
      // Si trouvé, c'est un ID de stock, utiliser directement
      stockId = foundStockId;
      log.debug("ID de stock détecté directement: {}", stockId);
    } catch (Exception e) {
      // Si pas trouvé, c'est probablement un ID de produit, chercher le stock correspondant
      log.debug("ID {} n'est pas un stock, recherche comme produit...", request.getFkStock());
      String findStockFromProduitSql = """
          SELECT id FROM stock_produits WHERE fkProduits = :produitId
          """;
      Map<String, Object> produitParams = new HashMap<>();
      produitParams.put("produitId", stockId);
      try {
        stockId =
            namedJdbcTemplate.queryForObject(findStockFromProduitSql, produitParams, Long.class);
        log.info("✅ ID de produit {} converti en stockId: {}", request.getFkStock(), stockId);
      } catch (Exception ex) {
        log.error("❌ Impossible de trouver le stock pour l'ID (produit ou stock): {}",
            request.getFkStock());
        log.error("Erreur lors de la recherche du stock: {}", ex.getMessage());
        throw new IllegalArgumentException(
            "Stock introuvable pour l'ID fourni: " + request.getFkStock()
                + ". Vérifiez que le produit a un stock associé dans stock_produits.");
      }
    }

    log.debug("Ajout d'une alerte de péremption pour stockId: {}, date: {}", stockId,
        request.getDateperemtion());

    // Vérifier que le stock existe
    String verifyStockSql = """
        SELECT COUNT(*) FROM stock_produits WHERE id = :stockId
        """;
    Map<String, Object> verifyParams = new HashMap<>();
    verifyParams.put("stockId", stockId);
    Integer stockExists =
        namedJdbcTemplate.queryForObject(verifyStockSql, verifyParams, Integer.class);

    if (stockExists == null || stockExists == 0) {
      log.error("Le stock avec l'ID {} n'existe pas", stockId);
      throw new IllegalArgumentException("Stock introuvable avec l'ID: " + stockId);
    }

    String sql =
        """
            INSERT INTO perimable_alerte_stock
                (fkStock, fkAprov, lot, dateperemtion, notifactif, approv, stockexpiree, datecreate, usercreateid)
            VALUES
                (:fkStock, :fkAprov, :lot, :dateperemtion, TRUE, FALSE, :stockexpiree, CURRENT_TIMESTAMP, :userId)
            """;

    Map<String, Object> params = new HashMap<>();
    params.put("fkStock", stockId);
    params.put("fkAprov", request.getFkAprov());
    params.put("lot", request.getLot());
    params.put("dateperemtion", request.getDateperemtion());
    params.put("stockexpiree", request.getStockexpiree() != null ? request.getStockexpiree() : 0f);
    params.put("userId", userId);

    int rowsInserted = namedJdbcTemplate.update(sql, params);

    if (rowsInserted == 0) {
      log.error("Aucune ligne insérée dans perimable_alerte_stock pour stockId: {}, date: {}",
          stockId, request.getDateperemtion());
      throw new IllegalStateException("Échec de l'insertion de l'alerte de péremption");
    }

    log.debug("{} ligne(s) insérée(s) dans perimable_alerte_stock", rowsInserted);

    // Récupérer l'ID de l'alerte créée
    String selectIdSql = """
        SELECT id FROM perimable_alerte_stock
        WHERE fkStock = :fkStock
          AND dateperemtion = :dateperemtion
          AND approv = FALSE
          AND notifactif = TRUE
        ORDER BY id DESC
        LIMIT 1
        """;

    Long alerteId = namedJdbcTemplate.queryForObject(selectIdSql, params, Long.class);

    if (alerteId == null) {
      log.error("Impossible de récupérer l'ID de l'alerte créée pour stockId: {}, date: {}",
          stockId, request.getDateperemtion());
      throw new IllegalStateException("Impossible de récupérer l'ID de l'alerte créée");
    }

    log.info("Alerte de péremption créée avec ID: {} pour stockId: {}, date: {}", alerteId, stockId,
        request.getDateperemtion());
    return alerteId;
  }

  /**
   * Retire du stock périmé en mettant notifactif = false et en définissant stockexpiree. Si la
   * quantité est zéro, désactive toutes les alertes de péremption pour ce produit. Le fkStock dans
   * la requête peut être soit l'ID du stock, soit l'ID du produit.
   *
   * @param request DTO contenant fkStock (peut être produit.id ou stock.id) et stockexpiree
   * @param userId ID de l'utilisateur qui effectue l'action
   */
  public void retirerStockExpire(RetirerStockExpireRequest request, Long userId) {
    Long stockId = request.getFkStock();

    // Vérifier si fkStock est un ID de stock ou un ID de produit
    String checkStockSql = """
        SELECT id FROM stock_produits WHERE id = :id
        """;
    Map<String, Object> checkParams = new HashMap<>();
    checkParams.put("id", stockId);

    try {
      Long foundStockId = namedJdbcTemplate.queryForObject(checkStockSql, checkParams, Long.class);
      stockId = foundStockId;
    } catch (Exception e) {
      // Si pas trouvé, c'est probablement un ID de produit, chercher le stock correspondant
      String findStockFromProduitSql = """
          SELECT id FROM stock_produits WHERE fkProduits = :produitId
          """;
      Map<String, Object> produitParams = new HashMap<>();
      produitParams.put("produitId", stockId);
      try {
        stockId =
            namedJdbcTemplate.queryForObject(findStockFromProduitSql, produitParams, Long.class);
        log.debug("ID de produit détecté, stockId trouvé: {}", stockId);
      } catch (Exception ex) {
        log.error("Impossible de trouver le stock pour l'ID: {}", request.getFkStock());
        throw new IllegalArgumentException(
            "Stock introuvable pour l'ID fourni: " + request.getFkStock());
      }
    }

    log.debug("Retrait de stock expiré pour stockId: {}, quantité: {}", stockId,
        request.getStockexpiree());

    // Vérifier si le stock est à zéro
    String checkStockQteSql = """
        SELECT qte FROM stock_produits WHERE id = :fkStock
        """;
    Map<String, Object> checkQteParams = new HashMap<>();
    checkQteParams.put("fkStock", stockId);
    Float currentStock =
        namedJdbcTemplate.queryForObject(checkStockQteSql, checkQteParams, Float.class);

    if (currentStock == null || currentStock == 0) {
      log.info("Stock à zéro pour stockId: {}, désactivation de toutes les alertes de péremption",
          stockId);
      desactiverAlertesPourStock(stockId);
      return;
    }

    String countActiveSql = """
        SELECT COUNT(*) FROM perimable_alerte_stock
        WHERE fkStock = :fkStock AND notifactif = TRUE
        """;
    Map<String, Object> countParams = new HashMap<>();
    countParams.put("fkStock", stockId);
    Integer activeAlertCount =
        namedJdbcTemplate.queryForObject(countActiveSql, countParams, Integer.class);
    int activeCount = activeAlertCount != null ? activeAlertCount : 0;

    float qtyToRetire = request.getStockexpiree() != null ? request.getStockexpiree() : 0f;
    // Une seule date active : tout le stock est retiré automatiquement
    if (activeCount == 1) {
      qtyToRetire = currentStock;
    }

    // Mettre à jour les alertes actives avec notifactif = false et stockexpiree
    // Si dateperemtion est fournie, ne mettre à jour que l'alerte avec cette date
    // Sinon, mettre à jour toutes les alertes actives pour ce stock
    StringBuilder sqlBuilder = new StringBuilder("""
        UPDATE perimable_alerte_stock
        SET notifactif = FALSE,
            stockexpiree = :stockexpiree,
            dateupdate = CURRENT_TIMESTAMP,
            userupdateid = :userId
        WHERE fkStock = :fkStock
          AND notifactif = TRUE
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("fkStock", stockId);
    params.put("stockexpiree", qtyToRetire);
    params.put("userId", userId);

    // Si une date de péremption est spécifiée, filtrer par cette date
    if (request.getDateperemtion() != null) {
      sqlBuilder.append(" AND dateperemtion = :dateperemtion");
      params.put("dateperemtion", request.getDateperemtion());
      log.debug("Retrait du stock expiré pour stockId: {}, date: {}, quantité: {}", stockId,
          request.getDateperemtion(), qtyToRetire);
    } else {
      log.debug("Retrait du stock expiré pour stockId: {}, toutes les dates, quantité: {}", stockId,
          qtyToRetire);
    }

    String sql = sqlBuilder.toString();

    int rowsUpdated = namedJdbcTemplate.update(sql, params);

    if (rowsUpdated > 0) {
      // Le stock physique est décrémenté par le trigger MySQL trigger_perimable
      // → CALL update_stock_quantity(NEW.fkStock, NEW.stockexpiree)
      log.info("{} alerte(s) de péremption mise(s) à jour pour stockId: {}, quantité retirée: {}",
          rowsUpdated, stockId, qtyToRetire);
    } else {
      log.debug("Aucune alerte active trouvée pour stockId: {}", stockId);
    }
  }

  /**
   * Met à jour la date de péremption d'une alerte active.
   */
  public void updateDate(Long alerteId, java.time.LocalDate newDate, Long userId) {
    if (alerteId == null) {
      throw new IllegalArgumentException("alerteId requis");
    }
    if (newDate == null) {
      throw new IllegalArgumentException("dateperemtion requise");
    }

    String checkSql = """
        SELECT COUNT(*) FROM perimable_alerte_stock
        WHERE id = :id AND notifactif = TRUE
        """;
    Map<String, Object> checkParams = new HashMap<>();
    checkParams.put("id", alerteId);
    Integer exists = namedJdbcTemplate.queryForObject(checkSql, checkParams, Integer.class);
    if (exists == null || exists == 0) {
      throw new IllegalArgumentException("Alerte de péremption introuvable ou inactive: " + alerteId);
    }

    String sql = """
        UPDATE perimable_alerte_stock
        SET dateperemtion = :dateperemtion,
            dateupdate = CURRENT_TIMESTAMP,
            userupdateid = :userId
        WHERE id = :id AND notifactif = TRUE
        """;

    Map<String, Object> params = new HashMap<>();
    params.put("id", alerteId);
    params.put("dateperemtion", newDate);
    params.put("userId", userId);

    int updated = namedJdbcTemplate.update(sql, params);
    if (updated == 0) {
      throw new IllegalStateException("Échec de la mise à jour de l'alerte " + alerteId);
    }

    log.info("Alerte péremption {} : nouvelle date {}", alerteId, newDate);
  }

  /**
   * Liste les alertes actives d'un stock (lot + péremption + quantité).
   */
  public List<PerimableAlerteStockResponse> listActiveByStock(Long stockId) {
    if (stockId == null) {
      return List.of();
    }
    String sql = """
        SELECT id, fkStock, lot, dateperemtion, stockexpiree
        FROM perimable_alerte_stock
        WHERE fkStock = :stockId AND notifactif = TRUE
        ORDER BY dateperemtion ASC, lot ASC
        """;
    Map<String, Object> params = new HashMap<>();
    params.put("stockId", stockId);
    return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> PerimableAlerteStockResponse.builder()
        .id(rs.getLong("id"))
        .fkStock(rs.getLong("fkStock"))
        .lot(rs.getString("lot"))
        .dateperemtion(rs.getObject("dateperemtion", LocalDate.class))
        .stockexpiree(rs.getObject("stockexpiree") != null ? rs.getFloat("stockexpiree") : 0f)
        .build());
  }

  /** Liste les alertes actives par pharmacie (via stock_produits). */
  public List<Map<String, Object>> listActiveByPharmacie(Long pharmacieId) {
    String sql =
        """
        SELECT pas.id, pas.fkStock, pas.lot, pas.dateperemtion, pas.stockexpiree,
               sp.fkPharmacies AS pharmacieId, ph.designation AS pharmacieNom,
               p.nomcommercial AS produitNom, sp.qte AS stockQte
        FROM perimable_alerte_stock pas
        INNER JOIN stock_produits sp ON sp.id = pas.fkStock
        INNER JOIN produits p ON p.id = sp.fkProduits
        LEFT JOIN pharmacies ph ON ph.id = sp.fkPharmacies
        WHERE pas.notifactif = TRUE
          AND (:pharmacieId IS NULL OR sp.fkPharmacies = :pharmacieId)
        ORDER BY pas.dateperemtion ASC
        LIMIT 500
        """;
    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    return namedJdbcTemplate.queryForList(sql, params);
  }

  /**
   * Enregistre les lots saisis à l'inventaire : remplace les alertes actives du stock
   * et retourne la somme des quantités (quantité physique inventoriée).
   */
  public float saveInventaireLots(Long stockId, List<InventaireLotEntryRequest> entrees, Long userId) {
    if (stockId == null) {
      throw new IllegalArgumentException("stockId requis");
    }
    if (entrees == null || entrees.isEmpty()) {
      throw new IllegalArgumentException("Au moins une ligne est requise");
    }

    String deactivateSql = """
        UPDATE perimable_alerte_stock
        SET notifactif = FALSE,
            dateupdate = CURRENT_TIMESTAMP,
            userupdateid = :userId
        WHERE fkStock = :stockId AND notifactif = TRUE
        """;
    Map<String, Object> deactivateParams = new HashMap<>();
    deactivateParams.put("stockId", stockId);
    deactivateParams.put("userId", userId);
    namedJdbcTemplate.update(deactivateSql, deactivateParams);

    String insertSql = """
        INSERT INTO perimable_alerte_stock
            (fkStock, fkAprov, lot, dateperemtion, notifactif, approv, stockexpiree, datecreate, usercreateid)
        VALUES
            (:fkStock, NULL, :lot, :dateperemtion, TRUE, FALSE, :stockexpiree, CURRENT_TIMESTAMP, :userId)
        """;

    float total = 0f;
    for (InventaireLotEntryRequest entry : entrees) {
      if (entry.getDateperemtion() == null) {
        throw new IllegalArgumentException("Date de péremption requise pour chaque ligne");
      }
      float qty = entry.getQuantite() != null ? entry.getQuantite() : 0f;
      if (qty < 0) {
        throw new IllegalArgumentException("Quantité invalide");
      }
      total += qty;

      Map<String, Object> insertParams = new HashMap<>();
      insertParams.put("fkStock", stockId);
      insertParams.put("lot", entry.getLot() != null && !entry.getLot().isBlank() ? entry.getLot().trim() : null);
      insertParams.put("dateperemtion", entry.getDateperemtion());
      insertParams.put("stockexpiree", qty);
      insertParams.put("userId", userId);
      namedJdbcTemplate.update(insertSql, insertParams);
    }

    log.info("Inventaire lots enregistrés pour stockId={}, {} ligne(s), total={}", stockId, entrees.size(), total);
    return total;
  }
}

