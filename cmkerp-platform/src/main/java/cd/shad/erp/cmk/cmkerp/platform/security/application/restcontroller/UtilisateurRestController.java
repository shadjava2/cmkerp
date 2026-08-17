package cd.shad.erp.cmk.cmkerp.platform.security.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.USERS_BASE;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.ChangePasswordRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.UtilisateurRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UserPermissionsResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UtilisateurResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.UtilisateurCommandService;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.UtilisateurQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(USERS_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - Utilisateurs", description = "Gestion des utilisateurs, rôles, permissions")
@Validated
@Slf4j
public class UtilisateurRestController {

  private final UtilisateurQueryService utilisateurQueryService;
  private final UtilisateurCommandService utilisateurCommandService;
  private final JwtTokenProvider jwtTokenProvider;
  private final cd.shad.erp.cmk.cmkerp.platform.security.application.service.UserReportService userReportService;

  /**
   * Récupère une page d'utilisateurs.
   */
  @GetMapping
  @Operation(summary = "Liste paginée des utilisateurs")
  public ResponseEntity<Page<UtilisateurResponse>> findAll(
      Pageable pageable,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
      @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean locked) {
    Page<UtilisateurResponse> utilisateurs = utilisateurQueryService.findAll(pageable, search, locked);
    return ResponseEntity.ok(utilisateurs);
  }

  /**
   * Récupère un utilisateur par son ID.
   */
  @GetMapping("/{id}")
  @Operation(summary = "Récupère un utilisateur par son ID")
  public ResponseEntity<UtilisateurResponse> findById(@PathVariable Long id) {
    UtilisateurResponse utilisateur = utilisateurQueryService.findById(id);
    return ResponseEntity.ok(utilisateur);
  }

  /**
   * Récupère un utilisateur par son nom d'utilisateur.
   */
  @GetMapping("/by-username/{username}")
  @Operation(summary = "Récupère un utilisateur par son nom d'utilisateur")
  public ResponseEntity<UtilisateurResponse> findByUsername(@PathVariable String username) {
    UtilisateurResponse utilisateur = utilisateurQueryService.findByUsername(username);
    return ResponseEntity.ok(utilisateur);
  }

  /**
   * Crée un nouvel utilisateur.
   */
  @PostMapping
  @Operation(summary = "Crée un nouvel utilisateur")
  public ResponseEntity<UtilisateurResponse> create(@Valid @RequestBody UtilisateurRequest request,
      HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    UtilisateurResponse created = utilisateurCommandService.create(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Met à jour un utilisateur existant.
   */
  @PutMapping("/{id}")
  @Operation(summary = "Met à jour un utilisateur")
  public ResponseEntity<UtilisateurResponse> update(@PathVariable Long id,
      @Valid @RequestBody UtilisateurRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    UtilisateurResponse updated = utilisateurCommandService.update(id, request, currentUserId);
    return ResponseEntity.ok(updated);
  }

  /**
   * Change le mot de passe d'un utilisateur.
   */
  @PatchMapping("/{id}/password")
  @Operation(summary = "Change le mot de passe d'un utilisateur")
  public ResponseEntity<Void> changePassword(@PathVariable Long id,
      @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    utilisateurCommandService.changePassword(id, request, currentUserId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Réinitialise le mot de passe d'un utilisateur (admin uniquement).
   * Génère un nouveau mot de passe aléatoire sécurisé et définit initPassword à false.
   */
  @PostMapping("/{id}/reset-password")
  @Operation(summary = "Réinitialise le mot de passe d'un utilisateur (admin)")
  public ResponseEntity<Void> resetPassword(@PathVariable Long id, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    utilisateurCommandService.resetPassword(id, currentUserId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Supprime un utilisateur.
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Supprime un utilisateur")
  public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    utilisateurCommandService.delete(id, currentUserId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Récupère les permissions d'un utilisateur.
   */
  @GetMapping("/{id}/permissions")
  @Operation(summary = "Récupère les permissions d'un utilisateur")
  public ResponseEntity<UserPermissionsResponse> getPermissions(@PathVariable Long id) {
    UserPermissionsResponse permissions = utilisateurQueryService.getPermissions(id);
    return ResponseEntity.ok(permissions);
  }

  /**
   * Récupère les pharmacies auxquelles un utilisateur a accès.
   */
  @GetMapping("/{id}/pharmacies")
  @Operation(summary = "Récupère les pharmacies d'un utilisateur")
  public ResponseEntity<List<cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse>> getPharmacies(
      @PathVariable Long id) {
    List<cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse> pharmacies =
        utilisateurQueryService.getPharmacies(id);
    return ResponseEntity.ok(pharmacies);
  }

  /**
   * Génère un rapport PDF de la liste des utilisateurs (page actuelle uniquement).
   * 🎯 OPTIMISATION: Utilise les mêmes filtres que la liste pour garantir la cohérence.
   *
   * @param searchTerm terme de recherche (username, nom, postnom, prenom, specialite)
   * @param locked filtre sur le statut verrouillé (true = verrouillé, false = non verrouillé, null = tous)
   * @param page page actuelle (défaut: 0)
   * @param size taille de page actuelle (défaut: 20)
   * @return PDF en streaming avec Content-Disposition: inline
   */
  @GetMapping("/reports")
  @Operation(summary = "Génère un rapport PDF de la liste des utilisateurs (page actuelle uniquement)")
  public ResponseEntity<byte[]> generateUsersReport(
      @org.springframework.web.bind.annotation.RequestParam(required = false) String searchTerm,
      @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean locked,
      // 🎯 CRITICAL: required = true pour forcer la transmission des paramètres (évite les valeurs par défaut)
      @org.springframework.web.bind.annotation.RequestParam(required = true) Integer page,
      @org.springframework.web.bind.annotation.RequestParam(required = true) Integer size) {

    log.info("🚀 [UtilisateurRestController] Début génération rapport utilisateurs - searchTerm: {}, locked: {}",
            searchTerm, locked);

    try {
      // 🎯 OPTIMISATION: Récupérer uniquement les utilisateurs de la page actuelle (pas tous les utilisateurs)
      // Utiliser la pagination actuelle pour générer le rapport uniquement pour la page affichée
      Pageable pageable = PageRequest.of(page, size);
      log.info("📄 [UtilisateurRestController] Génération rapport pour page={}, size={}", page, size);

      // 🎯 OPTIMISATION: Utiliser PageResponse pour cohérence avec le rapport produits
      // Récupérer les utilisateurs avec les mêmes filtres que la liste affichée
      Page<UtilisateurResponse> pageResponse = utilisateurQueryService.findAll(pageable, searchTerm, locked);
      // Convertir Page en PageResponse pour cohérence avec le reste du système (comme le rapport produits)
      PageResponse<UtilisateurResponse> pageResponseDto = PageResponse.fromSpringPage(pageResponse);
      List<UtilisateurResponse> utilisateurs = pageResponseDto.getContent();

      log.info("✅ [UtilisateurRestController] {} utilisateurs récupérés (page {} sur {}), génération du PDF...",
              utilisateurs.size(), page, pageResponseDto.getTotalPages());

      log.info("✅ [UtilisateurRestController] {} utilisateurs récupérés, génération du PDF...", utilisateurs.size());

      // 🎯 OPTIMISATION: Utiliser le service de rapport dédié avec JasperReports
      byte[] pdfBytes = userReportService.generateUsersReport(utilisateurs);

      log.info("✅ [UtilisateurRestController] PDF généré: {} bytes", pdfBytes.length);

      // Préparer les headers pour l'affichage inline dans un iframe
      org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
      headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
      String filename = "rapport-utilisateurs-" +
              java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
      headers.setContentDispositionFormData("inline", filename);
      headers.setContentLength(pdfBytes.length);

      log.info("✅ [UtilisateurRestController] Rapport utilisateurs généré avec succès: {} utilisateurs, taille: {} bytes, filename: {}",
              utilisateurs.size(), pdfBytes.length, filename);

      return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);

    } catch (Exception e) {
      // Gérer les erreurs JRException et autres
      String errorType = e.getClass().getSimpleName();
      if (errorType.contains("JRException")) {
        log.error("Erreur JasperReports lors de la génération du rapport utilisateurs: {}", e.getMessage(), e);
        return buildErrorResponse("Erreur JasperReports: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
      } else if (e instanceof RuntimeException) {
        log.error("Erreur lors de la génération du rapport utilisateurs: {}", e.getMessage(), e);
        return buildErrorResponse("Erreur: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
      } else {
        log.error("Erreur inattendue lors de la génération du rapport utilisateurs: {}", e.getMessage(), e);
        return buildErrorResponse("Erreur inattendue: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  /**
   * Construit une réponse d'erreur en JSON.
   */
  private ResponseEntity<byte[]> buildErrorResponse(String message, org.springframework.http.HttpStatus status) {
    try {
      java.util.Map<String, String> error = new java.util.HashMap<>();
      error.put("error", message);
      error.put("status", status.toString());

      // 🎯 OPTIMISATION: Échapper correctement les caractères spéciaux pour un JSON valide
      String escapedMessage = message
              .replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
              .replace("\t", "\\t");
      String jsonError = "{\"error\":\"" + escapedMessage + "\",\"status\":\"" + status + "\",\"message\":\"" + escapedMessage + "\"}";

      org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
      headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

      return new ResponseEntity<>(jsonError.getBytes(), headers, status);
    } catch (Exception e) {
      log.error("Erreur lors de la construction de la réponse d'erreur", e);
      return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Extrait l'ID de l'utilisateur connecté depuis le JWT token dans l'en-tête Authorization.
   *
   * @param request la requête HTTP
   * @return l'ID de l'utilisateur connecté
   * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si le token est
   *         invalide
   */
  private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }
}





