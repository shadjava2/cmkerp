package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config;

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
 * Crée les tables stock-intelligence sur une base ERP existante sans Flyway complet
 * (schéma legacy incompatible avec les migrations V4+).
 */
@Component
@Order(50)
public class StockIntelligenceSchemaInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StockIntelligenceSchemaInitializer.class);

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  public StockIntelligenceSchemaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!tableExists("stock_intelligence_snapshots")) {
      runScript("db/migration/V12__stock_intelligence_tables.sql", "V12 stock_intelligence_*");
    }
    if (!tableExists("stock_intelligence_email_log")) {
      runScript("db/migration/V15__stock_intelligence_email_log.sql", "V15 stock_intelligence_email_log");
    }
    if (!tableExists("mailingsend")) {
      runScript("db/migration/V14__create_mailingsend_table.sql", "V14 mailingsend");
    } else {
      ensureMailingsendAutoIncrement();
    }
    if (!tableExists("whatsapp_send")) {
      runScript("db/migration/V17__create_whatsapp_send.sql", "V17 whatsapp_send");
    }
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

  private void runScript(String classpathLocation, String label) throws Exception {
    log.info("Stock intelligence DDL : application de {} ({})", label, classpathLocation);
    try (var connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(classpathLocation));
    }
    log.info("Stock intelligence DDL : {} terminé", label);
  }

  private void ensureMailingsendAutoIncrement() {
    String extra = jdbcTemplate.queryForObject(
        """
            SELECT EXTRA FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
              AND table_name = 'mailingsend'
              AND column_name = 'id'
            """,
        String.class);
    if (extra != null && extra.toLowerCase().contains("auto_increment")) {
      return;
    }
    log.info("Stock intelligence DDL : correction AUTO_INCREMENT sur mailingsend.id (V16)");
    jdbcTemplate.execute("ALTER TABLE mailingsend MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
  }
}
