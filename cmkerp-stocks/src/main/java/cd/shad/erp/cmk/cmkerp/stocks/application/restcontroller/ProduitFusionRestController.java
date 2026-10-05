package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.LinkCodeCliniqueRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitFusionItemResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ProduitFusionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCKS_BASE + "/produit-fusion")
@RequiredArgsConstructor
@Validated
@Tag(name = "Stocks - Fusion produits CLINIQUE",
    description = "Lier produits CMKERP ↔ TPRODUIT via CODECLINIQUE (écriture ERP uniquement)")
public class ProduitFusionRestController {

  private final ProduitFusionService produitFusionService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping("/cmkerp")
  @Operation(summary = "Liste produits CMKERP pour fusion")
  public ResponseEntity<Map<String, Object>> listCmkerp(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String linkFilter,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return ResponseEntity.ok(produitFusionService.listCmkerp(q, linkFilter, page, size));
  }

  @GetMapping("/clinique")
  @Operation(summary = "Liste produits CLINIQUE (TPRODUIT) lecture seule")
  public ResponseEntity<Map<String, Object>> listClinique(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return ResponseEntity.ok(produitFusionService.listClinique(q, page, size));
  }

  @PutMapping("/cmkerp/{produitId}/code-clinique")
  @Operation(summary = "Attribuer CODECLINIQUE à un produit CMKERP")
  public ResponseEntity<ProduitFusionItemResponse> link(
      @PathVariable Long produitId,
      @Valid @RequestBody LinkCodeCliniqueRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(produitFusionService.link(produitId, request.getCodeClinique(), userId));
  }
}
