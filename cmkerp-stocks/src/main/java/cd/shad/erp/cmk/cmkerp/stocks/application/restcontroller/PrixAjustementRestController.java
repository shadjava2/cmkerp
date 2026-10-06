package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.PrixAjustementApplyRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.PrixAjustementApplyResult;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.PrixEcartPageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.PrixAjustementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCKS_BASE + "/prix-ajustement")
@RequiredArgsConstructor
@Validated
@Tag(name = "Stocks - Ajustement prix CLINIQUE",
    description = "Écarts prixachat ERP ↔ PAU TSTOCK (pharmacie) et alignement")
public class PrixAjustementRestController {

  private final PrixAjustementService prixAjustementService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping("/ecarts")
  @Operation(summary = "Produits liés dont le prix ERP diffère du PAU CLINIQUE")
  public ResponseEntity<PrixEcartPageResponse> ecarts(@RequestParam(required = false) String q) {
    return ResponseEntity.ok(prixAjustementService.listEcarts(q));
  }

  @PostMapping("/appliquer")
  @Operation(summary = "Copier prix ERP→PAU CLINIQUE ou PAU CLINIQUE→prix ERP")
  public ResponseEntity<PrixAjustementApplyResult> appliquer(
      @Valid @RequestBody PrixAjustementApplyRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(prixAjustementService.apply(request, userId));
  }
}
