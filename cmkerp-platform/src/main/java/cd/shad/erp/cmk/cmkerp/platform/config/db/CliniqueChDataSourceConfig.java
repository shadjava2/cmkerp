package cd.shad.erp.cmk.cmkerp.platform.config.db;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure.SqlServerProduitJdbcSupport;

/**
 * Deuxième serveur facturation SQL Server (CH — {@code 192.168.1.228}), même schéma CLINIQUE.
 * Optionnel : activé si {@code cmk.datasource.clinique-ch.url} non vide.
 */
@Configuration
@ConditionalOnExpression("'${cmk.datasource.clinique-ch.url:}'.trim().length() > 0")
public class CliniqueChDataSourceConfig {

  private static final Logger log = LoggerFactory.getLogger(CliniqueChDataSourceConfig.class);

  @Value("${platform.jdbc.clinique.fetch-size:100}")
  private int fetchSize;

  @Value("${platform.jdbc.clinique.query-timeout:15}")
  private int queryTimeout;

  @Bean(name = "cliniqueChDataSource")
  public DataSource cliniqueChDataSource(
      @Value("${cmk.datasource.clinique-ch.url}") String url,
      @Value("${cmk.datasource.clinique-ch.username:}") String username,
      @Value("${cmk.datasource.clinique-ch.password:}") String password,
      @Value("${cmk.datasource.clinique-ch.driver-class-name:com.microsoft.sqlserver.jdbc.SQLServerDriver}")
          String driver,
      @Value("${cmk.datasource.clinique-ch.pool-name:CMK-ERP-CliniqueChPool}") String poolName,
      @Value("${cmk.datasource.clinique-ch.maximum-pool-size:3}") int maxPool,
      @Value("${cmk.datasource.clinique-ch.connection-timeout:8000}") long connectionTimeout,
      @Value("${cmk.datasource.clinique-ch.leak-detection-threshold:30000}") long leakDetection) {

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName(driver);
    config.setPoolName(poolName);

    ExternalHikariSupport.applyExternalPoolPolicy(
        config,
        "CLINIQUE-CH",
        maxPool,
        ExternalHikariSupport.HARD_CAP_CLINIQUE,
        connectionTimeout,
        leakDetection,
        false);

    HikariDataSource dataSource = new HikariDataSource(config);
    log.info("Datasource CLINIQUE-CH prête (non-bloquante) -> pool={}, max={}",
        poolName, dataSource.getMaximumPoolSize());
    return dataSource;
  }

  @Bean(name = "cliniqueChJdbcTemplate")
  public JdbcTemplate cliniqueChJdbcTemplate(
      @Qualifier("cliniqueChDataSource") DataSource dataSource) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.setFetchSize(fetchSize);
    jdbc.setQueryTimeout(queryTimeout);
    return jdbc;
  }

  @Bean(name = "cliniqueChProduitSupport")
  public SqlServerProduitJdbcSupport cliniqueChProduitSupport(
      @Qualifier("cliniqueChJdbcTemplate") JdbcTemplate jdbcTemplate) {
    return new SqlServerProduitJdbcSupport(jdbcTemplate, "CLINIQUE-CH");
  }
}
