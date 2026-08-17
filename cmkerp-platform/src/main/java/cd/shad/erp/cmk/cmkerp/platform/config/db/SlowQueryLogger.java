package cd.shad.erp.cmk.cmkerp.platform.config.db;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Component;

/**
 * Logger pour les requêtes SQL lentes avec corrélation des logs.
 *
 * <p>
 * Intercepte les requêtes SQL via JdbcTemplate et log les requêtes lentes avec :
 * <ul>
 * <li>correlationId : Pour corréler avec les logs HTTP</li>
 * <li>durée d'exécution</li>
 * <li>requête SQL (normalisée)</li>
 * <li>paramètres (si disponibles)</li>
 * <li>nombre de lignes affectées</li>
 * </ul>
 *
 * <p>
 * Configuration via application.yml :
 *
 * <pre>{@code
 * cmkerp:
 *   db:
 *     slow-query-logger:
 *       enabled: true
 *       threshold-ms: 1000  # Seuil en millisecondes (défaut: 1000ms)
 * }</pre>
 */
@Component
public class SlowQueryLogger {

  private static final Logger log = LoggerFactory.getLogger(SlowQueryLogger.class);
  private static final Logger slowQueryLog = LoggerFactory.getLogger("slow.query");

  private final JdbcTemplate jdbcTemplate;
  private final boolean enabled;
  private final long thresholdMs;

  public SlowQueryLogger(JdbcTemplate jdbcTemplate,
      @Value("${cmkerp.db.slow-query-logger.enabled:true}") boolean enabled,
      @Value("${cmkerp.db.slow-query-logger.threshold-ms:1000}") long thresholdMs) {
    this.jdbcTemplate = jdbcTemplate;
    this.enabled = enabled;
    this.thresholdMs = thresholdMs;
  }

  /**
   * Log une requête lente si elle dépasse le seuil.
   *
   * @param sql la requête SQL
   * @param durationMs la durée d'exécution en millisecondes
   * @param rowsAffected le nombre de lignes affectées (optionnel)
   */
  public void logSlowQuery(String sql, long durationMs, Long rowsAffected) {
    if (!enabled || durationMs < thresholdMs) {
      return;
    }

    String correlationId = MDC.get("correlationId");
    String normalizedSql = normalizeSql(sql);

    StringBuilder logMessage = new StringBuilder();
    logMessage.append("SLOW QUERY detected - ");
    logMessage.append("duration: ").append(durationMs).append("ms, ");
    logMessage.append("threshold: ").append(thresholdMs).append("ms, ");

    if (correlationId != null) {
      logMessage.append("correlationId: ").append(correlationId).append(", ");
    }

    if (rowsAffected != null) {
      logMessage.append("rowsAffected: ").append(rowsAffected).append(", ");
    }

    logMessage.append("sql: ").append(normalizedSql);

    slowQueryLog.warn(logMessage.toString());

    // Log structuré en JSON pour faciliter l'analyse
    log.warn(
        "Slow query detected - correlationId: {}, duration: {}ms, threshold: {}ms, rowsAffected: {}, sql: {}",
        correlationId != null ? correlationId : "unknown", durationMs, thresholdMs, rowsAffected,
        normalizedSql);
  }

  /**
   * Normalise une requête SQL pour le logging (masque les valeurs sensibles).
   *
   * <p>
   * Remplace les valeurs par des placeholders pour éviter de logger des données sensibles.
   */
  private String normalizeSql(String sql) {
    if (sql == null || sql.isEmpty()) {
      return sql;
    }

    // Remplacer les valeurs entre quotes par ?
    String normalized = sql.replaceAll("'[^']*'", "'?'");
    normalized = normalized.replaceAll("\"[^\"]*\"", "\"?\"");

    // Remplacer les valeurs numériques par ? (pour les IDs, etc.)
    normalized = normalized.replaceAll("\\b\\d+\\b", "?");

    // Standardiser les espaces
    normalized = normalized.replaceAll("\\s+", " ").trim();

    // Limiter la longueur pour éviter les logs trop longs
    if (normalized.length() > 500) {
      normalized = normalized.substring(0, 500) + "...";
    }

    return normalized;
  }

  /**
   * Wrapper pour StatementCallback qui log les requêtes lentes.
   *
   * <p>
   * Note : Cette méthode est fournie pour usage manuel. Pour une intégration automatique,
   * considérer l'utilisation d'un DataSource proxy ou d'un aspect AOP.
   */
  public <T> T executeWithLogging(StatementCallback<T> action) {
    long startTime = System.nanoTime();
    try {
      T result = jdbcTemplate.execute(action);
      long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      // Extraire la requête SQL si possible
      String sql = extractSqlFromAction(action);
      Long rowsAffected = extractRowsAffected(result);

      if (enabled && durationMs >= thresholdMs) {
        logSlowQuery(sql, durationMs, rowsAffected);
      }

      return result;
    } catch (Exception e) {
      long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      String sql = extractSqlFromAction(action);
      if (enabled && durationMs >= thresholdMs) {
        logSlowQuery(sql, durationMs, null);
      }
      throw e;
    }
  }

  /**
   * Wrapper pour PreparedStatementCallback qui log les requêtes lentes.
   *
   * <p>
   * Note : Cette méthode est fournie pour usage manuel. Pour une intégration automatique,
   * considérer l'utilisation d'un DataSource proxy ou d'un aspect AOP.
   */
  public <T> T executeWithLogging(String sql, PreparedStatementCallback<T> action) {
    long startTime = System.nanoTime();
    try {
      T result = jdbcTemplate.execute(sql, action);
      long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      Long rowsAffected = extractRowsAffected(result);

      if (enabled && durationMs >= thresholdMs) {
        logSlowQuery(sql, durationMs, rowsAffected);
      }

      return result;
    } catch (Exception e) {
      long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      if (enabled && durationMs >= thresholdMs) {
        logSlowQuery(sql, durationMs, null);
      }
      throw e;
    }
  }

  /**
   * Extrait la requête SQL depuis un StatementCallback (si possible).
   */
  private String extractSqlFromAction(StatementCallback<?> action) {
    try {
      // Essayer d'extraire la requête via réflexion si c'est une classe anonyme
      // Sinon, retourner une valeur par défaut
      return action.toString();
    } catch (Exception e) {
      return "unknown";
    }
  }

  /**
   * Extrait le nombre de lignes affectées depuis le résultat (si c'est un Integer ou Long).
   */
  private Long extractRowsAffected(Object result) {
    if (result instanceof Integer integer) {
      return integer.longValue();
    } else if (result instanceof Long longValue) {
      return longValue;
    }
    return null;
  }
}
