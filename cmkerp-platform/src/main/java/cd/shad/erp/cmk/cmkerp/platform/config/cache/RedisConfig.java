package cd.shad.erp.cmk.cmkerp.platform.config.cache;

import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * Configuration Redis pour le cache distribué.
 *
 * <p>
 * Cette configuration permet de remplacer le cache en mémoire simple par un cache distribué basé
 * sur Redis, essentiel pour la scalabilité horizontale (multi-instances).
 *
 * <p>
 * Avantages :
 * <ul>
 * <li>Cache partagé entre toutes les instances de l'application</li>
 * <li>Évite la duplication de cache en mémoire sur chaque instance</li>
 * <li>Permet la scalabilité horizontale sans perte de cache</li>
 * <li>TTL (Time To Live) configurable par cache</li>
 * </ul>
 *
 * <p>
 * Configuration requise dans application-*.yml :
 *
 * <pre>{@code
 * spring:
 *   data:
 *     redis:
 *       host: ${CMK_REDIS_HOST:localhost}
 *       port: ${CMK_REDIS_PORT:6379}
 *       password: ${CMK_REDIS_PASSWORD:}
 *       timeout: 2000
 * }</pre>
 *
 * <p>
 * Caches configurés :
 * <ul>
 * <li>{@code users} : Profils utilisateurs (TTL: 1h)</li>
 * <li>{@code roles} : Rôles et permissions (TTL: 1h)</li>
 * <li>{@code sites} : Sites (TTL: 30min)</li>
 * <li>{@code permissions} : Permissions (TTL: 1h)</li>
 * </ul>
 *
 *
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisPoolProperties.class)
public class RedisConfig {

  private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Value("${spring.data.redis.timeout:2000}")
  private long redisTimeoutMs;

  /**
   * Configuration de la connexion Redis avec Lettuce (client non-bloquant) et pool optimisé.
   *
   * <p>
   * Le pool est configuré via {@link RedisPoolProperties} pour permettre un tuning fin selon
   * l'environnement (dev/prod) et le nombre d'instances.
   *
   * @param poolProperties les propriétés de configuration du pool Redis
   * @return RedisConnectionFactory configurée avec pool optimisé
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory(RedisPoolProperties poolProperties) {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(redisHost);
    config.setPort(redisPort);
    if (redisPassword != null && !redisPassword.isBlank()) {
      config.setPassword(redisPassword);
    }

    // Configuration du pool Lettuce
    GenericObjectPoolConfig<io.lettuce.core.api.StatefulConnection<?, ?>> poolConfig =
        new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(poolProperties.getMaxActive());
    poolConfig.setMaxIdle(poolProperties.getMaxIdle());
    poolConfig.setMinIdle(poolProperties.getMinIdle());
    poolConfig.setTimeBetweenEvictionRuns(
        Duration.ofMillis(poolProperties.getTimeBetweenEvictionRunsMs()));
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestWhileIdle(true);

    // ClientResources pour optimiser les ressources Lettuce
    ClientResources clientResources = DefaultClientResources.builder().build();

    // Configuration du pool Lettuce
    LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
        .poolConfig(poolConfig).commandTimeout(Duration.ofMillis(redisTimeoutMs))
        .clientResources(clientResources).build();

    LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
    factory.afterPropertiesSet();

    log.info(
        "RedisConnectionFactory initialisée -> Host: {}:{}, Pool: max-active={}, max-idle={}, min-idle={}, eviction-interval={}ms",
        redisHost, redisPort, poolProperties.getMaxActive(), poolProperties.getMaxIdle(),
        poolProperties.getMinIdle(), poolProperties.getTimeBetweenEvictionRunsMs());

    if (poolProperties.getNumberOfInstances() != null
        && poolProperties.getNumberOfInstances() > 0) {
      int totalConnections = poolProperties.getMaxActive() * poolProperties.getNumberOfInstances();
      int requiredMaxClients = (int) (totalConnections * 1.20); // +20% marge de sécurité
      log.info(
          "✅ Vérification maxclients activée : {} connexions totales attendues ({} max-active × {} instances). "
              + "maxclients Redis requis : ≥{}. Vérification automatique au démarrage par RedisMaxClientsChecker.",
          totalConnections, poolProperties.getMaxActive(), poolProperties.getNumberOfInstances(),
          requiredMaxClients);
    } else {
      log.info(
          "ℹ️ Vérification maxclients désactivée : configurez 'cmkerp.redis.pool.number-of-instances' pour l'activer. "
              + "Pour vérifier manuellement : CONFIG GET maxclients dans Redis CLI");
    }

    return factory;
  }

  /**
   * ObjectMapper configuré pour supporter les types Java 8 date/time (LocalDateTime, etc.).
   *
   * <p>
   * Ce ObjectMapper est utilisé par GenericJackson2JsonRedisSerializer pour sérialiser correctement
   * les objets contenant des LocalDateTime dans Redis.
   *
   * @return ObjectMapper configuré avec le module JavaTimeModule
   */
  @Bean
  public ObjectMapper redisObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /**
   * RedisTemplate pour opérations Redis personnalisées (hors cache Spring).
   *
   * <p>
   * Sériealisation :
   * <ul>
   * <li>Clés : String</li>
   * <li>Valeurs : JSON (GenericJackson2JsonRedisSerializer avec support Java 8 date/time)</li>
   * </ul>
   *
   * @param connectionFactory la factory Redis
   * @param redisObjectMapper l'ObjectMapper configuré pour Redis
   * @return RedisTemplate configuré
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
      ObjectMapper redisObjectMapper) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Sériealisation des clés en String
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    // Sériealisation des valeurs en JSON avec support Java 8 date/time
    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
  }

  /**
   * Configuration du CacheManager basé sur Redis.
   *
   * <p>
   * Caches configurés avec TTL personnalisés :
   * <ul>
   * <li>{@code users} : 1 heure</li>
   * <li>{@code roles} : 1 heure</li>
   * <li>{@code sites} : 30 minutes</li>
   * <li>{@code permissions} : 1 heure</li>
   * </ul>
   *
   * @param connectionFactory la factory Redis
   * @param redisObjectMapper l'ObjectMapper configuré pour Redis (avec support Java 8 date/time)
   * @return CacheManager configuré
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
      ObjectMapper redisObjectMapper) {
    // Serializer JSON avec support Java 8 date/time
    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(redisObjectMapper);

    // Configuration par défaut (sériealisation JSON avec support Java 8 date/time)
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1))
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
        .disableCachingNullValues(); // Ne pas cacher les valeurs null

    // Configurations spécifiques par cache (optimisées)
    return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig)
        .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("roles", defaultConfig.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("sites", defaultConfig.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("permissions", defaultConfig.entryTtl(Duration.ofHours(1)))
        .transactionAware() // Intégration avec les transactions Spring
        .enableStatistics() // Activer les statistiques pour monitoring
        .build();
  }
}

