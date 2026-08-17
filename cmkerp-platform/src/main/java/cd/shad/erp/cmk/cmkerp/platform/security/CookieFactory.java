package cd.shad.erp.cmk.cmkerp.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

/**
 * Factory pour créer des cookies d'authentification avec des attributs cohérents.
 *
 * <p>
 * Garantit que tous les cookies d'authentification sont créés avec les mêmes
 * attributs (Path="/", HttpOnly, SameSite=Lax) pour éviter les problèmes de
 * boucle de connexion. Les cookies host-only (sans Domain) fonctionnent en localhost et en IP.
 */
public final class CookieFactory {
  private CookieFactory() {}

  /**
   * Détecte si la requête est sécurisée (HTTPS), y compris derrière un reverse proxy.
   */
  public static boolean isSecure(HttpServletRequest request) {
    String proto = request.getHeader("x-forwarded-proto");
    if (proto != null) {
      return "https".equalsIgnoreCase(proto);
    }
    return request.isSecure();
  }

  /**
   * Crée un cookie HttpOnly pour le token d'accès.
   *
   * @param token le token JWT d'accès
   * @param secure true si le cookie doit être sécurisé (HTTPS), false en dev
   *               (HTTP)
   * @param rememberMe true si "Se souvenir de moi" est activé (durée prolongée)
   * @return ResponseCookie configuré avec Path="/" pour être accessible sur tout le domaine
   */
  public static ResponseCookie accessCookie(String token, boolean secure, boolean rememberMe) {
    long maxAge = rememberMe
        ? AuthCookies.ACCESS_REMEMBER_ME_MAX_AGE_SECONDS
        : AuthCookies.ACCESS_MAX_AGE_SECONDS;

    // SameSite: Lax en dev (HTTP), None en prod si cross-origin (nécessite Secure=true)
    String sameSite = secure ? "None" : "Lax";

    return ResponseCookie.from(AuthCookies.ACCESS, token)
        .httpOnly(true)
        .secure(secure) // dev: false, prod: true
        .sameSite(sameSite) // Lax en dev, None en prod (si cross-origin)
        .path(AuthCookies.PATH_ROOT) // ✅ CRITICAL: "/" pour être accessible sur /cmkerp-gateway/*
        .maxAge(maxAge)
        .build();
  }

  /**
   * Crée un cookie HttpOnly pour le token d'accès (surcharge sans rememberMe pour compatibilité).
   *
   * @param token le token JWT d'accès
   * @param secure true si le cookie doit être sécurisé (HTTPS), false en dev
   *               (HTTP)
   * @return ResponseCookie configuré avec Path="/"
   */
  public static ResponseCookie accessCookie(String token, boolean secure) {
    return accessCookie(token, secure, false);
  }

  /**
   * Crée un cookie HttpOnly pour le token de rafraîchissement.
   *
   * @param token le token JWT de rafraîchissement
   * @param secure true si le cookie doit être sécurisé (HTTPS), false en dev
   *               (HTTP)
   * @param rememberMe true si "Se souvenir de moi" est activé (durée prolongée)
   * @return ResponseCookie configuré avec Path="/" pour être accessible sur tout le domaine
   */
  public static ResponseCookie refreshCookie(String token, boolean secure, boolean rememberMe) {
    long maxAge = rememberMe
        ? AuthCookies.REFRESH_REMEMBER_ME_MAX_AGE_SECONDS
        : AuthCookies.REFRESH_MAX_AGE_SECONDS;

    // SameSite: Lax en dev (HTTP), None en prod si cross-origin (nécessite Secure=true)
    String sameSite = secure ? "None" : "Lax";

    return ResponseCookie.from(AuthCookies.REFRESH, token)
        .httpOnly(true)
        .secure(secure) // dev: false, prod: true
        .sameSite(sameSite) // Lax en dev, None en prod (si cross-origin)
        .path(AuthCookies.PATH_ROOT) // ✅ CRITICAL: "/" pour être accessible sur /cmkerp-gateway/*
        .maxAge(maxAge)
        .build();
  }

  /**
   * Crée un cookie HttpOnly pour le token de rafraîchissement (surcharge sans rememberMe pour compatibilité).
   *
   * @param token le token JWT de rafraîchissement
   * @param secure true si le cookie doit être sécurisé (HTTPS), false en dev
   *               (HTTP)
   * @return ResponseCookie configuré avec Path="/"
   */
  public static ResponseCookie refreshCookie(String token, boolean secure) {
    return refreshCookie(token, secure, false);
  }

  /**
   * Crée un cookie pour supprimer le token d'accès.
   *
   * <p>
   * IMPORTANT : Le Path doit être identique à celui utilisé lors de la création
   * du cookie pour que la suppression fonctionne.
   *
   * @param secure true si le cookie doit être sécurisé (HTTPS), false en dev
   *               (HTTP)
   * @return ResponseCookie avec maxAge=0 et Path="/"
   */
  public static ResponseCookie deleteAccess(boolean secure) {
    String sameSite = secure ? "None" : "Lax";
    return ResponseCookie.from(AuthCookies.ACCESS, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite) // Doit correspondre au cookie créé
        .path(AuthCookies.PATH_ROOT) // ✅ CRITICAL (must match set cookie)
        .maxAge(0)
        .build();
  }

  /**
   * Crée un cookie pour supprimer le token de rafraîchissement.
   *
   * <p>
   * IMPORTANT : Le Path doit être identique à celui utilisé lors de la création
   * du cookie pour que la suppression fonctionne.
   *
   * @param secure true si le cookie doit être sécurisé (HTTPS), false en dev
   *               (HTTP)
   * @return ResponseCookie avec maxAge=0 et Path="/"
   */
  public static ResponseCookie deleteRefresh(boolean secure) {
    String sameSite = secure ? "None" : "Lax";
    return ResponseCookie.from(AuthCookies.REFRESH, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite) // Doit correspondre au cookie créé
        .path(AuthCookies.PATH_ROOT) // ✅ CRITICAL (must match set cookie)
        .maxAge(0)
        .build();
  }

  /**
   * Cookie HttpOnly host-only (sans Domain) — localhost et IP (192.168.x.x).
   */
  public static ResponseCookie httpOnlyCookie(
      HttpServletRequest request,
      String name,
      String value,
      long maxAgeSeconds,
      String path) {
    boolean secure = isSecure(request);
    String sameSite = secure ? "None" : "Lax";

    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secure)
        .path(path == null ? AuthCookies.PATH_ROOT : path)
        .maxAge(maxAgeSeconds)
        .sameSite(sameSite)
        .build();
  }

  /**
   * Supprime un cookie (maxAge=0), même path que à la création.
   */
  public static ResponseCookie clearCookie(HttpServletRequest request, String name, String path) {
    boolean secure = isSecure(request);
    String sameSite = secure ? "None" : "Lax";

    return ResponseCookie.from(name, "")
        .httpOnly(true)
        .secure(secure)
        .path(path == null ? AuthCookies.PATH_ROOT : path)
        .maxAge(0)
        .sameSite(sameSite)
        .build();
  }
}

