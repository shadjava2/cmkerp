package cd.shad.erp.cmk.cmkerp.pos.transferts.application.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Résolution batch des noms produits à partir des IDs stock (évite N+1 JDBC).
 */
@Component("posTransfertInterneProduitLookup")
@Slf4j
public class TransfertInterneProduitLookup {

  private final JdbcTemplate jdbcTemplate;

  public TransfertInterneProduitLookup(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<Long, String> resolveNomsByStockIds(List<Long> stockIds) {
    if (stockIds == null || stockIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> distinctIds =
        stockIds.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
    if (distinctIds.isEmpty()) {
      return Collections.emptyMap();
    }

    String placeholders = distinctIds.stream().map(id -> "?").collect(Collectors.joining(","));
    String sql = "SELECT sp.id, p.nomcommercial FROM stock_produits sp "
        + "INNER JOIN produits p ON sp.fkProduits = p.id WHERE sp.id IN (" + placeholders + ")";

    Map<Long, String> result = new HashMap<>();
    jdbcTemplate.query(sql, rs -> {
      result.put(rs.getLong("id"), rs.getString("nomcommercial"));
    }, distinctIds.toArray());

    return result;
  }
}
