package cd.shad.erp.cmk.cmkerp.stocks.usage.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * Crée {@code app_usage_events} quand Flyway est désactivé (base legacy).
 */
@Component
@Order(55)
public class AppUsageSchemaInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AppUsageSchemaInitializer.class);

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  public AppUsageSchemaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (tableExists("app_usage_events")) {
      return;
    }
    log.info("App usage DDL : application de V20 app_usage_events");
    try (var connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/migration/V20__create_app_usage_events.sql"));
    }
    log.info("App usage DDL : V20 terminé");
  }

  private boolean tableExists(String tableName) {
    Integer count = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = ?
            """,
        Integer.class,
        tableName);
    return count != null && count > 0;
  }
}
