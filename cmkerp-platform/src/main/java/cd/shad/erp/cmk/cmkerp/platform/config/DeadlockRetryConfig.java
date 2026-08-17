package cd.shad.erp.cmk.cmkerp.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuration pour la gestion des deadlocks MySQL avec retry automatique.
 *
 * <p>
 * Cette configuration permet de gérer automatiquement les deadlocks MySQL en réessayant l'opération
 * avec un backoff exponentiel.
 *
 * <p>
 * Utilisation typique :
 *
 * <pre>{@code
 * &#64;Autowired
 * private RetryTemplate deadlockRetryTemplate;
 *
 * public void performOperation() {
 *   deadlockRetryTemplate.execute(context -> {
 *     // Opération susceptible de causer un deadlock
 *     jdbcTemplate.update("UPDATE table SET ... WHERE id = ?", ...);
 *     return null;
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Cette configuration est conditionnelle : elle ne sera chargée que si la dépendance
 * {@code spring-retry} est présente dans le classpath. Cela permet d'éviter les erreurs
 * lors des tests ou dans des environnements où cette dépendance n'est pas disponible.
 */
@Configuration
@EnableRetry
@ConditionalOnClass(name = "org.springframework.retry.RetryPolicy")
public class DeadlockRetryConfig {

  /**
   * Crée un RetryTemplate configuré spécifiquement pour les deadlocks MySQL.
   *
   * <p>
   * Configuration :
   * <ul>
   * <li>Nombre de tentatives : 3 (maxAttempts = 3)</li>
   * <li>Backoff exponentiel : initial 100ms, multiplier 2.0, max 1000ms</li>
   * <li>Exceptions retryables : PessimisticLockingFailureException (inclut les deadlocks)</li>
   * </ul>
   *
   * @return RetryTemplate configuré pour les deadlocks
   */
  @Bean(name = "deadlockRetryTemplate")
  public RetryTemplate deadlockRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    // Politique de retry : 3 tentatives maximum
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(3);
    retryTemplate.setRetryPolicy(retryPolicy);

    // Backoff exponentiel : 100ms, 200ms, 400ms (max 1000ms)
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(100); // 100ms
    backOffPolicy.setMultiplier(2.0); // Double à chaque tentative
    backOffPolicy.setMaxInterval(1000); // Max 1000ms
    retryTemplate.setBackOffPolicy(backOffPolicy);

    return retryTemplate;
  }

  /**
   * Détecte si une exception est un deadlock MySQL.
   *
   * <p>
   * MySQL retourne généralement :
   * <ul>
   * <li>SQLException avec SQLState = "40001" (Deadlock found)</li>
   * <li>PessimisticLockingFailureException (Spring wrapper, inclut les deadlocks)</li>
   * </ul>
   *
   * @param ex L'exception à vérifier
   * @return true si c'est un deadlock
   */
  public static boolean isDeadlockException(Throwable ex) {
    if (ex instanceof PessimisticLockingFailureException) {
      return true;
    }

    // Vérifier SQLState pour les deadlocks MySQL
    if (ex.getMessage() != null) {
      String message = ex.getMessage().toLowerCase();
      return message.contains("deadlock") || message.contains("try restarting transaction")
          || message.contains("40001");
    }

    return false;
  }
}
