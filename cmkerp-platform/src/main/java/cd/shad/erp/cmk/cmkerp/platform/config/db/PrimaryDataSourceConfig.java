package cd.shad.erp.cmk.cmkerp.platform.config.db;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Configuration de la datasource primaire (CMK ERP - base principale).
 *
 * <p>
 * Cette configuration définit la datasource principale utilisée par l'application ERP. Elle est
 * mappée aux propriétés avec le préfixe {@code cmk.datasource.primary.*}.
 *
 * <p>
 * Rôle de cette datasource :
 * <ul>
 * <li>Base de données principale du système ERP CMK</li>
 * <li>Contient les données métier : utilisateurs, rôles, pharmacies, sites, etc.</li>
 * <li>Utilisée par défaut par tous les repositories JDBC du shared-kernel</li>
 * </ul>
 *
 * <p>
 * Configuration des propriétés (application-dev.yml) :
 *
 * <pre>{@code
 * cmk:
 *   datasource:
 *     primary:
 *       url: jdbc:mysql://localhost:3306/cmk_erp
 *       username: cmk_user
 *       password: cmk_pass
 *       driver-class-name: com.mysql.cj.jdbc.Driver
 *       maximum-pool-size: 10
 *       pool-name: CMK-ERP-PrimaryPool
 * }</pre>
 *
 * <p>
 * Les beans créés sont marqués avec {@code @Primary} pour être utilisés par défaut lorsque aucune
 * qualification n'est spécifiée.
 *
 *
 */
@Configuration
@EnableConfigurationProperties({PrimaryDataSourceProperties.class, DatabasePoolProperties.class})
public class PrimaryDataSourceConfig {

  private static final Logger log = LoggerFactory.getLogger(PrimaryDataSourceConfig.class);

  /**
   * Taille du fetch size pour les requêtes JDBC de la datasource primaire. Peut être configuré via
   * application.yml : platform.jdbc.primary.fetch-size Valeur par défaut : 250 (optimisé pour haute
   * performance).
   *
   * <p>
   * Recommandations :
   * <ul>
   * <li>Petites requêtes : 100-250</li>
   * <li>Grandes requêtes : 250-500</li>
   * <li>Très grandes requêtes : 500-1000</li>
   * </ul>
   */
  @Value("${platform.jdbc.primary.fetch-size:250}")
  private int fetchSize;

  /**
   * Timeout des requêtes en secondes pour la datasource primaire. Peut être configuré via
   * application.yml : platform.jdbc.primary.query-timeout Valeur par défaut : 30 secondes.
   *
   * <p>
   * Pour les requêtes longues (rapports, exports), augmenter cette valeur ou utiliser un timeout
   * spécifique dans le code.
   */
  @Value("${platform.jdbc.primary.query-timeout:30}")
  private int queryTimeout;

  /**
   * URL JDBC de la datasource primaire (fallback si PrimaryDataSourceProperties ne bind pas).
   * Utilisé comme solution de secours pour les tests.
   */
  @Value("${cmk.datasource.primary.url:}")
  private String fallbackJdbcUrl;

  /**
   * Crée le bean DataSource primaire (CMK ERP) à partir de la configuration Hikari.
   *
   * <p>
   * Les propriétés sont injectées automatiquement dans PrimaryDataSourceProperties depuis
   * application.yml/properties via le préfixe {@code cmk.datasource.primary.*}. Les propriétés du
   * pool sont injectées depuis {@code cmkerp.db.pool.*}.
   *
   * <p>
   * Ce bean est marqué {@code @Primary} pour être utilisé par défaut lorsque aucune qualification
   * n'est spécifiée dans les injections.
   *
   * @param properties les propriétés de configuration de la datasource (URL, username, etc.)
   * @param poolProperties les propriétés de configuration du pool HikariCP
   * @return le DataSource primaire
   */
  @Bean(name = "primaryDataSource")
  @Primary
  public DataSource primaryDataSource(PrimaryDataSourceProperties properties,
      DatabasePoolProperties poolProperties, Environment environment) {
    // Validation : vérifier que l'URL JDBC est configurée
    String jdbcUrl = properties.getJdbcUrl();

    // Fallback 1 : si le binding n'a pas fonctionné, essayer de récupérer depuis @Value
    if ((jdbcUrl == null || jdbcUrl.isBlank())
        && (fallbackJdbcUrl != null && !fallbackJdbcUrl.isBlank())) {
      log.warn(
          "PrimaryDataSourceProperties.getJdbcUrl() est null, utilisation de la valeur depuis @Value: {}",
          fallbackJdbcUrl);
      properties.setJdbcUrl(fallbackJdbcUrl);
      jdbcUrl = fallbackJdbcUrl;
    }

    // Fallback 2 : si toujours null, essayer de récupérer directement depuis l'environnement
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      String urlFromEnv = environment.getProperty("cmk.datasource.primary.url");
      if (urlFromEnv != null && !urlFromEnv.isBlank()) {
        log.warn(
            "PrimaryDataSourceProperties.getJdbcUrl() est null, utilisation de la valeur depuis Environment: {}",
            urlFromEnv);
        properties.setJdbcUrl(urlFromEnv);
        jdbcUrl = urlFromEnv;
      }
    }

    // Validation finale
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      String[] activeProfiles = environment.getActiveProfiles();
      String profilesStr = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "aucun";
      String urlFromEnv = environment.getProperty("cmk.datasource.primary.url");

      String errorMsg = String.format("Configuration datasource primaire manquante. "
          + "Vérifier que le profil Spring est activé (dev/prod/test) et que les propriétés "
          + "cmk.datasource.primary.url sont définies dans application-{profile}.yml. "
          + "Profil(s) actif(s): %s, URL depuis @Value: %s, URL depuis Environment: %s, URL depuis Properties: %s",
          profilesStr,
          fallbackJdbcUrl != null && !fallbackJdbcUrl.isBlank() ? fallbackJdbcUrl : "null",
          urlFromEnv != null ? urlFromEnv : "null", jdbcUrl != null ? jdbcUrl : "null");
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    // Créer un HikariConfig à partir des propriétés de datasource
    HikariConfig hikariConfig = new HikariConfig();

    // Copier les propriétés de base (URL, username, password, driver, etc.)
    hikariConfig.setJdbcUrl(properties.getJdbcUrl());
    hikariConfig.setUsername(properties.getUsername());
    hikariConfig.setPassword(properties.getPassword());
    hikariConfig.setDriverClassName(properties.getDriverClassName());
    hikariConfig.setPoolName(properties.getPoolName());
    hikariConfig.setAutoCommit(properties.isAutoCommit());
    hikariConfig.setRegisterMbeans(properties.isRegisterMbeans());
    if (properties.getLeakDetectionThreshold() > 0) {
      hikariConfig.setLeakDetectionThreshold(properties.getLeakDetectionThreshold());
    }

    // Appliquer les propriétés du pool depuis cmkerp.db.pool.*
    hikariConfig.setMaximumPoolSize(poolProperties.getMaxSize());
    hikariConfig.setMinimumIdle(poolProperties.getMinIdle());
    hikariConfig.setConnectionTimeout(poolProperties.getConnectionTimeoutMs());
    hikariConfig.setIdleTimeout(poolProperties.getIdleTimeoutMs());
    hikariConfig.setMaxLifetime(poolProperties.getMaxLifetimeMs());
    hikariConfig.setKeepaliveTime(poolProperties.getKeepaliveTimeMs());
    hikariConfig.setValidationTimeout(poolProperties.getValidationTimeoutMs());

    // Query de validation rapide pour détecter les connexions mortes
    // JDBC 4+ fait cela automatiquement, mais explicite = plus rapide et fiable
    hikariConfig.setConnectionTestQuery("SELECT 1");

    // Pour les tests : désactiver la validation de connexion au démarrage si le profil "test" est
    // actif
    String[] activeProfiles = environment.getActiveProfiles();
    boolean isTestProfile = java.util.Arrays.asList(activeProfiles).contains("test");
    if (isTestProfile) {
      // Désactiver complètement la validation de connexion au démarrage pour les tests
      // Utiliser Long.MAX_VALUE pour éviter l'échec de validation (timeout infini = pas d'échec
      // immédiat)
      hikariConfig.setInitializationFailTimeout(Long.MAX_VALUE);
      hikariConfig.setMinimumIdle(0); // Pas de connexions minimum pour les tests
      hikariConfig.setConnectionTimeout(60000); // Augmenter le timeout de connexion pour les tests
      log.info(
          "Profil 'test' détecté : validation de connexion au démarrage avec timeout infini pour HikariCP (initializationFailTimeout=MAX_VALUE)");
    }

    HikariDataSource dataSource = new HikariDataSource(hikariConfig);

    // Logs synthétiques pour monitoring (INFO uniquement)
    log.info(
        "Datasource primaire initialisée -> URL: {}, Pool: {} (max: {}, min: {}), Timeouts: conn={}ms, validation={}ms, idle={}ms, lifetime={}ms, keepalive={}ms",
        dataSource.getJdbcUrl(), dataSource.getPoolName(), dataSource.getMaximumPoolSize(),
        dataSource.getMinimumIdle(), dataSource.getConnectionTimeout(),
        dataSource.getValidationTimeout(), dataSource.getIdleTimeout(), dataSource.getMaxLifetime(),
        dataSource.getKeepaliveTime());

    // La vérification complète wait_timeout sera effectuée au démarrage par MySQLWaitTimeoutChecker
    log.debug(
        "Configuration maxLifetime: {}ms ({} min). Vérification automatique au démarrage par MySQLWaitTimeoutChecker.",
        dataSource.getMaxLifetime(), dataSource.getMaxLifetime() / 60000);

    return dataSource;
  }

  /**
   * Crée le bean JdbcTemplate pour la datasource primaire.
   *
   * <p>
   * Configuration appliquée :
   * <ul>
   * <li>Fetch size : optimise le chargement des résultats (batch)</li>
   * <li>Query timeout : évite les requêtes qui bloquent indéfiniment</li>
   * </ul>
   *
   * <p>
   * Ce bean est marqué {@code @Primary} pour être utilisé par défaut par les repositories JDBC du
   * shared-kernel.
   *
   * @param dataSource la datasource primaire
   * @return un JdbcTemplate configuré pour la datasource primaire
   */
  @Bean(name = "primaryJdbcTemplate")
  @Primary
  public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
    log.info("Initialisation du JdbcTemplate primaire avec fetchSize={}, queryTimeout={}s",
        fetchSize, queryTimeout);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.setFetchSize(fetchSize);
    jdbcTemplate.setQueryTimeout(queryTimeout);
    // Optimisations supplémentaires
    jdbcTemplate.setMaxRows(0); // Pas de limite de lignes par défaut
    jdbcTemplate.setSkipResultsProcessing(false); // Traitement des résultats activé

    return jdbcTemplate;
  }

  /**
   * Crée le bean NamedParameterJdbcTemplate pour la datasource primaire.
   *
   * <p>
   * Utile pour les requêtes complexes avec de nombreux paramètres, améliore la lisibilité et réduit
   * les erreurs de positionnement.
   *
   * <p>
   * Ce bean est marqué {@code @Primary} pour être utilisé par défaut.
   *
   * @param dataSource la datasource primaire
   * @return un NamedParameterJdbcTemplate pour la datasource primaire
   */
  @Bean(name = "primaryNamedParameterJdbcTemplate")
  @Primary
  public NamedParameterJdbcTemplate primaryNamedParameterJdbcTemplate(
      @Qualifier("primaryDataSource") DataSource dataSource) {
    log.info("Initialisation du NamedParameterJdbcTemplate primaire");
    return new NamedParameterJdbcTemplate(dataSource);
  }
}
