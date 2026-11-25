package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lecture seule TCONTRAT (CLINIQUE Beach). Aucun DML.
 */
@Component
public class ClinicContratLegacyReader {

  private static final Logger log = LoggerFactory.getLogger(ClinicContratLegacyReader.class);

  private final JdbcTemplate cliniqueJdbc;

  public ClinicContratLegacyReader(
      @Autowired(required = false) @Qualifier("cliniqueJdbcTemplate") JdbcTemplate cliniqueJdbc) {
    this.cliniqueJdbc = cliniqueJdbc;
  }

  public boolean available() {
    return cliniqueJdbc != null;
  }

  public List<ClinicContratLegacyRow> listAll() {
    if (cliniqueJdbc == null) {
      return Collections.emptyList();
    }
    try {
      return cliniqueJdbc.query(
          "SELECT RTRIM(CODE) AS CODE, RTRIM(DESIGNATION) AS DESIGNATION FROM dbo.TCONTRAT ORDER BY CODE",
          (rs, i) -> new ClinicContratLegacyRow(
              rs.getString("CODE") == null ? null : rs.getString("CODE").trim(),
              rs.getString("DESIGNATION") == null ? null : rs.getString("DESIGNATION").trim()));
    } catch (Exception ex) {
      log.warn("Lecture TCONTRAT impossible: {}", ex.getMessage());
      throw ex;
    }
  }

  public int count() {
    if (cliniqueJdbc == null) {
      return 0;
    }
    Integer n = cliniqueJdbc.queryForObject("SELECT COUNT(*) FROM dbo.TCONTRAT", Integer.class);
    return n == null ? 0 : n;
  }
}
