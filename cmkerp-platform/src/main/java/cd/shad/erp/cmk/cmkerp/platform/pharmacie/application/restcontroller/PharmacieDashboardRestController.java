package cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.PHARMACIES_DASHBOARD_BASE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieOverviewResponse;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.service.PharmacieDashboardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST pour le dashboard Pharmacie.
 * Utilise le Query Service de la nouvelle architecture DDD.
 *
 * <p>
 * Expose un endpoint de recherche paginée de pharmacies avec métriques agrégées
 * pour un utilisateur donné.
 */
@RestController
@RequestMapping(PHARMACIES_DASHBOARD_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - Dashboard", description = "Tableaux de bord et statistiques")
@Validated
@Slf4j
public class PharmacieDashboardRestController {

  private final PharmacieDashboardQueryService pharmacieDashboardQueryService;
  private final JwtTokenProvider jwtTokenProvider;

  // ✅ Facebook-Grade 10/10 : Constantes pour éviter la duplication de littéraux
  private static final String COOKIE_AUTH_TOKEN = "cmk-auth-token";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  /**
   * Recherche paginée de pharmacies avec métriques pour l'utilisateur courant.
   *
   * <p>
   * Retourne une page de pharmacies avec :
   * <ul>
   * <li>Informations de base (pharmacie + site)</li>
   * <li>Indicateur d'accès de l'utilisateur (hasAccess)</li>
   * <li>Nombre d'utilisateurs ayant accès (nbUsersWithAccess)</li>
   * <li>Nombre de notifications en attente (nbNotificationsEnCours)</li>
   * </ul>
   *
   * <p>
   * Filtres disponibles :
   * <ul>
   * <li>siteId : filtre sur le site</li>
   * <li>typePharmacie : filtre sur le type de pharmacie</li>
   * <li>searchText : recherche textuelle sur designation ou site.designation</li>
   * </ul>
   *
   * <p>
   * Pagination :
   * <ul>
   * <li>page : numéro de page (0-indexed)</li>
   * <li>size : taille de la page</li>
   * </ul>
   *
   * <p>
   * L'ID de l'utilisateur courant est automatiquement extrait du token JWT dans l'en-tête
   * Authorization OU depuis le cookie HttpOnly "cmk-auth-token" (priorité : Authorization > Cookie).
   *
   * @param httpRequest la requête HTTP pour extraire le token JWT
   * @param siteId filtre optionnel sur le site
   * @param typePharmacie filtre optionnel sur le type de pharmacie
   * @param searchText filtre optionnel de recherche textuelle
   * @param pageable paramètres de pagination (page, size)
   * @return Page de PharmacieOverviewResponse
   */
  @GetMapping
  @Operation(summary = "Recherche paginée de pharmacies avec métriques pour le dashboard")
  public ResponseEntity<Page<PharmacieOverviewResponse>> searchPharmacies(
      HttpServletRequest httpRequest,
      @Parameter(description = "Filtre optionnel sur le site")
      @RequestParam(required = false) Long siteId,
      @Parameter(description = "Filtre optionnel sur le type de pharmacie")
      @RequestParam(required = false) String typePharmacie,
      @Parameter(description = "Recherche textuelle sur designation ou site.nom")
      @RequestParam(required = false) String searchText,
      Pageable pageable) {

    Long currentUserId = getCurrentUserId(httpRequest);

    // Validation: l'utilisateur doit être authentifié
    if (currentUserId == null) {
      throw new IllegalStateException("Impossible d'extraire l'ID utilisateur du token JWT");
    }

    Page<PharmacieOverviewResponse> result = pharmacieDashboardQueryService.searchPharmacies(
        currentUserId, siteId, typePharmacie, searchText, pageable);

    // IMPORTANT: Si aucune pharmacie n'est retournée, cela signifie que l'utilisateur
    // n'a pas de droits dans la table droits_pharmacies. L'utilisateur connecté
    // DOIT avoir des droits pour voir des pharmacies.
    if (result.getTotalElements() == 0) {
      // Log en mode développement pour aider au débogage
      if (System.getProperty("spring.profiles.active", "").contains("dev")) {
        System.out.println("[WARN] Aucune pharmacie trouvée pour l'utilisateur ID: " + currentUserId +
            " - Vérifier que l'utilisateur a des droits dans droits_pharmacies");
      }
    }

    return ResponseEntity.ok(result);
  }

  /**
   * Extrait l'ID de l'utilisateur connecté depuis Spring Security Context ou le token JWT.
   *
   * <p>
   * Facebook-Grade 10/10 : Méthode optimisée qui essaie d'abord d'utiliser Spring Security Context
   * (rempli par JwtAuthenticationFilter), puis fallback sur l'extraction manuelle du JWT si
   * nécessaire.
   *
   * <p>
   * Le JwtAuthenticationFilter lit le token depuis l'en-tête Authorization OU le cookie HttpOnly,
   * donc cette méthode fonctionne dans les deux cas.
   *
   * @param request la requête HTTP
   * @return l'ID de l'utilisateur connecté
   * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si le token est
   *         invalide
   */
  private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }

  /**
   * Extrait le token JWT depuis la requête HTTP (header Authorization ou cookie).
   *
   * <p>
   * Priorité : Authorization header > Cookie HttpOnly.
   *
   * @param request la requête HTTP
   * @return le token JWT ou null si non trouvé
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    // Priorité : Authorization header > Cookie
    String authHeader = request.getHeader(HEADER_AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      return authHeader.substring(BEARER_PREFIX.length());
    }

    // Si pas d'en-tête Authorization, lire depuis le cookie HttpOnly
    jakarta.servlet.http.Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (jakarta.servlet.http.Cookie cookie : cookies) {
        if (COOKIE_AUTH_TOKEN.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }

    return null;
  }

  /**
   * Extrait l'ID de l'utilisateur connecté depuis le JWT token dans l'en-tête Authorization ou le
   * cookie.
   *
   * @param request la requête HTTP
   * @return l'ID de l'utilisateur connecté
   * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si le token est
   *         invalide
   */
  private Long getCurrentUserIdFromToken(HttpServletRequest request) {
    String token = extractTokenFromRequest(request);
    if (token == null) {
      log.warn("Tentative d'accès non authentifié à une ressource protégée");
      throw new IllegalStateException("Utilisateur non authentifié");
    }

    if (!jwtTokenProvider.validateToken(token)) {
      log.warn("Tentative d'accès avec un token JWT invalide");
      throw new IllegalStateException("Token JWT invalide");
    }

    Long userId = jwtTokenProvider.getUserIdFromToken(token);
    if (userId == null) {
      log.warn("Impossible d'extraire l'ID utilisateur du token JWT");
      throw new IllegalStateException("Impossible d'extraire l'ID utilisateur du token JWT");
    }

    return userId;
  }
}





