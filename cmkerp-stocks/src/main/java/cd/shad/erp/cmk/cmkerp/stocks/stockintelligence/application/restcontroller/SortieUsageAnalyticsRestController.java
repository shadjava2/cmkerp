package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;
import static cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.SortieUsageAnalyticsService.fromParams;

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

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsagePeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.SortieUsageAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/pilotage/sorties-usage-analytics")
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - Pilotage sorties stock pour usage")
public class SortieUsageAnalyticsRestController {

  private final Optional<SortieUsageAnalyticsService> service;

  @GetMapping("/kpis")
  @Operation(summary = "Indicateurs synthèse sorties stock pour usage (SORTIE-USAGE)")
  public ResponseEntity<SortieUsageKpiDTO> kpis(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().kpis(criteria(all)));
  }

  @GetMapping("/liste")
  public ResponseEntity<List<SortieUsageListItemDTO>> liste(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().list(criteria(all)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SortieUsageDetailDTO> detail(@PathVariable long id) {
    return ResponseEntity.ok(require().detail(id));
  }

  @GetMapping("/analyse/pharmacies")
  public ResponseEntity<List<SortieUsageGroupStatDTO>> pharmacies(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byPharmacie(criteria(all)));
  }

  @GetMapping("/analyse/demandeurs")
  public ResponseEntity<List<SortieUsageGroupStatDTO>> demandeurs(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byDemandeur(criteria(all)));
  }

  @GetMapping("/analyse/raisons")
  public ResponseEntity<List<SortieUsageGroupStatDTO>> raisons(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byRaisonSortie(criteria(all)));
  }

  @GetMapping("/analyse/statuts")
  public ResponseEntity<List<SortieUsageGroupStatDTO>> statuts(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byStatut(criteria(all)));
  }

  @GetMapping("/analyse/utilisateurs")
  public ResponseEntity<List<SortieUsageGroupStatDTO>> utilisateurs(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byUtilisateur(criteria(all)));
  }

  @GetMapping("/analyse/top-produits")
  public ResponseEntity<List<SortieUsageGroupStatDTO>> topProduits(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().topProduits(criteria(all), false));
  }

  @GetMapping("/analyse/produits-rares")
  public ResponseEntity<List<SortieUsageGroupStatDTO>> produitsRares(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().topProduits(criteria(all), true));
  }

  @GetMapping("/analyse/periode/mensuelle")
  public ResponseEntity<List<SortieUsagePeriodStatDTO>> mensuelle(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().mensuelle(criteria(all)));
  }

  @GetMapping("/analyse/periode/annuelle")
  public ResponseEntity<List<SortieUsagePeriodStatDTO>> annuelle(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().annuelle(criteria(all)));
  }

  @GetMapping("/analyse/produits/{produitId}/historique")
  public ResponseEntity<List<SortieUsageProduitHistoryDTO>> historiqueProduit(
      @PathVariable long produitId,
      @RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().historiqueProduit(produitId, criteria(all)));
  }

  @GetMapping("/anomalies")
  public ResponseEntity<List<SortieUsageAnomalyDTO>> anomalies(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().anomalies(criteria(all)));
  }

  @GetMapping("/lookup/pharmacies")
  public ResponseEntity<List<Map<String, Object>>> lookupPharmacies(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(require().lookupPharmacies(q, limit, pharmacieId, scope));
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

  @GetMapping("/lookup/demandeurs")
  public ResponseEntity<List<Map<String, Object>>> lookupDemandeurs(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(require().lookupDemandeurs(q, limit, pharmacieId, scope));
  }

  private SortieUsageAnalyticsService require() {
    return service.orElseThrow(() -> new IllegalStateException("Module sorties usage analytics indisponible"));
  }

  private static cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.SortieUsageSearchCriteria criteria(
      Map<String, String> p) {
    return fromParams(
        parseDate(p.get("dateDebut")),
        parseDate(p.get("dateFin")),
        parseLong(p.get("pharmacieId")),
        parseLong(p.get("utilisateurId")),
        parseLong(p.get("produitId")),
        p.get("statut"),
        p.get("reference"),
        p.get("produitQ"),
        p.get("demandeur"),
        p.get("raisonSortie"),
        parseDecimal(p.get("quantiteMin")),
        parseDecimal(p.get("quantiteMax")),
        parseDecimal(p.get("montantMin")),
        parseDecimal(p.get("montantMax")),
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

  private static int parseInt(String v, int def) {
    if (v == null || v.isBlank()) {
      return def;
    }
    return Integer.parseInt(v);
  }
}
