package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitaire d'extraction et de validation de tokens JWT (Authorization header + cookie).
 *
 * <p>
 * Priorité : Authorization header > cookie HttpOnly.
 */
public final class AuthTokenExtractor {
  private AuthTokenExtractor() {}

  private static final Logger log = LoggerFactory.getLogger(AuthTokenExtractor.class);

  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  // Cookie d'accès JWT (doit rester cohérent avec AuthCookies.ACCESS côté gateway)
  public static final String ACCESS_COOKIE_NAME = "cmk-auth-token";

  /**
   * Extrait le token JWT depuis la requête HTTP (header Authorization ou cookie HttpOnly).
   *
   * @param request la requête HTTP
   * @return le token JWT ou null si non trouvé
   */
  public static String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader(HEADER_AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      String token = authHeader.substring(BEARER_PREFIX.length());
      log.debug("Token JWT extrait depuis Authorization header");
      return token;
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (ACCESS_COOKIE_NAME.equals(cookie.getName())) {
          log.debug("Token JWT extrait depuis cookie HttpOnly: {}", ACCESS_COOKIE_NAME);
          return cookie.getValue();
        }
      }
    }

    log.debug("Aucun token JWT trouvé (ni Authorization header, ni cookie)");
    return null;
  }

  /**
   * Extrait l'ID utilisateur depuis le token JWT en respectant l'approche cookies-first.
   *
   * @param request la requête HTTP
   * @param jwtTokenProvider le provider JWT
   * @return l'ID utilisateur
   * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si le token est invalide
   */
  public static Long getCurrentUserId(HttpServletRequest request, JwtTokenProvider jwtTokenProvider) {
    String token = extractToken(request);
    if (token == null || token.isBlank()) {
      log.warn("Aucun token JWT trouvé (Authorization header + cookie)");
      throw new IllegalStateException("Utilisateur non authentifié");
    }

    if (!jwtTokenProvider.validateToken(token)) {
      log.warn("Token JWT invalide ou expiré");
      throw new IllegalStateException("Token JWT invalide");
    }

    Long userId = jwtTokenProvider.getUserIdFromToken(token);
    if (userId == null) {
      log.warn("Impossible d'extraire l'ID utilisateur du token JWT");
      throw new IllegalStateException("Impossible d'extraire l'ID utilisateur du token JWT");
    }

    log.debug("ID utilisateur extrait avec succès: {}", userId);
    return userId;
  }
}
