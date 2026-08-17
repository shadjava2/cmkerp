package cd.shad.erp.cmk.cmkerp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import cd.shad.erp.cmk.cmkerp.gateway.config.StockIntelligenceWebConfiguration;

/**
 * Application principale du module Gateway CMK ERP.
 *
 * <p>
 * Configuration du scan de composants pour inclure explicitement :
 * <ul>
 * <li>cd.shad.erp.cmk.cmkerp.gateway : composants du module gateway</li>
 * <li>cd.shad.erp.cmk.cmkerp.platform : composants du module platform (repositories, services,
 * configs)</li>
 * <li>cd.shad.erp.cmk.cmkerp.stocks : composants du module stocks (repositories, services,
 * configs)</li>
 * <li>cd.shad.erp.cmk.cmkerp.pos : composants du module POS (repositories, services,
 * configs)</li>
 * <li>cd.shad.erp.cmk.cmkerp.approvisionnements : composants du module approvisionnements
 * (repositories, services, configs)</li>
 * <li>cd.shad.erp.cmk.cmkerp.sharedkernel : composants partagés</li>
 * </ul>
 *
 * <p>
 * Auto-configurations exclues :
 * <ul>
 * <li>DataSourceAutoConfiguration : datasources configurées manuellement dans
 * PrimaryDataSourceConfig</li>
 * <li>DataSourceTransactionManagerAutoConfiguration : transaction managers configurés
 * manuellement</li>
 * <li>HibernateJpaAutoConfiguration : JPA/Hibernate auto-configuré (nécessaire pour le module
 * stocks)</li>
 * <li>JpaRepositoriesAutoConfiguration : Auto-configuration des repositories JPA désactivée (
 * paConfig gère explicitement les repositories)</li>
 * <li>SqlInitializationAutoConfiguration : Initialisation SQL désactivée</li>
 * <li>FlywayAutoConfiguration : désactivé — base legacy (cmkerp-v24prod) ; DDL stock-intelligence
 * via {@link cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceSchemaInitializer}</li>
 *
 * <li>WebFluxAutoConfiguration : WebFlux désactivé (WebMVC uniquement)</li>
 * <li>ReactiveWebServerFactoryAutoConfiguration : Serveur réactif désactivé</li>
 * <li>OAuth2ResourceServerAutoConfiguration : OAuth2 Resource Server désactivé (utilisation d'un
 * filtre JWT personnalisé via JwtAuthenticationFilter)</li>
 * </ul>
 */
@SpringBootApplication(
    scanBasePackages = {"cd.shad.erp.cmk.cmkerp.gateway", "cd.shad.erp.cmk.cmkerp.platform",
        "cd.shad.erp.cmk.cmkerp.stocks", "cd.shad.erp.cmk.cmkerp.pos",
        "cd.shad.erp.cmk.cmkerp.sharedkernel"},
    exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class, FlywayAutoConfiguration.class,
        WebFluxAutoConfiguration.class, ReactiveWebServerFactoryAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class})
@ComponentScan(
    basePackages = {"cd.shad.erp.cmk.cmkerp.gateway", "cd.shad.erp.cmk.cmkerp.platform",
        "cd.shad.erp.cmk.cmkerp.stocks", "cd.shad.erp.cmk.cmkerp.pos",
        "cd.shad.erp.cmk.cmkerp.sharedkernel"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = ".*\\.stocks\\.application\\.exception\\.GlobalExceptionHandler"),
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = "cd\\.shad\\.erp\\.cmk\\.cmkerp\\.stocks\\.stockintelligence\\..*")})
@Import(StockIntelligenceWebConfiguration.class)
@EnableScheduling // Active la planification des tâches
@EntityScan(basePackages = {"cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model",
    "cd.shad.erp.cmk.cmkerp.platform.notification.domain.model",
    "cd.shad.erp.cmk.cmkerp.platform.security.domain.model",
    "cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.model",
    "cd.shad.erp.cmk.cmkerp.stocks.domain.model",
    "cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model"}) // Configuration JPA déplacée
                                                                      // dans JpaConfig.java pour
                                                                      // utiliser explicitement
                                                                      // primaryDataSource
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
