package cd.shad.erp.cmk.cmkerp.platform.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;

/**
 * Configuration pour l'exécution asynchrone de tâches.
 *
 * <p>
 * Fournit un pool de threads dédié pour les opérations non critiques qui ne doivent pas bloquer
 * le thread HTTP (envoi d'emails, notifications, logs métiers lourds, etc.).
 *
 * <p>
 * Configuration du pool :
 * <ul>
 * <li>Core pool size : 8 threads (threads toujours actifs)</li>
 * <li>Max pool size : 32 threads (peut monter jusqu'à 32 en cas de charge)</li>
 * <li>Queue capacity : 500 tâches (tampon avant rejet)</li>
 * <li>Thread name prefix : "cmkerp-async-" (pour monitoring)</li>
 * </ul>
 *
 * <p>
 * Utilisation dans les services :
 *
 * <pre>{@code
 * @Async("cmkerpAsyncExecutor")
 * public void sendEmail(String to, String subject, String body) {
 *     // Traitement asynchrone (ne bloque pas le thread HTTP)
 * }
 * }</pre>
 *
 * <p>
 * Configuration via application.yml (optionnel) :
 *
 * <pre>{@code
 * cmkerp:
 *   async:
 *     core-pool-size: 8
 *     max-pool-size: 32
 *     queue-capacity: 500
 * }</pre>
 *

 */
@Configuration
@EnableAsync
public class AsyncConfig {

  private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

  @Value("${cmkerp.async.core-pool-size:8}")
  private int corePoolSize;

  @Value("${cmkerp.async.max-pool-size:32}")
  private int maxPoolSize;

  @Value("${cmkerp.async.queue-capacity:500}")
  private int queueCapacity;

  private final MeterRegistry meterRegistry;

  public AsyncConfig(@Autowired(required = false) MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * ThreadPoolTaskExecutor dédié pour les opérations asynchrones.
   *
   * <p>
   * Nom du bean : {@code cmkerpAsyncExecutor} (utilisé avec {@code @Async("cmkerpAsyncExecutor")}).
   * Les métriques Micrometer sont automatiquement enregistrées pour le monitoring.
   *
   * @return Executor configuré pour les tâches asynchrones avec métriques
   */
  @Bean(name = "cmkerpAsyncExecutor")
  public Executor cmkerpAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("cmkerp-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();

    // Enregistrer les métriques Micrometer pour le monitoring (si disponible)
    if (meterRegistry != null) {
      java.util.concurrent.ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
      Gauge.builder("cmkerp.async.executor.active", threadPoolExecutor, tp -> tp.getActiveCount())
          .description("Nombre de threads actifs dans le pool async")
          .register(meterRegistry);
      Gauge.builder("cmkerp.async.executor.queue.size", threadPoolExecutor, tp -> tp.getQueue().size())
          .description("Taille de la queue du pool async")
          .register(meterRegistry);
      Gauge.builder("cmkerp.async.executor.pool.size", threadPoolExecutor, tp -> tp.getPoolSize())
          .description("Taille actuelle du pool async")
          .register(meterRegistry);
      Gauge.builder("cmkerp.async.executor.completed", threadPoolExecutor, tp -> tp.getCompletedTaskCount())
          .description("Nombre de tâches complétées")
          .register(meterRegistry);
    }

    log.info("ThreadPoolTaskExecutor initialisé -> core={}, max={}, queue={}, prefix=cmkerp-async-",
        corePoolSize, maxPoolSize, queueCapacity);

    return executor;
  }
}

