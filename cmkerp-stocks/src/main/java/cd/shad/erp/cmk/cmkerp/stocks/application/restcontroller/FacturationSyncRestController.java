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

import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.FacturationSyncApplyRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FacturationSyncApplyResult;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FacturationSyncCompareResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.FacturationSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCKS_BASE + "/facturation-sync")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Stocks - Sync facturation Beach ↔ CH",
    description = "Comparer / aligner PAU et importer TPRODUIT+TSTOCK entre les 2 serveurs SQL Server")
public class FacturationSyncRestController {

  private final FacturationSyncService facturationSyncService;

  @GetMapping("/comparer")
  @Operation(summary = "Écarts de prix et produits manquants Beach ↔ CH")
  public ResponseEntity<FacturationSyncCompareResponse> comparer(
      @RequestParam(required = false) String q) {
    return ResponseEntity.ok(facturationSyncService.compare(q));
  }

  @PostMapping("/appliquer")
  @Operation(summary = "Synchroniser prix ou importer produits entre Beach et CH")
  public ResponseEntity<FacturationSyncApplyResult> appliquer(
      @Valid @RequestBody FacturationSyncApplyRequest request) {
    return ResponseEntity.ok(facturationSyncService.apply(request));
  }
}
