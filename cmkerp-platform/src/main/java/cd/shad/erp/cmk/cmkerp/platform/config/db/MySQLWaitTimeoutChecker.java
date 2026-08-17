package cd.shad.erp.cmk.cmkerp.platform.config.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Vérificateur d'alignement wait_timeout MySQL avec les paramètres HikariCP au démarrage.
 *
 * <p>
 * Cette classe vérifie automatiquement que les timeouts HikariCP sont correctement alignés avec les
 * paramètres MySQL wait_timeout et interactive_timeout.
 *
 * <p>
 * Règle de vérification (ordre strict requis) :
 *
 * <pre>{@code
 * connectionTimeout < idleTimeout < maxLifetime < wait_timeout MySQL
 * }</pre>
 *
 * <p>
 * Exemple de configuration valide :
 * <ul>
 * <li>connectionTimeout = 30s (30000ms)</li>
 * <li>idleTimeout = 10min (600000ms)</li>
 * <li>maxLifetime = 7h30 (27000000ms)</li>
 * <li>wait_timeout MySQL = 8h (28800s = 28800000ms)</li>
 * <li>Vérification : 30000 < 600000 < 27000000 < 28800000 ✓</li>
 * </ul>
 *
 * <p>
 * Si maxLifetime ≥ wait_timeout MySQL, MySQL fermera les connexions avant qu'HikariCP ne les
 * recycle, causant des erreurs "Connection is closed" ou "Communications link failure".
 */
@Component
public class MySQLWaitTimeoutChecker implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(MySQLWaitTimeoutChecker.class);
  private static final long SAFETY_MARGIN_MS = 30L * 60L * 1000L; // 30 minutes de marge

  private final DataSource primaryDataSource;
  private final DatabasePoolProperties poolProperties;

  public MySQLWaitTimeoutChecker(@Qualifier("primaryDataSource") DataSource primaryDataSource,
      DatabasePoolProperties poolProperties) {
    this.primaryDataSource = primaryDataSource;
    this.poolProperties = poolProperties;
  }

  @Override
  public void run(String... args) {
    try {
      checkWaitTimeoutAlignment();
    } catch (Exception e) {
      log.error("Erreur lors de la vérification wait_timeout MySQL : {}", e.getMessage(), e);
      // Ne pas bloquer le démarrage, juste logger l'erreur
    }
  }

  /**
   * Vérifie l'alignement entre les timeouts HikariCP et wait_timeout MySQL.
   */
  private void checkWaitTimeoutAlignment() {
    try (Connection connection = primaryDataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // Récupérer wait_timeout depuis MySQL (en secondes)
      Long waitTimeoutSeconds = getMySQLVariable(statement, "wait_timeout");
      Long interactiveTimeoutSeconds = getMySQLVariable(statement, "interactive_timeout");

      if (waitTimeoutSeconds == null) {
        log.warn("Impossible de récupérer wait_timeout depuis MySQL. Vérification ignorée.");
        return;
      }

      // Convertir en millisecondes
      long waitTimeoutMs = waitTimeoutSeconds * 1000L;
      Long interactiveTimeoutMs =
          interactiveTimeoutSeconds != null ? (interactiveTimeoutSeconds.longValue() * 1000L)
              : null;

      // Récupérer les valeurs HikariCP
      long connectionTimeoutMs = poolProperties.getConnectionTimeoutMs();
      long idleTimeoutMs = poolProperties.getIdleTimeoutMs();
      long maxLifetimeMs = poolProperties.getMaxLifetimeMs();

      // Afficher les informations de configuration
      logConfigurationInfo(connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs, waitTimeoutSeconds,
          waitTimeoutMs, interactiveTimeoutSeconds, interactiveTimeoutMs);

      // Calculer la valeur recommandée pour maxLifetime
      long recommendedMaxLifetime = waitTimeoutMs - SAFETY_MARGIN_MS;

      // Vérifier et afficher les résultats
      validateAndLogResults(connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs, waitTimeoutMs,
          recommendedMaxLifetime, interactiveTimeoutMs);

      log.info("==================================================");

    } catch (Exception e) {
      log.error("Erreur lors de la vérification wait_timeout : {}", e.getMessage(), e);
      // Ne pas bloquer le démarrage
    }
  }

  /**
   * Affiche les informations de configuration HikariCP et MySQL.
   */
  private void logConfigurationInfo(long connectionTimeoutMs, long idleTimeoutMs,
      long maxLifetimeMs, Long waitTimeoutSeconds, long waitTimeoutMs,
      Long interactiveTimeoutSeconds, Long interactiveTimeoutMs) {
    log.info("=== Vérification MySQL wait_timeout alignment ===");
    log.info("Configuration HikariCP :");
    log.info("  - connection-timeout: {}ms ({} min)", connectionTimeoutMs,
        connectionTimeoutMs / 60000.0);
    log.info("  - idle-timeout: {}ms ({} min)", idleTimeoutMs, idleTimeoutMs / 60000.0);
    log.info("  - max-lifetime: {}ms ({} min)", maxLifetimeMs, maxLifetimeMs / 60000.0);
    log.info("Configuration MySQL :");
    log.info("  - wait_timeout: {}s ({} ms, {} min)", waitTimeoutSeconds, waitTimeoutMs,
        waitTimeoutMs / 60000.0);
    if (interactiveTimeoutMs != null) {
      log.info("  - interactive_timeout: {}s ({} ms, {} min)", interactiveTimeoutSeconds,
          interactiveTimeoutMs, interactiveTimeoutMs / 60000.0);
    }
  }

  /**
   * Valide l'alignement et affiche les résultats de vérification.
   */
  private void validateAndLogResults(long connectionTimeoutMs, long idleTimeoutMs,
      long maxLifetimeMs, long waitTimeoutMs, long recommendedMaxLifetime,
      Long interactiveTimeoutMs) {
    // Vérifier l'ordre : connectionTimeout < idleTimeout < maxLifetime
    boolean orderValid = connectionTimeoutMs < idleTimeoutMs && idleTimeoutMs < maxLifetimeMs;

    // Vérifier que maxLifetime < wait_timeout (avec marge)
    boolean maxLifetimeValid = maxLifetimeMs < waitTimeoutMs;
    boolean maxLifetimeWithMargin = maxLifetimeMs <= recommendedMaxLifetime;

    // Afficher les résultats
    if (orderValid && maxLifetimeValid && maxLifetimeWithMargin) {
      logValidConfiguration(connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs, waitTimeoutMs);
    } else if (orderValid && maxLifetimeValid) {
      logWarningWithoutMargin(connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs, waitTimeoutMs,
          recommendedMaxLifetime);
    } else {
      logInvalidConfiguration(connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs, waitTimeoutMs,
          recommendedMaxLifetime, orderValid, maxLifetimeValid);
    }

    // Vérifier aussi interactive_timeout si différent
    if (interactiveTimeoutMs != null && !interactiveTimeoutMs.equals(waitTimeoutMs)
        && maxLifetimeMs >= interactiveTimeoutMs) {
      log.warn("  ⚠ maxLifetime ({}) ≥ interactive_timeout MySQL ({})", maxLifetimeMs,
          interactiveTimeoutMs);
      log.warn(
          "    Recommandation : Aligner interactive_timeout avec wait_timeout ou ajuster maxLifetime");
    }
  }

  /**
   * Affiche un message de succès pour une configuration valide.
   */
  private void logValidConfiguration(long connectionTimeoutMs, long idleTimeoutMs,
      long maxLifetimeMs, long waitTimeoutMs) {
    log.info("✓ Configuration ALIGNÉE correctement :");
    log.info("  ✓ Ordre respecté : connectionTimeout ({}) < idleTimeout ({}) < maxLifetime ({})",
        connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs);
    log.info("  ✓ maxLifetime ({}) < wait_timeout ({}) avec marge de sécurité", maxLifetimeMs,
        waitTimeoutMs);
    long availableMargin = waitTimeoutMs - maxLifetimeMs;
    log.info("  ✓ Marge disponible : {}ms ({} min)", availableMargin, availableMargin / 60000.0);
  }

  /**
   * Affiche un avertissement pour une configuration valide mais sans marge.
   */
  private void logWarningWithoutMargin(long connectionTimeoutMs, long idleTimeoutMs,
      long maxLifetimeMs, long waitTimeoutMs, long recommendedMaxLifetime) {
    log.warn("⚠ Configuration VALIDE mais SANS MARGE de sécurité :");
    log.warn("  ✓ Ordre respecté : connectionTimeout ({}) < idleTimeout ({}) < maxLifetime ({})",
        connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs);
    log.warn("  ✓ maxLifetime ({}) < wait_timeout ({})", maxLifetimeMs, waitTimeoutMs);
    log.warn("  ⚠ Marge insuffisante : maxLifetime ({}) > recommandé ({})", maxLifetimeMs,
        recommendedMaxLifetime);
    log.warn("  Recommandation : Réduire max-lifetime-ms à {} ({} min) pour marge de 30min",
        recommendedMaxLifetime, recommendedMaxLifetime / 60000.0);
  }

  /**
   * Affiche une erreur pour une configuration invalide.
   */
  private void logInvalidConfiguration(long connectionTimeoutMs, long idleTimeoutMs,
      long maxLifetimeMs, long waitTimeoutMs, long recommendedMaxLifetime, boolean orderValid,
      boolean maxLifetimeValid) {
    log.error("✗ Configuration INVALIDE :");
    if (!orderValid) {
      logOrderError(connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs);
    }
    if (!maxLifetimeValid) {
      logMaxLifetimeError(maxLifetimeMs, waitTimeoutMs, recommendedMaxLifetime);
    }
  }

  /**
   * Affiche les erreurs d'ordre des timeouts.
   */
  private void logOrderError(long connectionTimeoutMs, long idleTimeoutMs, long maxLifetimeMs) {
    log.error(
        "  ✗ Ordre incorrect : connectionTimeout ({}) < idleTimeout ({}) < maxLifetime ({}) requis",
        connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs);
    if (connectionTimeoutMs >= idleTimeoutMs) {
      log.error("    → connectionTimeout ({}) doit être < idleTimeout ({})", connectionTimeoutMs,
          idleTimeoutMs);
    }
    if (idleTimeoutMs >= maxLifetimeMs) {
      log.error("    → idleTimeout ({}) doit être < maxLifetime ({})", idleTimeoutMs,
          maxLifetimeMs);
    }
  }

  /**
   * Affiche les erreurs de maxLifetime vs wait_timeout.
   */
  private void logMaxLifetimeError(long maxLifetimeMs, long waitTimeoutMs,
      long recommendedMaxLifetime) {
    log.error("  ✗ maxLifetime ({}) doit être < wait_timeout MySQL ({})", maxLifetimeMs,
        waitTimeoutMs);
    log.error("    → MySQL fermera les connexions avant qu'HikariCP ne les recycle !");
    log.error(
        "    → Cela causera des erreurs 'Connection is closed' ou 'Communications link failure'");
    log.error("  Action requise : Réduire max-lifetime-ms à au plus {} ({} min)",
        recommendedMaxLifetime, recommendedMaxLifetime / 60000.0);
  }

  /**
   * Récupère une variable MySQL via SHOW VARIABLES.
   *
   * @param statement le Statement SQL
   * @param variableName le nom de la variable MySQL
   * @return la valeur en secondes ou null si impossible à récupérer
   */
  private Long getMySQLVariable(Statement statement, String variableName) {
    try {
      String sql = "SHOW VARIABLES LIKE '" + variableName + "'";
      try (ResultSet rs = statement.executeQuery(sql)) {
        if (rs.next()) {
          String valueStr = rs.getString("Value");
          if (valueStr != null && !valueStr.isBlank()) {
            return Long.parseLong(valueStr.trim());
          }
        }
      }
      log.warn("Variable MySQL '{}' non trouvée ou valeur vide", variableName);
      return null;
    } catch (NumberFormatException e) {
      log.error("Erreur de format lors de la conversion de {} : {}", variableName, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("Erreur lors de la récupération de {} depuis MySQL : {}", variableName,
          e.getMessage(), e);
      return null;
    }
  }
}
