package cd.shad.erp.cmk.cmkerp.platform.config.metrics;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@Configuration
@ConditionalOnBean(DataSource.class)
public class MetricsConfig implements MeterBinder {

  private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);

  private final DataSource primaryDataSource;
  private final RedisConnectionFactory redisConnectionFactory;

  public MetricsConfig(@Qualifier("primaryDataSource") DataSource primaryDataSource,
      @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
    this.primaryDataSource = primaryDataSource;
    this.redisConnectionFactory = redisConnectionFactory;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    if (registry == null) {
      return;
    }
    // Métriques HikariCP
    if (primaryDataSource instanceof HikariDataSource hikariDS) {
      var poolBean = hikariDS.getHikariPoolMXBean();
      if (poolBean != null) {
        // Métriques de base (connexions)
        Gauge.builder("cmkerp.db.pool.active", poolBean, bean -> bean.getActiveConnections())
            .description("Nombre de connexions DB actives").register(registry);
        Gauge.builder("cmkerp.db.pool.idle", poolBean, bean -> bean.getIdleConnections())
            .description("Nombre de connexions DB idle").register(registry);
        Gauge.builder("cmkerp.db.pool.total", poolBean, bean -> bean.getTotalConnections())
            .description("Total de connexions DB (actives + idle)").register(registry);
        Gauge
            .builder("cmkerp.db.pool.waiting", poolBean,
                bean -> bean.getThreadsAwaitingConnection())
            .description("Nombre de threads en attente d'une connexion DB").register(registry);
        Gauge.builder("cmkerp.db.pool.max", hikariDS, HikariDataSource::getMaximumPoolSize)
            .description("Taille maximale du pool DB").register(registry);

        // Métriques supplémentaires (temps et performance)
        Gauge.builder("cmkerp.db.pool.min", hikariDS, HikariDataSource::getMinimumIdle)
            .description("Nombre minimum de connexions idle maintenues").register(registry);

        // Utilisation du pool (pourcentage)
        Gauge.builder("cmkerp.db.pool.utilization", poolBean, bean -> {
          int total = bean.getTotalConnections();
          int max = hikariDS.getMaximumPoolSize();
          return max > 0 ? (double) total / max * 100.0 : 0.0;
        }).description("Taux d'utilisation du pool DB (%)").register(registry);

        log.info("Métriques HikariCP enregistrées pour le monitoring (base + utilisation)");
      }
    }

    // Métriques Redis pool (Lettuce)
    if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
      GenericObjectPool<?> pool = getLettucePool(lettuceFactory);
      if (pool != null) {
        Gauge.builder("cmkerp.redis.pool.active", pool, GenericObjectPool::getNumActive)
            .description("Nombre de connexions Redis actives").register(registry);
        Gauge.builder("cmkerp.redis.pool.idle", pool, GenericObjectPool::getNumIdle)
            .description("Nombre de connexions Redis idle").register(registry);
        Gauge.builder("cmkerp.redis.pool.total", pool, p -> p.getNumActive() + p.getNumIdle())
            .description("Total de connexions Redis (actives + idle)").register(registry);
        Gauge.builder("cmkerp.redis.pool.max", pool, GenericObjectPool::getMaxTotal)
            .description("Taille maximale du pool Redis").register(registry);

        log.info("Métriques Redis pool (Lettuce) enregistrées pour le monitoring");
      } else {
        log.warn("Impossible d'accéder au pool Lettuce pour les métriques Redis");
      }
    } else if (redisConnectionFactory != null) {
      log.debug("RedisConnectionFactory n'est pas une instance LettuceConnectionFactory, "
          + "métriques pool Redis non disponibles");
    }

    // Métriques JVM (mémoire, threads, etc.)
    registerJvmMetrics(registry);
  }

  /**
   * Enregistre les métriques JVM (mémoire, threads, GC, etc.).
   */
  private void registerJvmMetrics(MeterRegistry registry) {
    // Mémoire
    Gauge.builder("jvm.memory.used", Runtime.getRuntime(), r -> r.totalMemory() - r.freeMemory())
        .description("Mémoire JVM utilisée (bytes)").register(registry);
    Gauge.builder("jvm.memory.free", Runtime.getRuntime(), Runtime::freeMemory)
        .description("Mémoire JVM libre (bytes)").register(registry);
    Gauge.builder("jvm.memory.total", Runtime.getRuntime(), Runtime::totalMemory)
        .description("Mémoire JVM totale (bytes)").register(registry);
    Gauge.builder("jvm.memory.max", Runtime.getRuntime(), Runtime::maxMemory)
        .description("Mémoire JVM maximale (bytes)").register(registry);

    // Threads
    Gauge.builder("jvm.threads.live", Thread::activeCount)
        .description("Nombre de threads JVM actifs").register(registry);

    log.info("Métriques JVM enregistrées pour le monitoring");
  }

  /**
   * Accède au pool GenericObjectPool de LettuceConnectionFactory via réflexion.
   *
   * <p>
   * Le pool est stocké dans un champ privé de LettuceConnectionFactory. Cette méthode utilise la
   * réflexion pour y accéder de manière sécurisée. Elle recherche le pool en parcourant tous les
   * champs déclarés de la classe.
   *
   * @param factory la LettuceConnectionFactory
   * @return le GenericObjectPool ou null si l'accès échoue
   */
  private GenericObjectPool<?> getLettucePool(LettuceConnectionFactory factory) {
    // Essayer d'abord avec le nom de champ le plus courant
    String[] possibleFieldNames = {"pool", "connectionPool", "asyncPool"};
    for (String fieldName : possibleFieldNames) {
      try {
        Field poolField = LettuceConnectionFactory.class.getDeclaredField(fieldName);
        poolField.setAccessible(true);
        Object pool = poolField.get(factory);
        if (pool instanceof GenericObjectPool) {
          return (GenericObjectPool<?>) pool;
        }
      } catch (NoSuchFieldException e) {
        // Continuer avec le prochain nom de champ
        log.trace("Champ '{}' non trouvé dans LettuceConnectionFactory", fieldName);
      } catch (IllegalAccessException e) {
        log.debug("Accès refusé au champ '{}' de LettuceConnectionFactory", fieldName, e);
      } catch (Exception e) {
        log.debug("Erreur lors de l'accès au champ '{}'", fieldName, e);
      }
    }

    // Si aucun champ nommé n'est trouvé, parcourir tous les champs déclarés
    try {
      Field[] fields = LettuceConnectionFactory.class.getDeclaredFields();
      for (Field field : fields) {
        if (GenericObjectPool.class.isAssignableFrom(field.getType())) {
          field.setAccessible(true);
          Object pool = field.get(factory);
          if (pool instanceof GenericObjectPool) {
            log.debug("Pool Lettuce trouvé via le champ '{}'", field.getName());
            return (GenericObjectPool<?>) pool;
          }
        }
      }
    } catch (IllegalAccessException e) {
      log.debug("Erreur lors de la recherche du pool dans tous les champs", e);
    } catch (Exception e) {
      log.debug("Erreur inattendue lors de la recherche du pool", e);
    }

    log.warn("Impossible de trouver le pool GenericObjectPool dans LettuceConnectionFactory");
    return null;
  }
}
