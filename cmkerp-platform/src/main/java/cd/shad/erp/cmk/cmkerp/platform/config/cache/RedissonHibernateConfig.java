package cd.shad.erp.cmk.cmkerp.platform.config.cache;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration Redisson pour le cache Hibernate second-level avec Redis.
 *
 * <p>
 * Cette configuration permet à Hibernate d'utiliser Redis comme provider de cache
 * au lieu de JCache générique. Redisson utilise automatiquement la même connexion
 * Redis que Spring Boot (configuration dans application.yml).
 *
 * <p>
 * Avantages :
 * <ul>
 * <li>Cache Hibernate distribué dans Redis (partagé entre instances)</li>
 * <li>Scalabilité horizontale : toutes les instances partagent le même cache</li>
 * <li>Cohérence du cache entre les instances de l'application</li>
 * <li>Performance améliorée pour les entités fréquemment accédées</li>
 * </ul>
 *
 * <p>
 * Configuration Redisson utilisant les mêmes paramètres Redis que Spring Boot :
 * <ul>
 * <li>Host : {@code spring.data.redis.host}</li>
 * <li>Port : {@code spring.data.redis.port}</li>
 * <li>Password : {@code spring.data.redis.password}</li>
 * </ul>
 *

 */
/**
 * Configuration Redisson pour le cache Hibernate second-level avec Redis.
 *
 * <p>
 * Cette configuration est conditionnelle : elle ne s'active que si le cache Hibernate
 * second-level est activé dans la configuration (spring.jpa.properties.hibernate.cache.use_second_level_cache=true).
 *
 * <p>
 * Si le cache est désactivé (par exemple en DEV), ce bean ne sera pas créé et Hibernate
 * utilisera le cache en mémoire par défaut.
 */
@Configuration
@ConditionalOnProperty(
    name = "spring.jpa.properties.hibernate.cache.use_second_level_cache",
    havingValue = "true",
    matchIfMissing = false
)
public class RedissonHibernateConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Configuration du client Redisson pour Hibernate.
     *
     * <p>
     * Redisson utilise automatiquement cette configuration pour le cache Hibernate
     * via la propriété {@code hibernate.cache.region.factory_class=org.redisson.hibernate.RedissonRegionFactory}
     * configurée dans application.yml.
     *
     * @return RedissonClient configuré
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Configuration de la connexion Redis (mode standalone)
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(20)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // Ajouter le mot de passe si configuré
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}





