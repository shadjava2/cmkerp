package cd.shad.erp.cmk.cmkerp.platform.config.db;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import cd.shad.erp.cmk.cmkerp.platform.config.db.routing.ReadWriteRoutingContext;

/**
 * Configuration pour les read replicas MySQL (IMPLÉMENTATION COMPLÈTE).
 *
 * <p>
 * Cette configuration permet de router automatiquement les lectures vers des read replicas MySQL,
 * permettant de distribuer la charge de lecture et d'améliorer les performances.
 *
 * <p>
 * <strong>IMPORTANT : Cette configuration est désactivée par défaut.</strong>
 * Pour l'activer, configurer dans application-*.yml :
 * <pre>{@code
 * cmkerp:
 *   db:
 *     read-replica:
 *       enabled: true
 *       url: jdbc:mysql://read-replica-host:3306/cmkerp
 *       username: read_user
 *       password: read_pass
 * }</pre>
 *
 * <p>
 * Architecture prévue :
 * <ul>
 *   <li>Primary DataSource : écritures et lectures critiques (transactions)</li>
 *   <li>Read Replica DataSource : lectures uniquement (SELECT)</li>
 *   <li>RoutingDataSource : route automatiquement les lectures vers le replica</li>
 * </ul>
 *
 * <p>
 * Implémentation complète :
 * <ol>
 *   <li>Activer cette configuration avec {@code cmkerp.db.read-replica.enabled=true}</li>
 *   <li>Configurer l'URL du read replica dans application-prod.yml</li>
 *   <li>Le {@link RoutingDataSource} route automatiquement selon {@link ReadWriteRoutingContext}</li>
 *   <li>Annoter les méthodes de lecture avec {@code @ReadOnly} pour utiliser automatiquement le replica</li>
 *   <li>Les écritures (INSERT/UPDATE/DELETE) utilisent automatiquement le primary</li>
 * </ol>
 *
 * <p>
 * Avantages :
 * <ul>
 *   <li>Distribution de la charge de lecture sur plusieurs serveurs MySQL</li>
 *   <li>Amélioration des performances pour les requêtes de lecture intensive</li>
 *   <li>Scalabilité horizontale pour les lectures</li>
 * </ul>
 *
 * <p>
 * Précautions :
 * <ul>
 *   <li>Le replica peut avoir un léger délai de réplication (lag)</li>
 *   <li>Les lectures critiques (après écriture) doivent utiliser le primary</li>
 *   <li>Surveiller le lag de réplication avec {@code SHOW SLAVE STATUS}</li>
 * </ul>
 *

 */
@Configuration
@ConditionalOnProperty(name = "cmkerp.db.read-replica.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties({ReadReplicaDataSourceProperties.class, DatabasePoolProperties.class})
public class ReadReplicaDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ReadReplicaDataSourceConfig.class);

    @Value("${platform.jdbc.read-replica.fetch-size:250}")
    private int fetchSize;

    @Value("${platform.jdbc.read-replica.query-timeout:30}")
    private int queryTimeout;

    /**
     * Crée le bean DataSource pour le read replica MySQL.
     *
     * <p>
     * Cette datasource est utilisée uniquement pour les lectures (SELECT).
     * Les écritures (INSERT, UPDATE, DELETE) doivent toujours utiliser le primary.
     *
     * <p>
     * Le pool est configuré via {@link DatabasePoolProperties} pour permettre
     * un tuning fin selon l'environnement.
     *
     * @param properties les propriétés de configuration du read replica
     * @param poolProperties les propriétés de configuration du pool HikariCP
     * @return le DataSource du read replica
     */
    @Bean(name = "readReplicaDataSource")
    public DataSource readReplicaDataSource(ReadReplicaDataSourceProperties properties,
                                             DatabasePoolProperties poolProperties) {
        // Validation : vérifier que l'URL JDBC est configurée
        if (properties.getJdbcUrl() == null || properties.getJdbcUrl().isBlank()) {
            String errorMsg = String.format(
                    "Configuration read replica manquante. " +
                    "Vérifier que cmkerp.db.read-replica.url est définie dans application-{profile}.yml. " +
                    "Profil actuel: %s",
                    System.getProperty("spring.profiles.active", "non défini"));
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Créer un HikariConfig à partir des propriétés de datasource
        HikariConfig hikariConfig = new HikariConfig();

        // Copier les propriétés de base
        hikariConfig.setJdbcUrl(properties.getJdbcUrl());
        hikariConfig.setUsername(properties.getUsername());
        hikariConfig.setPassword(properties.getPassword());
        hikariConfig.setDriverClassName(properties.getDriverClassName());
        hikariConfig.setPoolName(properties.getPoolName());
        hikariConfig.setAutoCommit(true); // Read-only : auto-commit activé
        hikariConfig.setReadOnly(true); // Marquer comme read-only
        hikariConfig.setRegisterMbeans(properties.isRegisterMbeans());

        // Appliquer les propriétés du pool depuis cmkerp.db.pool.*
        // Note : Le pool du replica peut être plus petit que le primary (lectures uniquement)
        hikariConfig.setMaximumPoolSize(poolProperties.getMaxSize());
        hikariConfig.setMinimumIdle(poolProperties.getMinIdle());
        hikariConfig.setConnectionTimeout(poolProperties.getConnectionTimeoutMs());
        hikariConfig.setIdleTimeout(poolProperties.getIdleTimeoutMs());
        hikariConfig.setMaxLifetime(poolProperties.getMaxLifetimeMs());
        hikariConfig.setKeepaliveTime(poolProperties.getKeepaliveTimeMs());

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        log.info("Read Replica DataSource initialisé -> URL: {}, Pool: {} (max: {}, min: {}), Read-only: true",
                dataSource.getJdbcUrl(),
                dataSource.getPoolName(),
                dataSource.getMaximumPoolSize(),
                dataSource.getMinimumIdle());

        return dataSource;
    }

    /**
     * Crée le bean JdbcTemplate pour le read replica.
     *
     * <p>
     * Ce template est utilisé uniquement pour les lectures (SELECT).
     * Il est configuré avec fetch size et query timeout optimisés.
     *
     * @param dataSource le DataSource du read replica
     * @return un JdbcTemplate configuré pour le read replica
     */
    @Bean(name = "readReplicaJdbcTemplate")
    public JdbcTemplate readReplicaJdbcTemplate(@Qualifier("readReplicaDataSource") DataSource dataSource) {
        log.info("Initialisation du JdbcTemplate read replica avec fetchSize={}, queryTimeout={}s",
                fetchSize, queryTimeout);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setFetchSize(fetchSize);
        jdbcTemplate.setQueryTimeout(queryTimeout);
        jdbcTemplate.setMaxRows(0);
        jdbcTemplate.setSkipResultsProcessing(false);

        return jdbcTemplate;
    }

    /**
     * Crée le bean NamedParameterJdbcTemplate pour le read replica.
     *
     * @param dataSource le DataSource du read replica
     * @return un NamedParameterJdbcTemplate pour le read replica
     */
    @Bean(name = "readReplicaNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate readReplicaNamedParameterJdbcTemplate(
            @Qualifier("readReplicaDataSource") DataSource dataSource) {
        log.info("Initialisation du NamedParameterJdbcTemplate read replica");
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Crée un RoutingDataSource qui route automatiquement les lectures vers le read replica
     * et les écritures vers le primary.
     *
     * <p>
     * Ce bean est marqué {@code @Primary} pour remplacer le primaryDataSource
     * et router automatiquement selon le contexte {@link ReadWriteRoutingContext}.
     *
     * <p>
     * Le routing est déterminé par :
     * <ul>
     *   <li>{@link ReadWriteRoutingContext#isReadOnly()} = true → read replica</li>
     *   <li>{@link ReadWriteRoutingContext#isReadOnly()} = false ou null → primary</li>
     * </ul>
     *
     * <p>
     * Utilisation avec annotation {@code @ReadOnly} :
     * <pre>{@code
     * @ReadOnly
     * public List<User> findAllUsers() {
     *     // Cette méthode utilisera automatiquement le read replica
     *     return jdbcTemplate.query("SELECT * FROM users", ...);
     * }
     * }</pre>
     *
     * <p>
     * Utilisation manuelle :
     * <pre>{@code
     * try {
     *     ReadWriteRoutingContext.setReadOnly(true);
     *     List<User> users = userRepository.findAll();
     * } finally {
     *     ReadWriteRoutingContext.clear();
     * }
     * }</pre>
     *
     * @param primaryDataSource la datasource primaire (écritures)
     * @param readReplicaDataSource la datasource read replica (lectures)
     * @return le RoutingDataSource configuré
     */
    @Bean(name = "routingDataSource")
    @Primary
    public DataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("readReplicaDataSource") DataSource readReplicaDataSource) {

        log.info("Initialisation du RoutingDataSource pour read replicas");

        org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource routingDataSource =
                new org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource() {
                    @Override
                    protected Object determineCurrentLookupKey() {
                        // Utiliser le contexte ThreadLocal pour déterminer la datasource
                        boolean readOnly = ReadWriteRoutingContext.isReadOnly();
                        String dataSourceKey = readOnly ? "replica" : "primary";
                        log.trace("Routing to datasource: {} (readOnly: {})", dataSourceKey, readOnly);
                        return dataSourceKey;
                    }
                };

        // Configurer les datasources cibles
        java.util.Map<Object, Object> targetDataSources = new java.util.HashMap<>();
        targetDataSources.put("primary", primaryDataSource);
        targetDataSources.put("replica", readReplicaDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource); // Par défaut : primary

        log.info("RoutingDataSource initialisé -> Primary: {}, Replica: {}, Default: primary",
                primaryDataSource.getClass().getSimpleName(),
                readReplicaDataSource.getClass().getSimpleName());

        return routingDataSource;
    }
}

