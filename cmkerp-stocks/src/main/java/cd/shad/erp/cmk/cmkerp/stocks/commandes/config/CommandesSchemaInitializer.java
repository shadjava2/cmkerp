package cd.shad.erp.cmk.cmkerp.stocks.commandes.config;

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
 * Applique le DDL du module Commandes sur une base legacy sans Flyway global.
 * Ne bloque jamais le démarrage du gateway en cas d'échec.
 */
@Component
@Order(56)
public class CommandesSchemaInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CommandesSchemaInitializer.class);

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  public CommandesSchemaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      if (!tableExists("demandes_cotation")) {
        runScript("db/migration/V18__create_commandes_module.sql", "V18 commandes module");
      } else {
        ensureApprovLinkColumns();
      }
    } catch (Exception ex) {
      log.warn("Commandes : initialisation DDL non bloquante en échec — {}", ex.getMessage(), ex);
    }
  }

  private void ensureApprovLinkColumns() {
    ensureColumn("approvsionnements", "fk_bon_commande", "BIGINT NULL");
    ensureColumn("approvsionnements", "fk_reception_commande", "BIGINT NULL");
  }

  private void ensureColumn(String table, String column, String definition) {
    if (columnExists(table, column)) {
      return;
    }
    try {
      jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
      log.info("Commandes DDL : colonne {}.{} ajoutée", table, column);
    } catch (Exception ex) {
      log.warn("Commandes DDL : {}.{} — {}", table, column, ex.getMessage());
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

  private boolean columnExists(String tableName, String columnName) {
    Integer count = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
            """,
        Integer.class,
        tableName,
        columnName);
    return count != null && count > 0;
  }

  private void runScript(String classpathLocation, String label) throws Exception {
    log.info("Commandes DDL : application de {} ({})", label, classpathLocation);
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(true);
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(classpathLocation));
    }
    log.info("Commandes DDL : {} terminé", label);
  }
}
