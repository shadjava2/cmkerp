package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserPharmacyScopeRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public UserPharmacyScopeRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Long> findAllowedPharmacyIds(long userId) {
    return jdbc.query(
        """
        SELECT DISTINCT dp.fkPharmacie
        FROM droits_pharmacies dp
        WHERE dp.fkUtilisateur = :userId
        ORDER BY dp.fkPharmacie
        """,
        Map.of("userId", userId),
        (rs, rowNum) -> rs.getLong("fkPharmacie"));
  }

  public boolean hasAccess(long userId, long pharmacieId) {
    Integer count = jdbc.queryForObject(
        """
        SELECT COUNT(*)
        FROM droits_pharmacies
        WHERE fkUtilisateur = :userId AND fkPharmacie = :pharmacieId
        """,
        Map.of("userId", userId, "pharmacieId", pharmacieId),
        Integer.class);
    return count != null && count > 0;
  }
}
