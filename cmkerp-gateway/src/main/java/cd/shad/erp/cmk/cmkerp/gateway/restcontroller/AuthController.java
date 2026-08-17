package cd.shad.erp.cmk.cmkerp.gateway.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.AUTH_BASE;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cd.shad.erp.cmk.cmkerp.gateway.dto.request.ChangePasswordRequest;
import cd.shad.erp.cmk.cmkerp.gateway.dto.request.InitPasswordRequest;
import cd.shad.erp.cmk.cmkerp.gateway.dto.request.LoginRequest;
import cd.shad.erp.cmk.cmkerp.gateway.dto.request.RefreshTokenRequest;
import cd.shad.erp.cmk.cmkerp.gateway.dto.response.AuthResponse;
import cd.shad.erp.cmk.cmkerp.gateway.security.service.JwtBlacklistService;
import cd.shad.erp.cmk.cmkerp.gateway.service.AuthService;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UtilisateurResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.AuthCookies;
import cd.shad.erp.cmk.cmkerp.platform.security.CookieFactory;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.UtilisateurQueryService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST pour l'authentification.
 *
 * <p>
 * Expose les endpoints d'authentification de l'API :
 * <ul>
 * <li>POST /api/v1/auth/login : authentification et génération de tokens JWT</li>
 * <li>GET /api/v1/auth/me : récupère les informations de l'utilisateur connecté</li>
 * <li>POST /api/v1/auth/change-password : changement de mot de passe pour l'utilisateur
 * connecté</li>
 * </ul>
 */
@RestController
@RequestMapping(AUTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Gateway - Authentification", description = "Authentification, sécurité & entrée API")
@Validated
@Slf4j
public class AuthController {

  private final AuthService authService;
  private final JwtTokenProvider jwtTokenProvider;
  private final UtilisateurQueryService utilisateurQueryService;
  private final JwtBlacklistService jwtBlacklistService;

  /**
   * Profil Spring actif (dev, prod, etc.) Utilisé pour déterminer si le cookie doit être sécurisé.
   */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  // ✅ Facebook-Grade 10/10 : Constantes pour éviter la duplication de littéraux
  private static final String COOKIE_AUTH_TOKEN = "cmk-auth-token";
  private static final String COOKIE_REFRESH_TOKEN = "cmk-refresh-token";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  /**
   * Authentifie un utilisateur et retourne les tokens JWT.
   *
   * <p>
   * Facebook-Grade : Crée également un cookie HttpOnly pour garantir la stabilité de la session. Le
   * cookie est créé côté backend pour être disponible immédiatement dans toutes les requêtes
   *
   *
   * <p>
   * Valide les credentials (username/password) et génère :
   * <ul>
   * <li>Un token d'accès (accessToken) : utilisé pour les requêtes API</li>
   * <li>Un token de rafraîchissement (refreshToken) : utilisé pour renouveler l'accessToken</li>
   * <li>Les permissions de l'utilisateur (user) : informations de sécurité incluant
   * initPassword</li>
   * </ul>
   *
   *
   * <p>
   * La réponse JSON (AuthResponse) contient :
   * <ul>
   * <li>accessToken : token JWT pour les requêtes API</li>
   * <li>refreshToken : token pour renouveler l'accessToken</li>
   * <li>user : UserPermissions contenant userId, username, initPassword (boolean), permissions,
   * etc.</li>
   * </ul>
   *
   *
   * <p>
   * Le cookie HttpOnly est créé avec :
   * <ul>
   * <li>Nom : cmk-auth-token (cohérent avec le frontend Next.js)</li>
   * <li>httpOnly : true (sécurité - non accessible via JavaScript)</li>
   * <li>secure : false en dev, true en prod (HTTPS requis en production)</li>
   * <li>sameSite : Lax (protection CSRF)</li>
   * <li>path : / (disponible sur tout le domaine)</li>
   * <li>maxAge : 7 jours (604800 secondes)</li>
   * </ul>
   *
   * <p>
   * Utilisation :
   * <ol>
   * <li>Appeler cet endpoint avec username/password</li>
   * <li>Récupérer l'accessToken de la réponse JSON</li>
   * <li>Le cookie HttpOnly est automatiquement créé et envoyé au client</li>
   * <li>Inclure l'accessToken dans l'en-tête Authorization : {@code Bearer <accessToken>}</li>
   * </ol>
   *
   * @param request les credentials (username, password)
   * @return AuthResponse contenant les tokens et les permissions, avec cookie HttpOnly dans les
   *         headers Set-Cookie
   */
  @PostMapping("/login")
  @Operation(summary = "Authentifie un utilisateur et génère les tokens JWT")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    log.debug("Tentative de connexion pour l'utilisateur: {} (rememberMe={})", request.username(),
        request.rememberMe());

    // Authentifier l'utilisateur et générer les tokens avec support pour "Se souvenir de moi"
    boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
    AuthResponse response = authService.login(request.username(), request.password(), rememberMe);

    // ✅ Utiliser CookieFactory pour garantir Path="/" et cohérence
    // Utiliser le paramètre rememberMe déjà extrait
    boolean secure = isCookieSecure();
    var accessCookie = CookieFactory.accessCookie(response.accessToken(), secure, rememberMe);
    var refreshCookie = CookieFactory.refreshCookie(response.refreshToken(), secure, rememberMe);

    log.info(
        "✅ Login réussi pour l'utilisateur: {} (secure={}, rememberMe={}) - Cookies créés: accessToken (maxAge={}s), refreshToken (maxAge={}s)",
        request.username(), secure, rememberMe,
        rememberMe ? AuthCookies.ACCESS_REMEMBER_ME_MAX_AGE_SECONDS
            : AuthCookies.ACCESS_MAX_AGE_SECONDS,
        rememberMe ? AuthCookies.REFRESH_REMEMBER_ME_MAX_AGE_SECONDS
            : AuthCookies.REFRESH_MAX_AGE_SECONDS);

    // Vérifier que initPassword est bien présent dans la réponse
    if (response.user() != null) {
      log.debug("initPassword dans UserPermissions: {}", response.user().isInitPassword());
    } else {
      log.warn("UserPermissions est null dans AuthResponse pour l'utilisateur: {}",
          request.username());
    }

    // ✅ CRITICAL: Retourner la réponse avec les cookies dans les headers
    // Les cookies doivent être envoyés avec les bons attributs (Path, SameSite, Secure)
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(response);
  }

  /**
   * Change le mot de passe de l'utilisateur connecté.
   *
   * <p>
   * L'utilisateur doit être authentifié (JWT token requis). L'ancien mot de passe est optionnel
   * pour permettre le c
   *
   * @param request la requête contenant l'ancien et le nouveau mot de passe
   * @param httpRequest la requête HTTP pour extraire le token JWT
   * @return ResponseEntity avec statut 204 (No Content) en cas de succès
   */
  /**
   * Récupère les informations de l'utilisateur connecté.
   *
   * <p>
   * L'utilisateur doit être authentifié (JWT token requis). Retourne les informations complètes de
   * l'utilisateur, y
   *
   * @param httpRequest la requête HTTP pour extraire le token JWT
   * @return ResponseEntity contenant les informations de l'utilisateur
   * @throws BusinessException si l'utilisateur n'est pas authentifié
   */
  /**
   * Vérifie l'état de la session côté serveur (source de vérité unique).
   *
   * <p>
   * Cet endpoint permet au frontend de vérifier si l'utilisateur est authentifié sans dépendre du
   * store Zustand ou du localStorage. Le cookie HttpOnly est vérifié automatiquement par Spring
   * Security via JwtAuthenticationFilter.
   *
   * @param authentication l'authentification Spring Security (injectée automatiquement)
   * @return ResponseEntity avec authenticated=true si session valide, 401 sinon
   */
  @GetMapping("/session")
  @Operation(summary = "Vérifie l'état de la session (source de vérité serveur)")
  public ResponseEntity<Map<String, Object>> session(Authentication authentication,
      HttpServletRequest httpRequest) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("authenticated", true);
    payload.put("username", authentication.getName());

    // Extraire userId depuis le token JWT si disponible
    Long userId = extractUserIdFromTokenForSession(httpRequest);
    if (userId != null) {
      payload.put("userId", userId);
    }

    return ResponseEntity.ok(payload);
  }

  @GetMapping("/me")
  @Operation(summary = "Récupère les informations de l'utilisateur connecté")
  public ResponseEntity<UtilisateurResponse> getCurrentUser(HttpServletRequest httpRequest) {
    // Essayer d'obtenir l'ID depuis Spring Security Context d'abord
    Long userId = getCurrentUserIdFromSecurityContext(httpRequest);

    // Récupérer les informations de l'utilisateur
    UtilisateurResponse utilisateur = utilisateurQueryService.findById(userId);

    return ResponseEntity.ok(utilisateur);
  }

  @PostMapping("/init-password")
  @Operation(
      summary = "Initialise le mot de passe lors de la première connexion (initPassword = false)")
  public ResponseEntity<Void> initPassword(@Valid @RequestBody InitPasswordRequest request,
      HttpServletRequest httpRequest) {
    // Essayer d'obtenir l'ID depuis Spring Security Context d'abord
    Long userId = getCurrentUserIdFromSecurityContext(httpRequest);

    // Initialiser le mot de passe (ne vérifie pas le mot de passe actuel, met initPassword à true)
    authService.initPassword(userId, request.getNewPassword());

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/change-password")
  @Operation(summary = "Change le mot de passe de l'utilisateur connecté depuis le profil")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest) {
    // Essayer d'obtenir l'ID depuis Spring Security Context d'abord
    Long userId = getCurrentUserIdFromSecurityContext(httpRequest);

    // Changer le mot de passe (vérifie strictement le mot de passe actuel, ne modifie pas
    // initPassword)
    authService.changePassword(userId, request);
    //

    return ResponseEntity.noContent().build();
  }

  /**
   * Rafraîchit les tokens JWT avec persistance de 48h.
   *
   * <p>
   * Facebook-Grade 10/10 : Le refresh token est lu depuis un cookie HttpOnly (sécurité renforcée).
   * Plus besoin d'envoyer le refresh token dans le body de la requête.
   *
   * <p>
   * STRATÉGIE : Persistance de 48h sans rotation du refresh token.
   *
   * <p>
   * Lors du refresh :
   * <ul>
   * <li>Un nouveau access token est généré (durée de vie : 48h)</li>
   * <li>Un nouveau refresh token est généré et stocké dans un cookie HttpOnly (durée de vie : 48h)</li>
   * <li>L'ancien refresh token reste valide jusqu'à son expiration naturelle (48h)</li>
   * <li>Le token n'est blacklisté que lors de la déconnexion explicite ou à l'expiration</li>
   * </ul>
   *
   * <p>
   * Avantages :
   * <ul>
   * <li>Persistance de 48h sans blacklistage prématuré</li>
   * <li>Les requêtes parallèles continuent de fonctionner avec l'ancien token</li>
   * <li>Expiration automatique après 48h (géré par Redis TTL)</li>
   * </ul>
   *
   * <p>
   * Compatibilité : Si un RefreshTokenRequest est fourni dans le body (ancien système), il sera
   * utilisé en fallback. Sinon, le refresh token est lu depuis le cookie HttpOnly.
   *
   * @param httpRequest la requête HTTP pour extraire le refresh token depuis le cookie
   * @param request la requête contenant le refresh token (optionnel, pour compatibilité)
   * @return AuthResponse contenant les nouveaux tokens
   */
  @PostMapping("/refresh")
  @Operation(summary = "Rafraîchit les tokens JWT avec persistance de 48h (cookie HttpOnly)")
  public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest httpRequest,
      @RequestBody(required = false) RefreshTokenRequest request) {
    log.debug("Tentative de rafraîchissement de token");

    String refreshToken = null;

    // ✅ Facebook-Grade 10/10 : Priorité 1 - Lire depuis le cookie HttpOnly (sécurité renforcée)
    jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
    if (cookies != null) {
      for (jakarta.servlet.http.Cookie cookie : cookies) {
        if (COOKIE_REFRESH_TOKEN.equals(cookie.getName())) {
          refreshToken = cookie.getValue();
          log.debug("Refresh token extrait depuis le cookie HttpOnly");
          break;
        }
      }
    }

    // Fallback : Si pas de cookie, essayer depuis le body (compatibilité avec ancien système)
    if (refreshToken == null && request != null && request.refreshToken() != null
        && !request.refreshToken().isBlank()) {
      refreshToken = request.refreshToken();
      log.debug("Refresh token lu depuis le body (mode compatibilité)");
    }

    // Vérifier que le refresh token est présent
    if (refreshToken == null || refreshToken.isBlank()) {
      log.warn("Refresh token manquant (ni dans le cookie, ni dans le body)");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Rafraîchir les tokens
    AuthResponse response = authService.refreshToken(refreshToken);

    // ✅ Déterminer si le refresh token original avait "Se souvenir de moi" activé
    // Si la durée de vie restante est supérieure à 60 jours, c'est qu'il a été créé avec rememberMe
    boolean rememberMe = false;
    if (jwtTokenProvider instanceof cd.shad.erp.cmk.cmkerp.gateway.security.JwtTokenProviderImpl providerImpl) {
      rememberMe = providerImpl.isRememberMeToken(refreshToken);
    }

    // ✅ Utiliser CookieFactory pour garantir Path="/" et cohérence
    // Préserver le paramètre rememberMe lors du rafraîchissement
    boolean secure = isCookieSecure();
    var accessCookie = CookieFactory.accessCookie(response.accessToken(), secure, rememberMe);
    var refreshCookie = CookieFactory.refreshCookie(response.refreshToken(), secure, rememberMe);

    log.info(
        "✅ Tokens rafraîchis avec succès, nouveaux cookies HttpOnly créés (rememberMe={}, secure={})",
        rememberMe, secure);

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(response);
  }

  /**
   * Déconnecte l'utilisateur en blacklistant le token actuel.
   *
   * <p>
   * Le token JWT est ajouté à la blacklist Redis et ne pourra plus être utilisé, même s'il n'est
   * pas encore expiré.
   *
   * @param httpRequest la requête HTTP pour extraire le token
   * @return ResponseEntity avec statut 204 (No Content)
   */
  @PostMapping("/logout")
  @Operation(summary = "Déconnecte l'utilisateur en invalidant le token JWT")
  public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
    boolean secure = isCookieSecure();
    String token = extractTokenFromRequest(httpRequest);
    if (token != null && jwtTokenProvider.validateToken(token)) {
      try {
        revokeTokenAndUserTokens(token);
      } catch (Exception e) {
        log.warn("Erreur lors de la révocation des tokens (non bloquant): {}", e.getMessage());
      }
    }

    log.debug("Cookies supprimés (access token et refresh token)");
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, CookieFactory.deleteAccess(secure).toString())
        .header(HttpHeaders.SET_COOKIE, CookieFactory.deleteRefresh(secure).toString()).build();
  }

  /**
   * Révoque le token d'accès et tous les refresh tokens de l'utilisateur.
   *
   * @param token le token JWT à révoquer
   */
  private void revokeTokenAndUserTokens(String token) {
    // Ajouter le token à la blacklist
    jwtBlacklistService.blacklistToken(token);

    // Récupérer userId pour révoquer aussi les refresh tokens
    Long userId = jwtTokenProvider.getUserIdFromToken(token);
    if (userId != null) {
      // Révoquer tous les refresh tokens de l'utilisateur
      jwtBlacklistService.revokeAllUserTokens(userId);
    }

    log.info("Utilisateur déconnecté, token blacklisté");
  }

  /**
   * *
   * <p>
   * Méthode optimisée qui essaie d'abord d'utiliser Spring Security Context, puis fallback sur
   * l'extraction manuelle du JWT si nécessaire.
   *
   * @param request la requête HTTP
   * @return l'ID de l'utilisateur connecté
   * @throws BusinessException si l'utilisateur n'est pas authentifié ou si le token est invalide
   */
  private Long getCurrentUserIdFromSecurityContext(HttpServletRequest request) {
    // Essayer d'obtenir l'utilisateur depuis Spring Security Context
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserDetails userDetails) {
      String username = userDetails.getUsername();

      // ✅ CRITICAL: Utiliser extractTokenFromRequest qui lit depuis Authorization header OU cookie
      String token = extractTokenFromRequest(request);
      if (token != null && jwtTokenProvider.validateToken(token)) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId != null) {
          log.debug("User ID extrait depuis JWT token (via Security Context) pour username: {}",
              username);
          return userId;
        }
      }
    }

    // Fallback : extraction manuelle depuis Authorization header OU cookie
    return getCurrentUserIdFromToken(request);
  }

  /**
   * Extrait l'ID de l'utilisateur connecté depuis le JWT token.
   *
   * <p>
   * Cette méthode est utilisée comme fallback si Spring Security Context n'est pas disponible. Elle
   * lit le token depuis le header Authorization OU depuis le cookie HttpOnly (priorité au header).
   *
   * @param request la requête HTTP
   * @return l'ID de l'utilisateur connecté
   * @throws BusinessException si l'utilisateur n'est pas authentifié ou si le token est invalide
   */
  private Long getCurrentUserIdFromToken(HttpServletRequest request) {
    // ✅ CRITICAL: Utiliser extractTokenFromRequest qui lit depuis Authorization header OU cookie
    String token = extractTokenFromRequest(request);

    if (token == null) {
      log.warn(
          "Tentative d'accès non authentifié à une ressource protégée - aucun token trouvé (ni dans Authorization header, ni dans cookie)");
      throw new BusinessException("Authentification requise. Veuillez vous connecter.");
    }

    if (!jwtTokenProvider.validateToken(token)) {
      log.warn("Tentative d'accès avec un token JWT invalide ou expiré");
      throw new BusinessException("Token JWT invalide ou expiré. Veuillez vous reconnecter.");
    }

    Long userId = jwtTokenProvider.getUserIdFromToken(token);
    if (userId == null) {
      log.warn("Impossible d'extraire l'ID utilisateur du token JWT");
      throw new BusinessException(
          "Impossible d'identifier l'utilisateur. Veuillez vous reconnecter.");
    }

    log.debug("User ID extrait depuis token JWT: {}", userId);
    return userId;
  }

  /**
   * Extrait le token JWT depuis la requête HTTP (header Authorization ou cookie).
   *
   * <p>
   * Priorité : Authorization header > Cookie HttpOnly.
   *
   * <p>
   * ✅ CRITICAL: Cette méthode est utilisée pour toutes les requêtes authentifiées. Elle permet de
   * lire le token depuis le cookie HttpOnly si le header Authorization n'est pas présent.
   *
   * @param httpRequest la requête HTTP
   * @return le token JWT ou null si non trouvé
   */
  private String extractTokenFromRequest(HttpServletRequest httpRequest) {
    // Priorité 1 : Authorization header (utilisé par le frontend qui met le token dans le header)
    String authHeader = httpRequest.getHeader(HEADER_AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      String token = authHeader.substring(BEARER_PREFIX.length());
      log.debug("Token extrait depuis Authorization header");
      return token;
    }

    // Priorité 2 : Cookie HttpOnly (utilisé si le frontend n'a pas mis le token dans le header)
    jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
    if (cookies != null) {
      for (jakarta.servlet.http.Cookie cookie : cookies) {
        if (COOKIE_AUTH_TOKEN.equals(cookie.getName())) {
          log.debug("Token extrait depuis cookie HttpOnly: {}", COOKIE_AUTH_TOKEN);
          return cookie.getValue();
        }
      }
      // Log pour debugging si les cookies sont présents mais pas le token
      if (log.isDebugEnabled()) {
        StringBuilder cookieNames = new StringBuilder();
        for (jakarta.servlet.http.Cookie cookie : cookies) {
          if (!cookieNames.isEmpty())
            cookieNames.append(", ");
          cookieNames.append(cookie.getName());
        }
        log.debug("Cookies présents mais {} non trouvé. Cookies disponibles: {}", COOKIE_AUTH_TOKEN,
            cookieNames);
      }
    } else {
      log.debug("Aucun cookie présent dans la requête");
    }

    log.debug("Aucun token trouvé (ni dans Authorization header, ni dans cookie)");
    return null;
  }

  /**
   * Extrait l'ID utilisateur depuis le token JWT pour l'endpoint /session.
   *
   * <p>
   * Gère les erreurs silencieusement en retournant null si l'extraction échoue.
   *
   * @param httpRequest la requête HTTP
   * @return l'ID utilisateur ou null si non trouvé ou invalide
   */
  private Long extractUserIdFromTokenForSession(HttpServletRequest httpRequest) {
    try {
      String token = extractTokenFromRequest(httpRequest);
      if (token != null && jwtTokenProvider.validateToken(token)) {
        return jwtTokenProvider.getUserIdFromToken(token);
      }
    } catch (Exception e) {
      log.debug("Impossible d'extraire userId depuis le token pour /session: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Détermine si les cookies doivent être sécurisés (Secure=true) selon l'environnement.
   *
   * <p>
   * Règle stricte : Secure=false en dev (HTTP), Secure=true en prod (HTTPS).
   *
   * @return true si en production, false en développement
   */
  private boolean isCookieSecure() {
    // ✅ strict: Secure=false en dev (HTTP), Secure=true en prod (HTTPS)
    // adjust if you already have a better env resolver.
    String profiles = System.getProperty("spring.profiles.active", activeProfile);
    boolean dev = profiles.contains("dev") || profiles.contains("local") || profiles.isBlank();
    return !dev;
  }
}

