package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.WorkspaceModuleAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/pilotage/modules")
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - Modules pilotage")
public class WorkspaceModuleAnalyticsRestController {

  private final WorkspaceModuleAnalyticsService service;

  @GetMapping("/{module}/kpis")
  @Operation(summary = "KPI synthèse par module pilotage")
  public ResponseEntity<Map<String, Object>> kpis(
      @PathVariable String module,
      @RequestParam(defaultValue = "CENTRALE") String scope,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(service.kpis(module, scope, pharmacieId, from, to));
  }

  @GetMapping("/{module}/liste")
  public ResponseEntity<List<Map<String, Object>>> liste(
      @PathVariable String module,
      @RequestParam(defaultValue = "CENTRALE") String scope,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "100") int limit) {
    return ResponseEntity.ok(service.liste(module, scope, pharmacieId, from, to, limit));
  }
}
