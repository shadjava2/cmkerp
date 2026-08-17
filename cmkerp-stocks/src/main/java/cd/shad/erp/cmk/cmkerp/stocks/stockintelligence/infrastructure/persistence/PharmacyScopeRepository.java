package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PharmacyScopeOptionDTO;

@Repository
public class PharmacyScopeRepository {

  private static final String RIGHTS_JOIN = """
      INNER JOIN droits_pharmacies dp_rights ON dp_rights.fkPharmacie = ph.id
        AND dp_rights.fkUtilisateur = :userId
      """;

  private static final String LIST_CENTRALES = """
      SELECT ph.id, ph.designation, ph.typepharmacie,
             COUNT(sp.id) AS produits_operationnels
      FROM pharmacies ph
      LEFT JOIN stock_produits sp ON sp.fkPharmacies = ph.id AND sp.operationnel = 1
      WHERE UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'
      """ + CentralPharmacyExclusions.SQL_NOT_IN + """
      GROUP BY ph.id, ph.designation, ph.typepharmacie
      ORDER BY ph.designation
      """;

  private static final String LIST_CENTRALES_FOR_USER = """
      SELECT ph.id, ph.designation, ph.typepharmacie,
             COUNT(sp.id) AS produits_operationnels
      FROM pharmacies ph
      """
      + RIGHTS_JOIN + """
      LEFT JOIN stock_produits sp ON sp.fkPharmacies = ph.id AND sp.operationnel = 1
      WHERE UPPER(TRIM(ph.typepharmacie)) = 'CENTRALE'
      """ + CentralPharmacyExclusions.SQL_NOT_IN + """
      GROUP BY ph.id, ph.designation, ph.typepharmacie
      ORDER BY ph.designation
      """;

  private static final String LIST_CLIENTS = """
      SELECT ph.id, ph.designation, ph.typepharmacie,
             COUNT(sp.id) AS produits_operationnels
      FROM pharmacies ph
      LEFT JOIN stock_produits sp ON sp.fkPharmacies = ph.id AND sp.operationnel = 1
      WHERE UPPER(TRIM(ph.typepharmacie)) IN ('CLIENTE', 'URGENCE', 'HOSPITALISATION')
      GROUP BY ph.id, ph.designation, ph.typepharmacie
      ORDER BY ph.designation
      """;

  private static final String LIST_CLIENTS_FOR_USER = """
      SELECT ph.id, ph.designation, ph.typepharmacie,
             COUNT(sp.id) AS produits_operationnels
      FROM pharmacies ph
      """
      + RIGHTS_JOIN + """
      LEFT JOIN stock_produits sp ON sp.fkPharmacies = ph.id AND sp.operationnel = 1
      WHERE UPPER(TRIM(ph.typepharmacie)) IN ('CLIENTE', 'URGENCE', 'HOSPITALISATION')
      GROUP BY ph.id, ph.designation, ph.typepharmacie
      ORDER BY ph.designation
      """;

  private final NamedParameterJdbcTemplate jdbc;

  public PharmacyScopeRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<PharmacyScopeOptionDTO> findCentralPharmacies() {
    return jdbc.query(LIST_CENTRALES, mapper());
  }

  public List<PharmacyScopeOptionDTO> findCentralPharmaciesForUser(long userId) {
    return jdbc.query(LIST_CENTRALES_FOR_USER, Map.of("userId", userId), mapper());
  }

  public List<PharmacyScopeOptionDTO> findClientPharmacies() {
    return jdbc.query(LIST_CLIENTS, mapper());
  }

  public List<PharmacyScopeOptionDTO> findClientPharmaciesForUser(long userId) {
    return jdbc.query(LIST_CLIENTS_FOR_USER, Map.of("userId", userId), mapper());
  }

  public String resolveDesignation(Long pharmacieId) {
    if (pharmacieId == null) {
      return null;
    }
    List<String> names = jdbc.query(
        "SELECT designation FROM pharmacies WHERE id = :id",
        Map.of("id", pharmacieId),
        (rs, rowNum) -> rs.getString("designation"));
    return names.isEmpty() ? null : names.get(0);
  }

  private static org.springframework.jdbc.core.RowMapper<PharmacyScopeOptionDTO> mapper() {
    return (rs, rowNum) -> new PharmacyScopeOptionDTO(
        rs.getLong("id"),
        rs.getString("designation"),
        rs.getString("typepharmacie"),
        rs.getInt("produits_operationnels"));
  }
}
