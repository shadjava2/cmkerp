package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.GMAO_INVENTAIRES_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.InventaireCampagneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.InventaireLigneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InventaireCampagneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InventaireLigneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service.GmaoInventaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventaire physique GMAO (équipements).
 * Nom distinct de {@code stocks.inventaires...InventaireRestController} (inventaire stock).
 */
@RestController
@RequestMapping(GMAO_INVENTAIRES_BASE)
@RequiredArgsConstructor
@Validated
@Tag(name = "GMAO — Inventaire physique",
    description = "Campagnes de vérification de disponibilité (distinct de l'identification)")
public class GmaoInventaireRestController {

  private final GmaoInventaireService inventaireService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping
  @Operation(summary = "Lister les campagnes d'inventaire")
  public ResponseEntity<PageResponse<InventaireCampagneResponse>> list(
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(inventaireService.findCampagnes(statut, search, page, size));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Détail d'une campagne")
  public ResponseEntity<InventaireCampagneResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(inventaireService.findCampagne(id));
  }

  @PostMapping
  @Operation(summary = "Créer une campagne d'inventaire")
  public ResponseEntity<InventaireCampagneResponse> create(
      @Valid @RequestBody InventaireCampagneRequest request, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(inventaireService.createCampagne(request, userId));
  }

  @PostMapping("/{id}/demarrer")
  @Operation(summary = "Démarrer la campagne et générer les lignes si besoin")
  public ResponseEntity<InventaireCampagneResponse> demarrer(@PathVariable Long id,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(inventaireService.demarrer(id, userId));
  }

  @PostMapping("/{id}/generer-lignes")
  @Operation(summary = "Générer les lignes depuis le parc (périmètre)")
  public ResponseEntity<InventaireCampagneResponse> genererLignes(@PathVariable Long id,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(inventaireService.genererLignes(id, userId));
  }

  @PostMapping("/{id}/cloturer")
  @Operation(summary = "Clôturer la campagne et propager les constats")
  public ResponseEntity<InventaireCampagneResponse> cloturer(@PathVariable Long id,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(inventaireService.cloturer(id, userId));
  }

  @GetMapping("/{id}/lignes")
  @Operation(summary = "Lignes de contrôle de la campagne")
  public ResponseEntity<PageResponse<InventaireLigneResponse>> lignes(
      @PathVariable Long id,
      @RequestParam(required = false) String resultat,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(inventaireService.findLignes(id, resultat, search, page, size));
  }

  @PutMapping("/lignes/{ligneId}")
  @Operation(summary = "Enregistrer le contrôle d'une ligne d'inventaire")
  public ResponseEntity<InventaireLigneResponse> controler(
      @PathVariable Long ligneId,
      @Valid @RequestBody InventaireLigneRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(inventaireService.controlerLigne(ligneId, request, userId));
  }
}
