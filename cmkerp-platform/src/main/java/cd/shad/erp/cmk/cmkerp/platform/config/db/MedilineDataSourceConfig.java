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
 * Datasource Mediline / RIS (MySQL {@code production}).
 *
 * <p>Optionnelle : activée seulement si {@code cmk.datasource.mediline.url} est non vide.
 * Aucune migration Flyway. Pool séparé, {@code autoCommit=true} (pas de TX manager MySQL).
 * Ne pas JOINER avec cmkerp-v24prod.
 */
@Configuration
@ConditionalOnExpression("'${cmk.datasource.mediline.url:}'.trim().length() > 0")
public class MedilineDataSourceConfig {

  private static final Logger log = LoggerFactory.getLogger(MedilineDataSourceConfig.class);

  @Value("${platform.jdbc.mediline.fetch-size:250}")
  private int fetchSize;

  @Value("${platform.jdbc.mediline.query-timeout:30}")
  private int queryTimeout;

  @Bean(name = "medilineDataSource")
  public DataSource medilineDataSource(
      @Value("${cmk.datasource.mediline.url}") String url,
      @Value("${cmk.datasource.mediline.username:}") String username,
      @Value("${cmk.datasource.mediline.password:}") String password,
      @Value("${cmk.datasource.mediline.driver-class-name:com.mysql.cj.jdbc.Driver}") String driver,
      @Value("${cmk.datasource.mediline.pool-name:CMK-ERP-MedilinePool}") String poolName,
      @Value("${cmk.datasource.mediline.maximum-pool-size:10}") int maxPool,
      @Value("${cmk.datasource.mediline.minimum-idle:2}") int minIdle,
      @Value("${cmk.datasource.mediline.connection-timeout:30000}") long connectionTimeout,
      @Value("${cmk.datasource.mediline.leak-detection-threshold:60000}") long leakDetection) {

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName(driver);
    config.setPoolName(poolName);
    config.setMaximumPoolSize(maxPool);
    config.setMinimumIdle(minIdle);
    config.setConnectionTimeout(connectionTimeout);
    config.setLeakDetectionThreshold(leakDetection);
    config.setRegisterMbeans(false);
    // Obligatoire : @Transactional ne couvre que la primaire cmkerp
    config.setAutoCommit(true);
    config.setConnectionTestQuery("SELECT 1");
    // Ne jamais bloquer le boot si Mediline est down / mal configuré
    config.setInitializationFailTimeout(-1);
    config.setMinimumIdle(0);

    HikariDataSource dataSource = new HikariDataSource(config);
    log.info("Datasource Mediline initialisée (non-bloquante) -> pool={}, max={}", poolName, maxPool);
    return dataSource;
  }

  @Bean(name = "medilineJdbcTemplate")
  public JdbcTemplate medilineJdbcTemplate(@Qualifier("medilineDataSource") DataSource dataSource) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.setFetchSize(fetchSize);
    jdbc.setQueryTimeout(queryTimeout);
    return jdbc;
  }

  @Bean(name = "medilineNamedParameterJdbcTemplate")
  public NamedParameterJdbcTemplate medilineNamedParameterJdbcTemplate(
      @Qualifier("medilineJdbcTemplate") JdbcTemplate jdbcTemplate) {
    return new NamedParameterJdbcTemplate(jdbcTemplate);
  }
}
