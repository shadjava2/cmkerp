package cd.shad.erp.cmk.cmkerp.gateway.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.HEALTH_BASE;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour les endpoints de santé de l'API v1.
 *
 * <p>
 * Expose un endpoint de santé versionné sous /api/v1/health qui retourne un résumé du statut de
 * l'application. Cet endpoint peut être utilisé par les load balancers, orchestrateurs (K8s) ou
 * outils de monitoring pour vérifier la disponibilité de l'API.
 *
 * <p>
 * <strong>Note :</strong> L'endpoint Actuator /actuator/health reste disponible en parallèle pour
 * la compatibilité avec les outils de monitoring standard.
 *
 * <p>
 * Endpoints disponibles :
 * <ul>
 * <li>GET /api/v1/health : Statut de santé de l'API (versionnée)</li>
 * </ul>
 *

 */
@RestController
@RequestMapping(HEALTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Gateway - Health", description = "Endpoints de santé et monitoring")
public class HealthController {

  private final HealthEndpoint healthEndpoint;

  /**
   * Retourne le statut de santé de l'application.
   *
   * <p>
   * Cet endpoint retourne :
   * <ul>
   * <li>Un statut HTTP 200 si l'application est UP</li>
   * <li>Un statut HTTP 503 si l'application est DOWN</li>
   * <li>Les détails de santé (base de données, etc.) dans le corps de la réponse</li>
   * </ul>
   *
   * <p>
   * Format de réponse :
   *
   * <pre>{@code
   * {
   *   "status": "UP",
   *   "components": {
   *     "db": { "status": "UP" },
   *     "appInfo": { "status": "UP", "details": {...} }
   *   }
   * }
   * }</pre>
   *
   * @return ResponseEntity avec le statut de santé
   */
  @GetMapping
  @Operation(summary = "Vérifie le statut de santé de l'application",
      description = "Retourne le statut de santé de l'API et de ses composants (base de données, etc.)")
  public ResponseEntity<Map<String, Object>> health() {
    HealthComponent healthComponent = healthEndpoint.health();

    Map<String, Object> response = new HashMap<>();
    Status status = healthComponent.getStatus();
    response.put("status", status.getCode());

    // Extraire les détails/composants si c'est une instance de Health
    if (healthComponent instanceof Health health) {
      // Les détails de Health contiennent les composants
      Map<String, Object> details = health.getDetails();
      response.put("components", details != null ? details : new HashMap<>());
    } else {
      // Si ce n'est pas une Health, créer un map simple avec le statut
      Map<String, Object> components = new HashMap<>();
      components.put("status", status.getCode());
      response.put("components", components);
    }

    // Retourner 503 si DOWN, 200 sinon
    return ResponseEntity.status(status.getCode().equals("UP") ? 200 : 503).body(response);
  }
}

