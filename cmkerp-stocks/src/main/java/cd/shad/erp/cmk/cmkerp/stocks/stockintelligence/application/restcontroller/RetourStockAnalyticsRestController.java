package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;
import static cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.RetourStockAnalyticsService.fromParams;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.RetourStockAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/pilotage/retours-stock-analytics")
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - Pilotage retours stock (transferts internes)")
public class RetourStockAnalyticsRestController {

  private final Optional<RetourStockAnalyticsService> service;

  @GetMapping("/kpis")
  @Operation(summary = "Indicateurs synthèse retours stock vers dépôt central")
  public ResponseEntity<RetourStockKpiDTO> kpis(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().kpis(criteria(all)));
  }

  @GetMapping("/liste")
  public ResponseEntity<List<RetourStockListItemDTO>> liste(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().list(criteria(all)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<RetourStockDetailDTO> detail(@PathVariable long id) {
    return ResponseEntity.ok(require().detail(id));
  }

  @GetMapping("/analyse/pharmacies-source")
  public ResponseEntity<List<RetourStockGroupStatDTO>> pharmaciesSource(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byPharmacieSource(criteria(all)));
  }

  @GetMapping("/analyse/pharmacies-destination")
  public ResponseEntity<List<RetourStockGroupStatDTO>> pharmaciesDestination(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byPharmacieDestination(criteria(all)));
  }

  @GetMapping("/analyse/statuts")
  public ResponseEntity<List<RetourStockGroupStatDTO>> statuts(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byStatut(criteria(all)));
  }

  @GetMapping("/analyse/statuts-reception")
  public ResponseEntity<List<RetourStockGroupStatDTO>> statutsReception(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byStatutReception(criteria(all)));
  }

  @GetMapping("/analyse/perime")
  public ResponseEntity<List<RetourStockGroupStatDTO>> perime(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byPerime(criteria(all)));
  }

  @GetMapping("/analyse/utilisateurs")
  public ResponseEntity<List<RetourStockGroupStatDTO>> utilisateurs(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byUtilisateur(criteria(all)));
  }

  @GetMapping("/analyse/top-produits")
  public ResponseEntity<List<RetourStockGroupStatDTO>> topProduits(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().topProduits(criteria(all), false));
  }

  @GetMapping("/analyse/produits-rares")
  public ResponseEntity<List<RetourStockGroupStatDTO>> produitsRares(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().topProduits(criteria(all), true));
  }

  @GetMapping("/analyse/periode/mensuelle")
  public ResponseEntity<List<RetourStockPeriodStatDTO>> mensuelle(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().mensuelle(criteria(all)));
  }

  @GetMapping("/analyse/periode/annuelle")
  public ResponseEntity<List<RetourStockPeriodStatDTO>> annuelle(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().annuelle(criteria(all)));
  }

  @GetMapping("/analyse/produits/{produitId}/historique")
  public ResponseEntity<List<RetourStockProduitHistoryDTO>> historiqueProduit(
      @PathVariable long produitId,
      @RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().historiqueProduit(produitId, criteria(all)));
  }

  @GetMapping("/anomalies")
  public ResponseEntity<List<RetourStockAnomalyDTO>> anomalies(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().anomalies(criteria(all)));
  }

  @GetMapping("/lookup/pharmacies-source")
  public ResponseEntity<List<Map<String, Object>>> lookupPharmaciesSource(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(require().lookupPharmaciesSource(q, limit, pharmacieId, scope));
  }

  @GetMapping("/lookup/pharmacies-destination")
  public ResponseEntity<List<Map<String, Object>>> lookupPharmaciesDestination(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(require().lookupPharmaciesDestination(q, limit, pharmacieId, scope));
  }

  @GetMapping("/lookup/utilisateurs")
  public ResponseEntity<List<Map<String, Object>>> lookupUtilisateurs(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(require().lookupUtilisateurs(q, limit, pharmacieId, scope));
  }

  @GetMapping("/lookup/produits")
  public ResponseEntity<List<Map<String, Object>>> lookupProduits(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(require().lookupProduits(q, limit, pharmacieId, scope));
  }

  private RetourStockAnalyticsService require() {
    return service.orElseThrow(() -> new IllegalStateException("Module retours stock analytics indisponible"));
  }

  private static cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.RetourStockSearchCriteria criteria(
      Map<String, String> p) {
    return fromParams(
        parseDate(p.get("dateDebut")),
        parseDate(p.get("dateFin")),
        parseLong(p.get("pharmacieSourceId")),
        parseLong(p.get("pharmacieDestinationId") != null ? p.get("pharmacieDestinationId") : p.get("pharmacieId")),
        parseLong(p.get("utilisateurId")),
        parseLong(p.get("produitId")),
        p.get("statut"),
        p.get("statutReception"),
        p.get("reference"),
        p.get("produitQ"),
        parseBoolObj(p.get("perime")),
        parseDecimal(p.get("quantiteMin")),
        parseDecimal(p.get("quantiteMax")),
        p.get("scope"),
        p.get("preset"),
        p.get("anomalyType"),
        parseBool(p.get("tousStatuts")),
        parseInt(p.get("limit"), 50),
        parseInt(p.get("offset"), 0));
  }

  private static LocalDate parseDate(String v) {
    if (v == null || v.isBlank()) {
      return null;
    }
    return LocalDate.parse(v);
  }

  private static Long parseLong(String v) {
    if (v == null || v.isBlank()) {
      return null;
    }
    return Long.parseLong(v);
  }

  private static BigDecimal parseDecimal(String v) {
    if (v == null || v.isBlank()) {
      return null;
    }
    return new BigDecimal(v);
  }

  private static boolean parseBool(String v) {
    return v != null && ("true".equalsIgnoreCase(v) || "1".equals(v));
  }

  private static Boolean parseBoolObj(String v) {
    if (v == null || v.isBlank()) {
      return null;
    }
    return parseBool(v);
  }

  private static int parseInt(String v, int def) {
    if (v == null || v.isBlank()) {
      return def;
    }
    return Integer.parseInt(v);
  }
}
