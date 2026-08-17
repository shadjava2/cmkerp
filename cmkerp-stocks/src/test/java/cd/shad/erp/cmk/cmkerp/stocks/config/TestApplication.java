package cd.shad.erp.cmk.cmkerp.stocks.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Classe principale Spring Boot pour les tests d'intégration du module stocks.
 *
 * <p>
 * Permet de charger le contexte Spring complet pour les tests d'intégration sans dépendre d'une
 * application principale externe.
 *
 * <p>
 * Auto-configurations exclues :
 * <ul>
 * <li>FlywayAutoConfiguration : Les tests utilisent schema.sql pour initialiser la base de
 * données</li>
 * <li>JpaRepositoriesAutoConfiguration : Auto-configuration des repositories JPA désactivée
 * (JpaConfig gère explicitement les repositories)</li>
 * <li>SqlInitializationAutoConfiguration : Initialisation SQL désactivée pour éviter la validation
 * de connexion au démarrage</li>
 * </ul>
 *
 * <p>
 * Note : Les repositories JPA sont configurés par
 * {@link cd.shad.erp.cmk.cmkerp.platform.config.JpaConfig} qui est chargé automatiquement via le
 * scan de packages. Ne pas redéfinir @EnableJpaRepositories ici pour éviter les conflits de beans.
 */
@SpringBootApplication(scanBasePackages = "cd.shad.erp.cmk.cmkerp",
    exclude = {FlywayAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class})
@EntityScan(basePackages = {"cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model",
    "cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model",
    "cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model",
    "cd.shad.erp.cmk.cmkerp.stocks.domain.model",
    "cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model"})
@Import(TestSecurityConfig.class)
public class TestApplication {

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(TestApplication.class);
    // Activer le profil 'test' par défaut pour charger application-test.yml
    app.setAdditionalProfiles("test");
    app.run(args);
  }
}


