package cd.shad.erp.cmk.cmkerp.gateway.metrics;

import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Intercepteur pour collecter les métriques HTTP avec corrélation des logs.
 *
 * <p>
 * Collecte les métriques suivantes :
 * <ul>
 * <li>Timer : Durée des requêtes HTTP par endpoint</li>
 * <li>Counter : Nombre de requêtes par endpoint et status code</li>
 * <li>Counter : Nombre d'erreurs par endpoint</li>
 * </ul>
 *
 * <p>
 * Les métriques incluent les tags :
 * <ul>
 * <li>method : Méthode HTTP (GET, POST, etc.)</li>
 * <li>uri : Pattern de l'URI (normalisé, sans IDs)</li>
 * <li>status : Code de statut HTTP</li>
 * <li>correlationId : ID de corrélation pour tracer les requêtes</li>
 * </ul>
 */
@Component
public class HttpMetricsInterceptor implements HandlerInterceptor {

  private static final String TIMER_NAME = "cmkerp.http.request.duration";
  private static final String COUNTER_NAME = "cmkerp.http.request.total";
  private static final String ERROR_COUNTER_NAME = "cmkerp.http.request.errors";

  private final MeterRegistry meterRegistry;
  private final ThreadLocal<Timer.Sample> timerSample = new ThreadLocal<>();

  public HttpMetricsInterceptor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    // Démarrer le timer
    timerSample.set(Timer.start(meterRegistry));
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, @Nullable Exception ex) {
    try {
      Timer.Sample sample = timerSample.get();
      if (sample == null) {
        return;
      }

      String method = request.getMethod();
      String uri = normalizeUri(request.getRequestURI());
      int status = response.getStatus();
      String correlationId = MDC.get("correlationId");

      // Arrêter le timer avec tags
      Timer.Builder timerBuilder = Timer.builder(TIMER_NAME).description("Durée des requêtes HTTP")
          .tag("method", method).tag("uri", uri).tag("status", String.valueOf(status));

      if (correlationId != null) {
        timerBuilder.tag("correlationId", correlationId);
      }

      sample.stop(timerBuilder.register(meterRegistry));

      // Compter les requêtes totales
      Counter.Builder counterBuilder =
          Counter.builder(COUNTER_NAME).description("Nombre total de requêtes HTTP")
              .tag("method", method).tag("uri", uri).tag("status", String.valueOf(status));

      if (correlationId != null) {
        counterBuilder.tag("correlationId", correlationId);
      }

      counterBuilder.register(meterRegistry).increment();

      // Compter les erreurs (4xx et 5xx)
      if (status >= 400) {
        Counter.Builder errorBuilder =
            Counter.builder(ERROR_COUNTER_NAME).description("Nombre d'erreurs HTTP")
                .tag("method", method).tag("uri", uri).tag("status", String.valueOf(status));

        if (correlationId != null) {
          errorBuilder.tag("correlationId", correlationId);
        }

        errorBuilder.register(meterRegistry).increment();
      }

    } finally {
      timerSample.remove();
    }
  }

  /**
   * Normalise l'URI pour regrouper les requêtes similaires (remplace les IDs par {id}).
   *
   * <p>
   * Exemples :
   * <ul>
   * <li>/api/v1/users/123 → /api/v1/users/{id}</li>
   * <li>/api/v1/stocks/products/456/detail → /api/v1/stocks/products/{id}/detail</li>
   * </ul>
   */
  private String normalizeUri(String uri) {
    if (uri == null || uri.isEmpty()) {
      return uri;
    }

    // Remplacer les IDs numériques par {id}
    String normalized = uri.replaceAll("/\\d+", "/{id}");

    // Remplacer les UUIDs par {id}
    normalized = normalized
        .replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}");

    return normalized;
  }
}
