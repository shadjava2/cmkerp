package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Commandes sur stock_produits (quantité, statut opérationnel, etc.).
 */
@Service
@Slf4j
@Transactional
public class StockProduitCommandService {

  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  public StockProduitCommandService(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
    this.namedJdbcTemplate = namedJdbcTemplate;
  }

  /**
   * Active ou désactive un produit pour une pharmacie (stock_produits.operationnel).
   */
  public void setOperationnel(Long stockId, boolean operationnel, Long userId) {
    if (stockId == null) {
      throw new IllegalArgumentException("stockId requis");
    }

    String checkSql = "SELECT COUNT(*) FROM stock_produits WHERE id = :stockId";
    Map<String, Object> checkParams = new HashMap<>();
    checkParams.put("stockId", stockId);
    Integer exists = namedJdbcTemplate.queryForObject(checkSql, checkParams, Integer.class);
    if (exists == null || exists == 0) {
      throw NotFoundException.entity("StockProduit", stockId);
    }

    String sql = """
        UPDATE stock_produits
        SET operationnel = :operationnel,
            dateupdate = CURRENT_TIMESTAMP,
            userupdateid = :userId
        WHERE id = :stockId
        """;

    Map<String, Object> params = new HashMap<>();
    params.put("stockId", stockId);
    params.put("operationnel", operationnel);
    params.put("userId", userId);

    int updated = namedJdbcTemplate.update(sql, params);
    if (updated == 0) {
      throw NotFoundException.entity("StockProduit", stockId);
    }

    log.info("Stock {} : operationnel={}", stockId, operationnel);
  }

  /**
   * Ajuste la quantité actuelle (stock_produits.qte) pour une ligne de stock.
   */
  public void setQte(Long stockId, float qte, Long userId) {
    if (stockId == null) {
      throw new IllegalArgumentException("stockId requis");
    }
    if (qte < 0) {
      throw new IllegalArgumentException("La quantité ne peut pas être négative");
    }

    String checkSql = "SELECT COUNT(*) FROM stock_produits WHERE id = :stockId";
    Map<String, Object> checkParams = new HashMap<>();
    checkParams.put("stockId", stockId);
    Integer exists = namedJdbcTemplate.queryForObject(checkSql, checkParams, Integer.class);
    if (exists == null || exists == 0) {
      throw NotFoundException.entity("StockProduit", stockId);
    }

    String sql = """
        UPDATE stock_produits
        SET qte = :qte,
            dateupdate = CURRENT_TIMESTAMP,
            userupdateid = :userId
        WHERE id = :stockId
        """;

    Map<String, Object> params = new HashMap<>();
    params.put("stockId", stockId);
    params.put("qte", qte);
    params.put("userId", userId);

    int updated = namedJdbcTemplate.update(sql, params);
    if (updated == 0) {
      throw NotFoundException.entity("StockProduit", stockId);
    }

    log.info("Stock {} : qte={}", stockId, qte);
  }
}
