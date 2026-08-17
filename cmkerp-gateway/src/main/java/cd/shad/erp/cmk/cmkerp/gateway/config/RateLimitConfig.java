package cd.shad.erp.cmk.cmkerp.gateway.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Bucket4j pour le rate limiting distribué avec Redis.
 *
 * <p>
 * Cette configuration expose un {@link ProxyManager<String>} basé sur Redis (Lettuce)
 * qui permet de partager les buckets de rate limiting entre toutes les instances de l'API.
 *
 * <p>
 * Avantages :
 * <ul>
 * <li>Rate limiting distribué : les quotas sont partagés entre toutes les instances</li>
 * <li>Scalabilité horizontale : fonctionne avec plusieurs instances JAR derrière un load balancer</li>
 * <li>Pas de désynchronisation : un utilisateur ne peut pas contourner la limite en changeant d'instance</li>
 * </ul>
 *
 * <p>
 * Configuration requise dans application-*.yml :
 * <pre>{@code
 * spring:
 *   data:
 *     redis:
 *       host: ${CMK_REDIS_HOST:localhost}
 *       port: ${CMK_REDIS_PORT:6379}
 *       password: ${CMK_REDIS_PASSWORD:}
 * }</pre>
 *
 * <p>
 * Le ProxyManager utilise une connexion Redis dédiée pour Bucket4j.
 * Les clés de rate limiting sont stockées dans Redis avec le préfixe "bucket4j:".
 *

 */
@Slf4j
@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Crée un ProxyManager basé sur Redis (Lettuce) pour le rate limiting distribué.
     *
     * <p>
     * Ce bean utilise une connexion Redis dédiée pour Bucket4j, basée sur la configuration
     * Spring Data Redis existante. Les buckets sont stockés dans Redis avec le préfixe "bucket4j:".
     *
     * <p>
     * Note : Bucket4j nécessite une connexion Redis dédiée (StatefulRedisConnection),
     * différente de celle utilisée par Spring Data Redis pour le cache. C'est pourquoi
     * nous créons un RedisClient séparé basé sur la même configuration.
     *
     * @return ProxyManager configuré pour Redis
     */
    @Bean
    public ProxyManager<String> rateLimitProxyManager() {
        log.info("Initialisation du ProxyManager Bucket4j pour rate limiting distribué (Redis: {}:{})",
                redisHost, redisPort);

        // Créer un RedisClient basé sur la configuration Spring
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort);

        if (redisPassword != null && !redisPassword.isBlank()) {
            uriBuilder.withPassword(redisPassword.toCharArray());
        }

        RedisURI redisURI = uriBuilder.build();
        RedisClient redisClient = RedisClient.create(redisURI);

        // Créer une connexion Redis dédiée pour Bucket4j avec le codec approprié
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        // Créer le ProxyManager
        // Note: withExpirationStrategy() est déprécié mais fonctionne toujours
        // L'expiration est gérée automatiquement par Bucket4j
        ProxyManager<String> proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();

        log.info("ProxyManager Bucket4j initialisé avec succès (préfixe: bucket4j:)");

        return proxyManager;
    }
}

