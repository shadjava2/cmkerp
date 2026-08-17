package cd.shad.erp.cmk.cmkerp.platform.config.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Configurateur du slow query log MySQL au démarrage de l'application.
 *
 * <p>
 * Cette classe configure automatiquement le slow query log MySQL pour :
 * <ul>
 * <li>Activer le slow query log</li>
 * <li>Définir le seuil de temps (long_query_time) pour considérer une requête comme lente</li>
 * <li>Configurer la rétention des logs (7 jours par défaut)</li>
 * <li>Activer l'enregistrement des requêtes sans index (log_queries_not_using_indexes)</li>
 * </ul>
 *
 * <p>
 * Configuration via application.yml :
 *
 * <pre>{@code
 * cmkerp:
 *   db:
 *     slow-query-log:
 *       enabled: true
 *       long-query-time-seconds: 2  # Seuil en secondes (défaut: 2s)
 *       retention-days: 7  # Rétention des logs en jours (défaut: 7)
 *       log-queries-not-using-indexes: true  # Logger les requêtes sans index
 * }</pre>
 *
 * <p>
 * Le slow query log est écrit dans le fichier défini par la variable MySQL
 * {@code slow_query_log_file} (généralement dans le répertoire de données MySQL).
 */
@Component
public class MySQLSlowQueryLogConfigurator implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(MySQLSlowQueryLogConfigurator.class);

  private final DataSource primaryDataSource;

  @Value("${cmkerp.db.slow-query-log.enabled:true}")
  private boolean enabled;

  @Value("${cmkerp.db.slow-query-log.long-query-time-seconds:2}")
  private int longQueryTimeSeconds;

  @Value("${cmkerp.db.slow-query-log.retention-days:7}")
  private int retentionDays;

  @Value("${cmkerp.db.slow-query-log.log-queries-not-using-indexes:true}")
  private boolean logQueriesNotUsingIndexes;

  public MySQLSlowQueryLogConfigurator(
      @Qualifier("primaryDataSource") DataSource primaryDataSource) {
    this.primaryDataSource = primaryDataSource;
  }

  @Override
  public void run(String... args) {
    if (!enabled) {
      log.info("Slow query log désactivé via configuration");
      return;
    }

    try {
      configureSlowQueryLog();
    } catch (Exception e) {
      log.error("Erreur lors de la configuration du slow query log MySQL : {}", e.getMessage(), e);
      // Ne pas bloquer le démarrage, juste logger l'erreur
    }
  }

  /**
   * Configure le slow query log MySQL.
   */
  private void configureSlowQueryLog() {
    try (Connection connection = primaryDataSource.getConnection();
        Statement statement = connection.createStatement()) {

      log.info("=== Configuration MySQL Slow Query Log ===");

      // Vérifier si le slow query log est déjà activé
      String currentSlowQueryLog = getMySQLVariable(statement, "slow_query_log");
      String currentLongQueryTime = getMySQLVariable(statement, "long_query_time");
      String currentLogQueriesNotUsingIndexes =
          getMySQLVariable(statement, "log_queries_not_using_indexes");
      String currentSlowQueryLogFile = getMySQLVariable(statement, "slow_query_log_file");

      log.info("État actuel :");
      log.info("  - slow_query_log: {}", currentSlowQueryLog);
      log.info("  - long_query_time: {}s", currentLongQueryTime);
      log.info("  - log_queries_not_using_indexes: {}", currentLogQueriesNotUsingIndexes);
      log.info("  - slow_query_log_file: {}", currentSlowQueryLogFile);

      // Activer le slow query log (seulement si pas déjà activé)
      if (!"ON".equalsIgnoreCase(currentSlowQueryLog)) {
        statement.execute("SET GLOBAL slow_query_log = 'ON'");
        log.info("✓ Slow query log activé");
      } else {
        log.info("✓ Slow query log déjà activé");
      }

      // Configurer le seuil de temps (seulement si différent)
      double currentLongQueryTimeValue =
          currentLongQueryTime != null ? Double.parseDouble(currentLongQueryTime) : -1;
      if (Math.abs(currentLongQueryTimeValue - longQueryTimeSeconds) > 0.01) {
        statement.execute("SET GLOBAL long_query_time = " + longQueryTimeSeconds);
        log.info("✓ long_query_time configuré à {} secondes (était: {}s)", longQueryTimeSeconds,
            currentLongQueryTime);
      } else {
        log.info("✓ long_query_time déjà configuré à {} secondes", longQueryTimeSeconds);
      }

      // Activer l'enregistrement des requêtes sans index (seulement si différent)
      boolean currentLogQueriesNotUsingIndexesValue =
          "ON".equalsIgnoreCase(currentLogQueriesNotUsingIndexes);
      if (logQueriesNotUsingIndexes != currentLogQueriesNotUsingIndexesValue) {
        if (logQueriesNotUsingIndexes) {
          statement.execute("SET GLOBAL log_queries_not_using_indexes = 'ON'");
          log.info("✓ log_queries_not_using_indexes activé");
        } else {
          statement.execute("SET GLOBAL log_queries_not_using_indexes = 'OFF'");
          log.info("✓ log_queries_not_using_indexes désactivé");
        }
      } else {
        log.info("✓ log_queries_not_using_indexes déjà configuré à {}",
            logQueriesNotUsingIndexes ? "ON" : "OFF");
      }

      // Afficher les informations de rétention
      log.info("Configuration de rétention :");
      log.info("  - Rétention configurée : {} jours", retentionDays);
      log.info("  - Note : La rotation des logs doit être configurée au niveau MySQL");
      log.info("    (via logrotate ou script de nettoyage automatique)");
      log.info("  - Fichier de log : {}", currentSlowQueryLogFile);

      // Recommandations
      log.info("Recommandations :");
      log.info("  1. Configurer logrotate pour rotation automatique des logs");
      log.info("  2. Analyser régulièrement les logs avec mysqldumpslow ou pt-query-digest");
      log.info("  3. Surveiller la taille du fichier de log");
      log.info("  4. Exemple de commande d'analyse :");
      log.info("     mysqldumpslow -s t -t 10 {} | head -20", currentSlowQueryLogFile);

      log.info("==========================================");

    } catch (Exception e) {
      log.error("Erreur lors de la configuration du slow query log : {}", e.getMessage(), e);
      // Ne pas bloquer le démarrage
    }
  }

  /**
   * Récupère une variable MySQL via SHOW VARIABLES.
   *
   * @param statement le Statement SQL
   * @param variableName le nom de la variable MySQL
   * @return la valeur de la variable ou null si impossible à récupérer
   */
  private String getMySQLVariable(Statement statement, String variableName) {
    try {
      String sql = "SHOW VARIABLES LIKE '" + variableName + "'";
      try (ResultSet rs = statement.executeQuery(sql)) {
        if (rs.next()) {
          return rs.getString("Value");
        }
      }
      log.warn("Variable MySQL '{}' non trouvée", variableName);
      return null;
    } catch (Exception e) {
      log.error("Erreur lors de la récupération de {} depuis MySQL : {}", variableName,
          e.getMessage(), e);
      return null;
    }
  }
}
