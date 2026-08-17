package cd.shad.erp.cmk.cmkerp.gateway.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

/**
 * Filtre d'authentification JWT qui intercepte les requêtes HTTP
 * et valide les tokens JWT depuis l'en-tête Authorization OU le cookie HttpOnly.
 *
 * <p>
 * Facebook-Grade 9/10 : Ce filtre :
 * <ul>
 * <li>Extrait le token depuis l'en-tête Authorization (format: "Bearer &lt;token&gt;") OU le cookie "cmk-auth-token"</li>
 * <li>Priorité : Authorization header > Cookie (pour compatibilité)</li>
 * <li>Valide le token via JwtTokenProvider</li>
 * <li>Charge les informations de l'utilisateur avec les permissions depuis le token JWT</li>
 * <li>Place l'authentification dans le SecurityContext</li>
 * </ul>
 *
 * <p>
 * Optimisations :
 * <ul>
 * <li>Les permissions sont extraites directement du token JWT (pas de requête DB)</li>
 * <li>Support des cookies HttpOnly pour une sécurité renforcée</li>
 * <li>Logs de debug pour le troubleshooting</li>
 * <li>Gestion robuste des erreurs</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtUserDetailsService jwtUserDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String requestPath = request.getRequestURI();
    String token = null;

    // ✅ Facebook-Grade 9/10 : Lire le token depuis l'en-tête Authorization OU le cookie HttpOnly
    // Priorité : Authorization header > Cookie (pour compatibilité avec les clients qui envoient les deux)
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      token = authHeader.substring(7);
    } else {
      // Si pas d'en-tête Authorization, essayer de lire depuis le cookie HttpOnly
      jakarta.servlet.http.Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (jakarta.servlet.http.Cookie cookie : cookies) {
          if ("cmk-auth-token".equals(cookie.getName())) {
            token = cookie.getValue();
            if (log.isDebugEnabled()) {
              log.debug("Token JWT extrait depuis le cookie cmk-auth-token pour {}", requestPath);
            }
            break;
          }
        }
      }
    }

    if (token != null) {

      try {
        if (jwtTokenProvider.validateToken(token)) {
          String username = jwtTokenProvider.getUsernameFromToken(token);

          // Facebook-Grade : Charger les permissions depuis le token JWT (pas de requête DB)
          // Cela évite une requête DB supplémentaire à chaque requête HTTP
          var userDetails = jwtUserDetailsService.loadUserByUsernameWithToken(username, token);

          if (userDetails != null) {
            var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (log.isDebugEnabled()) {
              log.debug("Authentification réussie pour {} sur {} - {} autorités chargées",
                  username, requestPath, userDetails.getAuthorities().size());
            }
          } else {
            if (log.isWarnEnabled()) {
              log.warn("UserDetails null pour l'utilisateur {} sur {}", username, requestPath);
            }
            SecurityContextHolder.clearContext();
          }
        } else {
          // Token invalide ou expiré : nettoyer le SecurityContext
          if (log.isDebugEnabled()) {
            log.debug("Token invalide ou expiré pour la requête {}", requestPath);
          }
          SecurityContextHolder.clearContext();
        }
      } catch (Exception e) {
        // En cas d'erreur lors de la validation (token malformé, expiré, etc.)
        if (log.isDebugEnabled()) {
          log.debug("Erreur lors de la validation du token pour {}: {}", requestPath, e.getMessage());
        }
        SecurityContextHolder.clearContext();
      }
    } else {
      // Pas de token (ni dans Authorization header, ni dans cookie) : nettoyer le SecurityContext
      if (log.isTraceEnabled()) {
        log.trace("Pas de token JWT dans la requête {} (ni Authorization header, ni cookie)", requestPath);
      }
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}

