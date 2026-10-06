package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import java.util.List;
import java.util.Map;

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

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.BatchLinkCodeCliniqueRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.LinkCodeCliniqueRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.BatchLinkResultResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FusionStatsResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FusionSuggestionResponse;
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

  @GetMapping("/stats")
  @Operation(summary = "Compteurs liés / non liés ERP + CLINIQUE")
  public ResponseEntity<FusionStatsResponse> stats(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "true") Boolean actif) {
    return ResponseEntity.ok(produitFusionService.stats(pharmacieId, actif));
  }

  @GetMapping("/cmkerp")
  @Operation(summary = "Liste produits CMKERP pour fusion (stock d'une pharmacie)")
  public ResponseEntity<Map<String, Object>> listCmkerp(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "unlinked") String linkFilter,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "true") Boolean actif) {
    return ResponseEntity.ok(
        produitFusionService.listCmkerp(q, linkFilter, page, size, pharmacieId, actif));
  }

  @GetMapping("/clinique")
  @Operation(summary = "Liste produits CLINIQUE (TPRODUIT) lecture seule")
  public ResponseEntity<Map<String, Object>> listClinique(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "unlinked") String linkFilter,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return ResponseEntity.ok(produitFusionService.listClinique(q, linkFilter, page, size));
  }

  @GetMapping("/suggestions")
  @Operation(summary = "Propositions automatiques de liaison par similarité de désignation")
  public ResponseEntity<List<FusionSuggestionResponse>> suggestions(
      @RequestParam(required = false, defaultValue = "0.78") double minScore,
      @RequestParam(required = false, defaultValue = "100") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "true") Boolean actif) {
    return ResponseEntity.ok(produitFusionService.suggest(minScore, limit, pharmacieId, actif));
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

  @PostMapping("/batch-link")
  @Operation(summary = "Valider plusieurs liaisons CODECLINIQUE en une fois")
  public ResponseEntity<BatchLinkResultResponse> batchLink(
      @Valid @RequestBody BatchLinkCodeCliniqueRequest request,
      HttpServletRequest httpRequest) {
    Long userId = AuthTokenExtractor.getCurrentUserId(httpRequest, jwtTokenProvider);
    return ResponseEntity.ok(produitFusionService.batchLink(request, userId));
  }
}
