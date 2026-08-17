package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.CentralPharmacyOptionDTO;

@Repository
public class CentralPharmacyRepository {

  private static final String LIST_CENTRALES_SQL = """
      SELECT
        ph.id,
        ph.designation,
        COUNT(sp.id) AS produits_operationnels
      FROM pharmacies ph
      LEFT JOIN stock_produits sp
        ON sp.fkPharmacies = ph.id AND sp.operationnel = 1
      WHERE UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'
      GROUP BY ph.id, ph.designation
      ORDER BY ph.designation
      """;

  private static final String LIST_CENTRALES_FOR_USER_SQL = """
      SELECT
        ph.id,
        ph.designation,
        COUNT(sp.id) AS produits_operationnels
      FROM pharmacies ph
      INNER JOIN droits_pharmacies dp ON dp.fkPharmacie = ph.id AND dp.fkUtilisateur = :userId
      LEFT JOIN stock_produits sp
        ON sp.fkPharmacies = ph.id AND sp.operationnel = 1
      WHERE UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'
      GROUP BY ph.id, ph.designation
      ORDER BY ph.designation
      """;

  private final NamedParameterJdbcTemplate jdbc;

  public CentralPharmacyRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<CentralPharmacyOptionDTO> findCentralPharmaciesWithStock() {
    return jdbc.query(LIST_CENTRALES_SQL, (rs, rowNum) -> new CentralPharmacyOptionDTO(
        rs.getLong("id"),
        rs.getString("designation"),
        rs.getInt("produits_operationnels")));
  }

  public List<CentralPharmacyOptionDTO> findCentralPharmaciesWithStockForUser(long userId) {
    return jdbc.query(LIST_CENTRALES_FOR_USER_SQL, Map.of("userId", userId), (rs, rowNum) ->
        new CentralPharmacyOptionDTO(
            rs.getLong("id"),
            rs.getString("designation"),
            rs.getInt("produits_operationnels")));
  }
}
