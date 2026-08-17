package cd.shad.erp.cmk.cmkerp.platform.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Classe principale Spring Boot pour les tests d'intégration du module platform.
 *
 * <p>
 * Permet de charger le contexte Spring complet pour les tests d'intégration sans dépendre d'une
 * application principale externe.
 *
 * <p>
 * Auto-configurations exclues :
 * <ul>
 * <li>FlywayAutoConfiguration : Les tests utilisent schema.sql pour initialiser la base de données</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "cd.shad.erp.cmk.cmkerp",
    exclude = {FlywayAutoConfiguration.class})
@EntityScan(basePackages = {"cd.shad.erp.cmk.cmkerp.platform.site.domain.model",
    "cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model",
    "cd.shad.erp.cmk.cmkerp.platform.notification.domain.model",
    "cd.shad.erp.cmk.cmkerp.platform.security.domain.model"})
@EnableJpaRepositories(basePackages = {"cd.shad.erp.cmk.cmkerp.platform.site.domain.repository",
    "cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository",
    "cd.shad.erp.cmk.cmkerp.platform.notification.domain.repository",
    "cd.shad.erp.cmk.cmkerp.platform.security.domain.repository"})
public class TestApplication {

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(TestApplication.class);
    // Activer le profil 'test' par défaut pour charger application-test.yml
    app.setAdditionalProfiles("test");
    app.run(args);
  }
}

