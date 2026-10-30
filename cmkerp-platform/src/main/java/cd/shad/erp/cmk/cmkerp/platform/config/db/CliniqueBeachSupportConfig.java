package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure.SqlServerProduitJdbcSupport;

/** Support JDBC Beach (datasource CLINIQUE existante) pour sync facturation. */
@Configuration
@ConditionalOnBean(name = "cliniqueJdbcTemplate")
public class CliniqueBeachSupportConfig {

  @Bean(name = "cliniqueBeachProduitSupport")
  public SqlServerProduitJdbcSupport cliniqueBeachProduitSupport(
      @Qualifier("cliniqueJdbcTemplate") JdbcTemplate jdbcTemplate) {
    return new SqlServerProduitJdbcSupport(jdbcTemplate, "CLINIQUE-BEACH");
  }
}
