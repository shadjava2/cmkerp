package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.GMAO_INTERVENTIONS_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.ClotureInterventionRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.InterventionRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InterventionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service.InterventionService;
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

@RestController
@RequestMapping(GMAO_INTERVENTIONS_BASE)
@RequiredArgsConstructor
@Validated
@Tag(name = "GMAO — Interventions", description = "Ordres de travail / interventions")
public class InterventionRestController {

  private final InterventionService interventionService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping
  @Operation(summary = "Liste paginée des interventions")
  public ResponseEntity<PageResponse<InterventionResponse>> findAll(
      @RequestParam(required = false) Long fkPharmacie,
      @RequestParam(required = false) Long fkEquipement,
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(interventionService.findAll(fkPharmacie, fkEquipement, statut, type,
        search, page, size));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Détail d'une intervention")
  public ResponseEntity<InterventionResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(interventionService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Créer une intervention")
  public ResponseEntity<InterventionResponse> create(
      @Valid @RequestBody InterventionRequest request, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(interventionService.create(request, userId));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Mettre à jour une intervention ouverte")
  public ResponseEntity<InterventionResponse> update(@PathVariable Long id,
      @Valid @RequestBody InterventionRequest request, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(interventionService.update(id, request, userId));
  }

  @PostMapping("/{id}/planifier")
  @Operation(summary = "Planifier une intervention")
  public ResponseEntity<InterventionResponse> planifier(@PathVariable Long id,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(interventionService.planifier(id, userId));
  }

  @PostMapping("/{id}/demarrer")
  @Operation(summary = "Démarrer une intervention")
  public ResponseEntity<InterventionResponse> demarrer(@PathVariable Long id,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(interventionService.demarrer(id, userId));
  }

  @PostMapping("/{id}/cloturer")
  @Operation(summary = "Clôturer une intervention")
  public ResponseEntity<InterventionResponse> cloturer(@PathVariable Long id,
      @RequestBody(required = false) ClotureInterventionRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(interventionService.cloturer(id, request, userId));
  }

  @PostMapping("/{id}/annuler")
  @Operation(summary = "Annuler une intervention")
  public ResponseEntity<InterventionResponse> annuler(@PathVariable Long id,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(interventionService.annuler(id, userId));
  }
}
