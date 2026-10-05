package cd.shad.erp.cmk.cmkerp.platform.config.metrics;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

@Configuration
@ConditionalOnBean(name = "primaryDataSource")
public class MetricsConfig implements MeterBinder {

  private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);

  private final DataSource primaryDataSource;
  private final ObjectProvider<DataSource> medilineDataSource;
  private final ObjectProvider<DataSource> cliniqueDataSource;
  private final RedisConnectionFactory redisConnectionFactory;

  public MetricsConfig(
      @Qualifier("primaryDataSource") DataSource primaryDataSource,
      @Qualifier("medilineDataSource") ObjectProvider<DataSource> medilineDataSource,
      @Qualifier("cliniqueDataSource") ObjectProvider<DataSource> cliniqueDataSource,
      @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
    this.primaryDataSource = primaryDataSource;
    this.medilineDataSource = medilineDataSource;
    this.cliniqueDataSource = cliniqueDataSource;
    this.redisConnectionFactory = redisConnectionFactory;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    if (registry == null) {
      return;
    }
    registerHikari(registry, "primary", primaryDataSource);
    registerHikari(registry, "mediline", medilineDataSource.getIfAvailable());
    registerHikari(registry, "clinique", cliniqueDataSource.getIfAvailable());

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
        log.info("Métriques Redis pool (Lettuce) enregistrées");
      } else {
        log.warn("Impossible d'accéder au pool Lettuce pour les métriques Redis");
      }
    } else if (redisConnectionFactory != null) {
      log.debug("RedisConnectionFactory non Lettuce — métriques pool Redis indisponibles");
    }

    registerJvmMetrics(registry);
  }

  private void registerHikari(MeterRegistry registry, String poolId, DataSource ds) {
    if (!(ds instanceof HikariDataSource hikariDS)) {
      return;
    }
    HikariPoolMXBean poolBean = hikariDS.getHikariPoolMXBean();
    if (poolBean == null) {
      return;
    }
    Tags tags = Tags.of("pool", poolId);
    Gauge.builder("cmkerp.db.pool.active", poolBean, HikariPoolMXBean::getActiveConnections)
        .tags(tags).description("Connexions DB actives").register(registry);
    Gauge.builder("cmkerp.db.pool.idle", poolBean, HikariPoolMXBean::getIdleConnections)
        .tags(tags).description("Connexions DB idle").register(registry);
    Gauge.builder("cmkerp.db.pool.total", poolBean, HikariPoolMXBean::getTotalConnections)
        .tags(tags).description("Total connexions DB").register(registry);
    Gauge.builder("cmkerp.db.pool.waiting", poolBean, HikariPoolMXBean::getThreadsAwaitingConnection)
        .tags(tags).description("Threads en attente d'une connexion").register(registry);
    Gauge.builder("cmkerp.db.pool.max", hikariDS, HikariDataSource::getMaximumPoolSize)
        .tags(tags).description("Taille max du pool").register(registry);
    Gauge.builder("cmkerp.db.pool.min", hikariDS, HikariDataSource::getMinimumIdle)
        .tags(tags).description("Min idle du pool").register(registry);
    Gauge.builder("cmkerp.db.pool.utilization", poolBean, bean -> {
      int max = hikariDS.getMaximumPoolSize();
      return max > 0 ? (double) bean.getTotalConnections() / max * 100.0 : 0.0;
    }).tags(tags).description("Utilisation du pool (%)").register(registry);
    log.info("Métriques HikariCP enregistrées pour pool={}", poolId);
  }

  private void registerJvmMetrics(MeterRegistry registry) {
    Gauge.builder("jvm.memory.used", Runtime.getRuntime(), r -> r.totalMemory() - r.freeMemory())
        .description("Mémoire JVM utilisée (bytes)").register(registry);
    Gauge.builder("jvm.memory.free", Runtime.getRuntime(), Runtime::freeMemory)
        .description("Mémoire JVM libre (bytes)").register(registry);
    Gauge.builder("jvm.memory.total", Runtime.getRuntime(), Runtime::totalMemory)
        .description("Mémoire JVM totale (bytes)").register(registry);
    Gauge.builder("jvm.memory.max", Runtime.getRuntime(), Runtime::maxMemory)
        .description("Mémoire JVM maximale (bytes)").register(registry);
    Gauge.builder("jvm.threads.live", Thread::activeCount)
        .description("Nombre de threads JVM actifs").register(registry);
    log.info("Métriques JVM enregistrées");
  }

  private GenericObjectPool<?> getLettucePool(LettuceConnectionFactory factory) {
    String[] possibleFieldNames = {"pool", "connectionPool", "asyncPool"};
    for (String fieldName : possibleFieldNames) {
      try {
        Field poolField = LettuceConnectionFactory.class.getDeclaredField(fieldName);
        poolField.setAccessible(true);
        Object pool = poolField.get(factory);
        if (pool instanceof GenericObjectPool) {
          return (GenericObjectPool<?>) pool;
        }
      } catch (Exception e) {
        log.trace("Champ '{}' Lettuce inaccessible: {}", fieldName, e.toString());
      }
    }
    try {
      for (Field field : LettuceConnectionFactory.class.getDeclaredFields()) {
        if (GenericObjectPool.class.isAssignableFrom(field.getType())) {
          field.setAccessible(true);
          Object pool = field.get(factory);
          if (pool instanceof GenericObjectPool) {
            return (GenericObjectPool<?>) pool;
          }
        }
      }
    } catch (Exception e) {
      log.debug("Recherche pool Lettuce échouée: {}", e.toString());
    }
    return null;
  }
}
