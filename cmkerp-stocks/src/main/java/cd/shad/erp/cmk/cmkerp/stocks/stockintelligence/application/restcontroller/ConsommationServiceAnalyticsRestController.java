package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.ConsommationServiceAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/pilotage/consommation-services")
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - Rapport consommation services")
public class ConsommationServiceAnalyticsRestController {

  private final Optional<ConsommationServiceAnalyticsService> service;

  @GetMapping("/kpis")
  @Operation(summary = "KPI consommation service (PAYEE / FACTUREE / SORTIE-USAGE)")
  public ResponseEntity<Map<String, Object>> kpis(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q) {
    return ResponseEntity.ok(require().kpis(pharmacieId, from, to, q));
  }

  @GetMapping("/stats/mensuel")
  @Operation(summary = "Résumé mensuel des 3 types de sorties")
  public ResponseEntity<List<Map<String, Object>>> statsMensuel(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q) {
    return ResponseEntity.ok(require().statsMensuel(pharmacieId, from, to, q));
  }

  @GetMapping("/stats/produits")
  @Operation(summary = "Stats produits consommés sur la période")
  public ResponseEntity<List<Map<String, Object>>> statsProduits(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "200") int limit) {
    return ResponseEntity.ok(require().statsProduits(pharmacieId, from, to, q, limit));
  }

  @GetMapping("/stats/produits-mensuel")
  @Operation(summary = "Tableau croisé produits × mois (quantités sorties)")
  public ResponseEntity<Map<String, Object>> statsProduitsMensuel(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "200") int limit) {
    return ResponseEntity.ok(require().statsProduitsMensuel(pharmacieId, from, to, q, limit));
  }

  @GetMapping("/details")
  @Operation(summary = "Détail des lignes de consommation")
  public ResponseEntity<List<Map<String, Object>>> details(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String usageType,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "500") int limit) {
    return ResponseEntity.ok(require().details(pharmacieId, from, to, usageType, q, limit));
  }

  @GetMapping("/export/excel")
  @Operation(summary = "Export Excel consommation service (synthèse, mensuel, produits, détail)")
  public ResponseEntity<byte[]> exportExcel(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String usageType,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String pharmacieLabel) {
    ConsommationServiceAnalyticsService svc = require();
    byte[] bytes = svc.exportExcel(pharmacieId, from, to, usageType, q, pharmacieLabel);
    String filename = svc.exportFilename();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .contentLength(bytes.length)
        .body(bytes);
  }

  private ConsommationServiceAnalyticsService require() {
    return service.orElseThrow(
        () ->
            new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Module stock-intelligence désactivé"));
  }
}
