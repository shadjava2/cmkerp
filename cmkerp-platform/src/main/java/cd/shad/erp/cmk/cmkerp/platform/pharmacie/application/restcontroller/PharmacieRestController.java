package cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.PHARMACIES_BASE;

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
import cd.shad.erp.cmk.cmkerp.platform.dto.request.PharmacieRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.service.PharmacieQueryService;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.application.service.PharmacieCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour la gestion des pharmacies.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(PHARMACIES_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - Pharmacies", description = "Gestion pharmacie, droits, structures liées")
@Validated
public class PharmacieRestController {

  private final PharmacieQueryService pharmacieQueryService;
  private final PharmacieCommandService pharmacieCommandService;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * Récupère toutes les pharmacies.
   */
  @GetMapping
  @Operation(summary = "Liste toutes les pharmacies")
  public ResponseEntity<List<PharmacieResponse>> findAll() {
    List<PharmacieResponse> pharmacies = pharmacieQueryService.findAll();
    return ResponseEntity.ok(pharmacies);
  }

  /**
   * Récupère une pharmacie par son ID.
   */
  @GetMapping("/{id}")
  @Operation(summary = "Récupère une pharmacie par son ID")
  public ResponseEntity<PharmacieResponse> findById(@PathVariable Long id) {
    PharmacieResponse pharmacie = pharmacieQueryService.findById(id);
    return ResponseEntity.ok(pharmacie);
  }

  /**
   * Récupère les pharmacies auxquelles un utilisateur a accès.
   * Note: Cet endpoint est également disponible via /api/v1/users/{id}/pharmacies
   */
  @GetMapping("/utilisateurs/{utilisateurId}")
  @Operation(summary = "Récupère les pharmacies d'un utilisateur")
  public ResponseEntity<List<PharmacieResponse>> findByUtilisateur(
      @PathVariable Long utilisateurId) {
    List<PharmacieResponse> pharmacies = pharmacieQueryService.findByUtilisateur(utilisateurId);
    return ResponseEntity.ok(pharmacies);
  }

  /**
   * Crée une nouvelle pharmacie.
   */
  @PostMapping
  @Operation(summary = "Crée une nouvelle pharmacie")
  public ResponseEntity<PharmacieResponse> create(@Valid @RequestBody PharmacieRequest request,
      HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    PharmacieResponse created = pharmacieCommandService.create(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Met à jour une pharmacie existante.
   */
  @PutMapping("/{id}")
  @Operation(summary = "Met à jour une pharmacie")
  public ResponseEntity<PharmacieResponse> update(@PathVariable Long id,
      @Valid @RequestBody PharmacieRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    PharmacieResponse updated = pharmacieCommandService.update(id, request, currentUserId);
    return ResponseEntity.ok(updated);
  }

  /**
   * Supprime une pharmacie.
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Supprime une pharmacie")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    pharmacieCommandService.deleteById(id);
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





