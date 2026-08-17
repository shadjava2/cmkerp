package cd.shad.erp.cmk.cmkerp.platform.security;

/**
 * Constantes pour les noms et configurations des cookies d'authentification.
 *
 * <p>
 * Centralise les noms de cookies et leurs paramètres pour garantir la cohérence
 * entre la création et la suppression des cookies.
 */
public final class AuthCookies {
  private AuthCookies() {}

  /** Nom du cookie contenant le token d'accès JWT. */
  public static final String ACCESS = "cmk-auth-token";

  /** Nom du cookie contenant le token de rafraîchissement JWT. */
  public static final String REFRESH = "cmk-refresh-token";

  /**
   * Chemin du cookie (Path). CRITIQUE : doit être "/" pour que le cookie soit
   * accessible sur toutes les routes (y compris /dashboard).
   */
  public static final String PATH_ROOT = "/";

  /** Durée de vie du cookie d'accès en secondes (48 heures pour correspondre au token JWT). */
  public static final long ACCESS_MAX_AGE_SECONDS = 48 * 3600; // 48 heures (172800 secondes)

  /** Durée de vie du cookie d'accès avec "Se souvenir de moi" (48 heures - même durée que normale). */
  public static final long ACCESS_REMEMBER_ME_MAX_AGE_SECONDS = 48 * 3600; // 48 heures

  /** Durée de vie du cookie de rafraîchissement en secondes (48 heures). */
  public static final long REFRESH_MAX_AGE_SECONDS = 48 * 3600; // 48 heures (172800 secondes)

  /** Durée de vie du cookie de rafraîchissement avec "Se souvenir de moi" (48 heures - même durée que normale). */
  public static final long REFRESH_REMEMBER_ME_MAX_AGE_SECONDS = 48 * 3600; // 48 heures
}

