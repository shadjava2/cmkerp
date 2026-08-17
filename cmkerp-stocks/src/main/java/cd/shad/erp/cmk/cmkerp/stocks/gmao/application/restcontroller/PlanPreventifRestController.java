package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.GMAO_PLANS_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request.PlanPreventifRequest;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.InterventionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.PlanPreventifResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service.PlanPreventifService;
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
@RequestMapping(GMAO_PLANS_BASE)
@RequiredArgsConstructor
@Validated
@Tag(name = "GMAO — Plans préventifs", description = "Maintenance préventive planifiée")
public class PlanPreventifRestController {

  private final PlanPreventifService planPreventifService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping
  @Operation(summary = "Liste paginée des plans préventifs")
  public ResponseEntity<PageResponse<PlanPreventifResponse>> findAll(
      @RequestParam(required = false) Long fkEquipement,
      @RequestParam(required = false) Boolean actif,
      @RequestParam(required = false) Boolean enRetardOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(
        planPreventifService.findAll(fkEquipement, actif, enRetardOnly, page, size));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Détail d'un plan préventif")
  public ResponseEntity<PlanPreventifResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(planPreventifService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Créer un plan préventif")
  public ResponseEntity<PlanPreventifResponse> create(
      @Valid @RequestBody PlanPreventifRequest request, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(planPreventifService.create(request, userId));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Mettre à jour un plan préventif")
  public ResponseEntity<PlanPreventifResponse> update(@PathVariable Long id,
      @Valid @RequestBody PlanPreventifRequest request, HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(planPreventifService.update(id, request, userId));
  }

  @PostMapping("/{id}/generer-intervention")
  @Operation(summary = "Générer une OT préventive et avancer l'échéance")
  public ResponseEntity<InterventionResponse> genererIntervention(@PathVariable Long id,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(planPreventifService.genererIntervention(id, userId));
  }
}
