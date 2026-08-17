package cd.shad.erp.cmk.cmkerp.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre pour générer et propager un Correlation ID pour le traçage des requêtes.
 *
 * <p>Fonctionnalités :
 * <ul>
 *   <li>Génère un UUID si l'en-tête {@code X-Correlation-Id} est absent</li>
 *   <li>Stocke le Correlation ID dans le MDC (clé {@code correlationId})</li>
 *   <li>Renvoie le Correlation ID dans la réponse HTTP via l'en-tête {@code X-Correlation-Id}</li>
 * </ul>
 *
 * <p>Le Correlation ID permet de tracer une requête à travers tous les logs et services.
 * Il est automatiquement inclus dans les logs via le pattern Logback configuré avec {@code %X{correlationId}}.
 *
 * <p>Ordre : 1 (avant les autres filtres pour avoir le correlationId disponible partout)
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Récupère ou génère le Correlation ID
            String correlationId = getOrGenerateCorrelationId(request);

            // Stocke dans le MDC pour les logs
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Ajoute dans la réponse HTTP
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue la chaîne de filtres
            filterChain.doFilter(request, response);
        } finally {
            // Nettoie le MDC après la requête pour éviter les fuites mémoire
            MDC.clear();
        }
    }

    /**
     * Récupère le Correlation ID depuis l'en-tête HTTP ou en génère un nouveau.
     *
     * @param request la requête HTTP
     * @return le Correlation ID (UUID)
     */
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        return correlationId;
    }
}
