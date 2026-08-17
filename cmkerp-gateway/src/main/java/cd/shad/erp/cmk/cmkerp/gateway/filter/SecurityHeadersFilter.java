package cd.shad.erp.cmk.cmkerp.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtre pour ajouter les headers de sécurité HTTP (hors environnement dev).
 *
 * <p>Headers ajoutés :
 * <ul>
 *   <li>X-Content-Type-Options: nosniff</li>
 *   <li>X-Frame-Options: DENY</li>
 *   <li>X-XSS-Protection: 1; mode=block</li>
 *   <li>Referrer-Policy: no-referrer-when-downgrade</li>
 * </ul>
 *
 * <p>Note: Strict-Transport-Security sera ajouté lors de la configuration HTTPS.
 */
@Component
@Order(2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final List<String> DEV_PROFILES = Arrays.asList("dev", "development");
    private final Environment environment;

    public SecurityHeadersFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // N'applique pas les headers de sécurité en dev
        if (isDevProfile()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Headers de sécurité
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "no-referrer-when-downgrade");

        // Strict-Transport-Security sera ajouté lors de la configuration HTTPS
        // response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        filterChain.doFilter(request, response);
    }

    private boolean isDevProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(profile -> DEV_PROFILES.contains(profile.toLowerCase()));
    }
}

