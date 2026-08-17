package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.APPROVISIONNEMENTS_BASE;
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
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.LigneApprovRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.LigneApprovCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.LigneApprovQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST pour la gestion des lignes d'approvisionnement.
 */
@RestController
@RequestMapping(APPROVISIONNEMENTS_BASE + "/{approvId}/lignes")
@RequiredArgsConstructor
@Tag(name = "Approvisionnements - Lignes", description = "Gestion des lignes d'approvisionnement")
@Validated
@Slf4j
public class LigneApprovRestController {

  private final LigneApprovQueryService ligneApprovQueryService;
  private final LigneApprovCommandService ligneApprovCommandService;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * Récupère le userId depuis le JWT token.
   */
  private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }

  /**
   * Récupère toutes les lignes d'un approvisionnement.
   */
  @GetMapping
  @Operation(summary = "Récupère toutes les lignes d'un approvisionnement")
  public ResponseEntity<List<LigneApprovResponse>> findAll(@PathVariable Long approvId) {
    List<LigneApprovResponse> lignes = ligneApprovQueryService.findByFkApprov(approvId);
    return ResponseEntity.ok(lignes);
  }

  /**
   * Récupère une ligne par son ID.
   */
  @GetMapping("/{id}")
  @Operation(summary = "Récupère une ligne par son ID")
  public ResponseEntity<LigneApprovResponse> findById(@PathVariable Long id) {
    LigneApprovResponse ligne = ligneApprovQueryService.findById(id);
    return ResponseEntity.ok(ligne);
  }

  /**
   * Crée une nouvelle ligne d'approvisionnement.
   */
  @PostMapping
  @Operation(summary = "Crée une nouvelle ligne d'approvisionnement")
  public ResponseEntity<LigneApprovResponse> create(@PathVariable Long approvId,
      @Valid @RequestBody LigneApprovRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    // S'assurer que fkApprov correspond au path variable
    request.setFkApprov(approvId);
    LigneApprovResponse created = ligneApprovCommandService.create(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Met à jour une ligne d'approvisionnement existante.
   */
  @PutMapping("/{id}")
  @Operation(summary = "Met à jour une ligne d'approvisionnement")
  public ResponseEntity<LigneApprovResponse> update(@PathVariable Long approvId,
      @PathVariable Long id, @Valid @RequestBody LigneApprovRequest request,
      HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    // S'assurer que fkApprov correspond au path variable
    request.setFkApprov(approvId);
    LigneApprovResponse updated = ligneApprovCommandService.update(id, request, currentUserId);
    return ResponseEntity.ok(updated);
  }

  /**
   * Supprime une ligne d'approvisionnement.
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Supprime une ligne d'approvisionnement")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    ligneApprovCommandService.delete(id);
    return ResponseEntity.noContent().build();
  }
}





