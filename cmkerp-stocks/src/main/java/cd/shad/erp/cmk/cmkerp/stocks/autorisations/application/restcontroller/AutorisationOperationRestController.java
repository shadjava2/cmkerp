package cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.AUTORISATIONS_OPERATIONS_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.request.DecisionAutorisationRequest;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.response.AutorisationOperationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.service.AutorisationOperationCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.service.AutorisationOperationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AUTORISATIONS_OPERATIONS_BASE)
@RequiredArgsConstructor
@Tag(name = "Autorisations opérations", description = "Demandes d'autorisation centralisées")
@Validated
public class AutorisationOperationRestController {

  private final AutorisationOperationQueryService queryService;
  private final AutorisationOperationCommandService commandService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping
  @Operation(summary = "Liste paginée des demandes d'autorisation")
  public ResponseEntity<PageResponse<AutorisationOperationResponse>> findAll(
      Pageable pageable,
      @RequestParam(required = false) String statut) {
    return ResponseEntity.ok(queryService.findAll(pageable, statut));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Détail d'une demande d'autorisation")
  public ResponseEntity<AutorisationOperationResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(queryService.findById(id));
  }

  @PostMapping("/{id}/approuver")
  @Operation(summary = "Approuver une demande (admin)")
  public ResponseEntity<AutorisationOperationResponse> approuver(
      @PathVariable Long id,
      @RequestBody(required = false) DecisionAutorisationRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(commandService.approuver(id, request, userId));
  }

  @PostMapping("/{id}/rejeter")
  @Operation(summary = "Rejeter une demande (admin)")
  public ResponseEntity<AutorisationOperationResponse> rejeter(
      @PathVariable Long id,
      @RequestBody(required = false) DecisionAutorisationRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(commandService.rejeter(id, request, userId));
  }
}
