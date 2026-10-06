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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Datasource CLINIQUE (SQL Server {@code SVR-THALIA\SQLEXPRESS} / base {@code CLINIQUE}).
 *
 * <p>Optionnelle : activée seulement si {@code cmk.datasource.clinique.url} est non vide.
 * Lecture + UPDATE ciblé de {@code TSTOCK.PAU} uniquement. Pas de DROP / DELETE / DDL.
 */
@Configuration
@ConditionalOnExpression("'${cmk.datasource.clinique.url:}'.trim().length() > 0")
public class CliniqueDataSourceConfig {

  private static final Logger log = LoggerFactory.getLogger(CliniqueDataSourceConfig.class);

  @Value("${platform.jdbc.clinique.fetch-size:100}")
  private int fetchSize;

  @Value("${platform.jdbc.clinique.query-timeout:15}")
  private int queryTimeout;

  @Bean(name = "cliniqueDataSource")
  public DataSource cliniqueDataSource(
      @Value("${cmk.datasource.clinique.url}") String url,
      @Value("${cmk.datasource.clinique.username:}") String username,
      @Value("${cmk.datasource.clinique.password:}") String password,
      @Value("${cmk.datasource.clinique.driver-class-name:com.microsoft.sqlserver.jdbc.SQLServerDriver}") String driver,
      @Value("${cmk.datasource.clinique.pool-name:CMK-ERP-CliniquePool}") String poolName,
      @Value("${cmk.datasource.clinique.maximum-pool-size:3}") int maxPool,
      @Value("${cmk.datasource.clinique.connection-timeout:8000}") long connectionTimeout,
      @Value("${cmk.datasource.clinique.leak-detection-threshold:30000}") long leakDetection) {

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName(driver);
    config.setPoolName(poolName);

    ExternalHikariSupport.applyExternalPoolPolicy(
        config,
        "CLINIQUE",
        maxPool,
        ExternalHikariSupport.HARD_CAP_CLINIQUE,
        connectionTimeout,
        leakDetection,
        false);

    HikariDataSource dataSource = new HikariDataSource(config);
    log.info("Datasource CLINIQUE prête (non-bloquante, PAU update autorisé) -> pool={}, max={}",
        poolName, dataSource.getMaximumPoolSize());
    return dataSource;
  }

  @Bean(name = "cliniqueJdbcTemplate")
  public JdbcTemplate cliniqueJdbcTemplate(@Qualifier("cliniqueDataSource") DataSource dataSource) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.setFetchSize(fetchSize);
    jdbc.setQueryTimeout(queryTimeout);
    return jdbc;
  }

  @Bean(name = "cliniqueNamedParameterJdbcTemplate")
  public NamedParameterJdbcTemplate cliniqueNamedParameterJdbcTemplate(
      @Qualifier("cliniqueJdbcTemplate") JdbcTemplate jdbcTemplate) {
    return new NamedParameterJdbcTemplate(jdbcTemplate);
  }
}
