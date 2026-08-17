package cd.shad.erp.cmk.cmkerp.gateway.health;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * HealthIndicator personnalisé pour exposer les informations d'environnement dans l'endpoint
 * /actuator/health.
 *
 * <p>
 * Affiche :
 * <ul>
 * <li>L'environnement courant (DEV, PROD, UNKNOWN)</li>
 * <li>Les profils Spring actifs</li>
 * <li>Le nom de l'application</li>
 * <li>La version de l'application</li>
 * </ul>
 *
 * <p>
 * L'environnement est déterminé dynamiquement à partir des profils Spring actifs :
 * <ul>
 * <li>Si le profil "prod" est actif → env: "PROD"</li>
 * <li>Si le profil "dev" est actif → env: "DEV"</li>
 * <li>Sinon → env: "UNKNOWN"</li>
 * </ul>
 *
 * <p>
 * Configuration des propriétés (application.yml) :
 *
 * <pre>{@code
 * spring:
 *   application:
 *     name: cmkerp-gateway
 *
 * cmkerp:
 *   version: 4.1.1
 * }</pre>
 *

 */
@Component("appInfo")
public class EnvironmentHealthIndicator implements HealthIndicator {

  private final Environment environment;

  public EnvironmentHealthIndicator(Environment environment) {
    this.environment = environment;
  }

  @Override
  public Health health() {
    String[] activeProfiles = environment.getActiveProfiles();
    List<String> profiles = Arrays.asList(activeProfiles);

    String envLabel;
    if (profiles.contains("prod")) {
      envLabel = "PROD";
    } else if (profiles.contains("dev")) {
      envLabel = "DEV";
    } else {
      envLabel = "UNKNOWN";
    }

    String appVersion = environment.getProperty("cmkerp.version", "unknown");
    String appName = environment.getProperty("spring.application.name", "cmkerp-gateway");

    return Health.up().withDetail("env", envLabel).withDetail("activeProfiles", activeProfiles)
        .withDetail("appName", appName).withDetail("appVersion", appVersion).build();
  }
}

