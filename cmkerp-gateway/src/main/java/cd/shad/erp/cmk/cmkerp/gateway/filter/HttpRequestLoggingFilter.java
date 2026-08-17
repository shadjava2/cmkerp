package cd.shad.erp.cmk.cmkerp.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtre de logging HTTP structuré en JSON pour observabilité haute charge.
 *
 * <p>
 * Logge chaque requête HTTP dans un format JSON structuré avec :
 * <ul>
 * <li>correlationId (depuis MDC)</li>
 * <li>timestamp</li>
 * <li>method, path, queryString</li>
 * <li>status, duration</li>
 * <li>userId/username (si disponible depuis le contexte de sécurité)</li>
 * <li>clientIp</li>
 * <li>userAgent</li>
 * </ul>
 *
 * <p>
 * Format de log (une seule ligne JSON par requête) :
 * <pre>{@code
 * {
 *   "timestamp": "2025-01-15T10:30:45.123Z",
 *   "correlationId": "550e8400-e29b-41d4-a716-446655440000",
 *   "method": "GET",
 *   "path": "/api/v1/sites",
 *   "queryString": "page=0&size=10",
 *   "status": 200,
 *   "duration": 45,
 *   "userId": 123,
 *   "username": "admin",
 *   "clientIp": "192.168.1.100",
 *   "userAgent": "Mozilla/5.0..."
 * }
 * }</pre>
 *
 * <p>
 * Ordre : 2 (après CorrelationIdFilter pour avoir le correlationId dans MDC)
 */
@Slf4j
@Component
@Order(2)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

  private static final String LOGGER_NAME = "http.request";
  private static final org.slf4j.Logger httpLogger = org.slf4j.LoggerFactory.getLogger(LOGGER_NAME);

  private final ObjectMapper objectMapper;
  private final JwtTokenProvider jwtTokenProvider;

  public HttpRequestLoggingFilter(ObjectMapper objectMapper, JwtTokenProvider jwtTokenProvider) {
    this.objectMapper = objectMapper;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // Wrapper pour pouvoir lire le body de la réponse après traitement
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    long startTime = System.currentTimeMillis();
    String correlationId = MDC.get("correlationId");

    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      logHttpRequest(wrappedRequest, wrappedResponse, duration, correlationId);
      // Copier le body de la réponse vers la réponse originale
      wrappedResponse.copyBodyToResponse();
    }
  }

  /**
   * Logge la requête HTTP au format JSON structuré.
   */
  private void logHttpRequest(
      ContentCachingRequestWrapper request,
      ContentCachingResponseWrapper response,
      long duration,
      String correlationId) {

    try {
      Map<String, Object> logData = new HashMap<>();
      logData.put("timestamp", Instant.now().toString());
      logData.put("correlationId", correlationId != null ? correlationId : "unknown");
      logData.put("method", request.getMethod());
      logData.put("path", request.getRequestURI());
      if (request.getQueryString() != null) {
        logData.put("queryString", request.getQueryString());
      }
      logData.put("status", response.getStatus());
      logData.put("duration", duration);

      // Informations utilisateur (depuis le contexte de sécurité)
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
        String username = auth.getName();
        if (username != null) {
          logData.put("username", username);
        }
        // Extraire userId depuis le JWT token si disponible
        Long userId = extractUserIdFromRequest(request);
        if (userId != null) {
          logData.put("userId", userId);
        }
      }

      // Informations client
      logData.put("clientIp", getClientIp(request));
      String userAgent = request.getHeader("User-Agent");
      if (userAgent != null) {
        logData.put("userAgent", userAgent);
      }

      // Log en JSON (une seule ligne)
      String jsonLog = objectMapper.writeValueAsString(logData);
      httpLogger.info(jsonLog);

    } catch (JsonProcessingException e) {
      // Fallback en cas d'erreur de sérialisation JSON
      log.warn("Erreur lors de la sérialisation du log HTTP: {}", e.getMessage());
      httpLogger.info("HTTP {} {} {} {}ms correlationId={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          duration,
          correlationId);
    }
  }

  /**
   * Extrait le userId depuis le JWT token dans l'en-tête Authorization.
   */
  private Long extractUserIdFromRequest(HttpServletRequest request) {
    try {
      String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        if (jwtTokenProvider.validateToken(token)) {
          return jwtTokenProvider.getUserIdFromToken(token);
        }
      }
    } catch (Exception e) {
      // Ignorer les erreurs silencieusement pour ne pas perturber le logging
      log.debug("Impossible d'extraire userId du token JWT: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Récupère l'IP réelle du client (gère les proxies/load balancers).
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      // Prendre la première IP (client réel)
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Ne pas logger les requêtes vers Actuator (trop verbeux)
    String path = request.getRequestURI();
    return path.startsWith("/actuator");
  }
}
