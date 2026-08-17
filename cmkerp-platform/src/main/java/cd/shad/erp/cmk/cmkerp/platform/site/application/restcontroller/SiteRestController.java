package cd.shad.erp.cmk.cmkerp.platform.site.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.SITES_BASE;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.SiteRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.SiteResponse;
import cd.shad.erp.cmk.cmkerp.platform.site.application.service.SiteQueryService;
import cd.shad.erp.cmk.cmkerp.platform.site.application.service.SiteCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour la gestion des sites.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(SITES_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - Sites", description = "Gestion des sites et structures organisationnelles")
@Validated
public class SiteRestController {

  private final SiteQueryService siteQueryService;
  private final SiteCommandService siteCommandService;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * Récupère tous les sites.
   */
  @GetMapping
  @Operation(summary = "Liste tous les sites")
  public ResponseEntity<List<SiteResponse>> findAll() {
    List<SiteResponse> sites = siteQueryService.findAll();
    return ResponseEntity.ok(sites);
  }

  /**
   * Récupère un site par son ID.
   */
  @GetMapping("/{id}")
  @Operation(summary = "Récupère un site par son ID")
  public ResponseEntity<SiteResponse> findById(@PathVariable Long id) {
    SiteResponse site = siteQueryService.findById(id);
    return ResponseEntity.ok(site);
  }

  /**
   * Crée un nouveau site.
   */
  @PostMapping
  @Operation(summary = "Crée un nouveau site")
  public ResponseEntity<SiteResponse> create(@Valid @RequestBody SiteRequest request,
      HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    SiteResponse created = siteCommandService.create(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Met à jour un site existant.
   */
  @PutMapping("/{id}")
  @Operation(summary = "Met à jour un site")
  public ResponseEntity<SiteResponse> update(@PathVariable Long id,
      @Valid @RequestBody SiteRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    SiteResponse updated = siteCommandService.update(id, request, currentUserId);
    return ResponseEntity.ok(updated);
  }

  /**
   * Supprime un site.
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Supprime un site")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    siteCommandService.deleteById(id);
    return ResponseEntity.noContent().build();
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





