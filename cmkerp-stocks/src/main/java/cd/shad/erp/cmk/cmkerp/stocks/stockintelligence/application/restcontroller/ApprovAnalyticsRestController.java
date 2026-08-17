package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;
import static cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.ApprovAnalyticsService.fromParams;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovAnomalyDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovGroupStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovKpiDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovPeriodStatDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovProduitHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ApprovSearchCriteria;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.ApprovAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/pilotage/approvisionnements-analytics")
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - Pilotage approvisionnements")
public class ApprovAnalyticsRestController {

  private final Optional<ApprovAnalyticsService> service;

  @GetMapping("/kpis")
  @Operation(summary = "Indicateurs synthèse approvisionnements")
  public ResponseEntity<ApprovKpiDTO> kpis(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().kpis(criteria(all)));
  }

  @GetMapping("/liste")
  public ResponseEntity<List<ApprovListItemDTO>> liste(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().list(criteria(all)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApprovDetailDTO> detail(@PathVariable long id) {
    return ResponseEntity.ok(require().detail(id));
  }

  @GetMapping("/analyse/fournisseurs")
  public ResponseEntity<List<ApprovGroupStatDTO>> fournisseurs(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byFournisseur(criteria(all)));
  }

  @GetMapping("/analyse/pharmacies")
  public ResponseEntity<List<ApprovGroupStatDTO>> pharmacies(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byPharmacie(criteria(all)));
  }

  @GetMapping("/analyse/statuts")
  public ResponseEntity<List<ApprovGroupStatDTO>> statuts(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byStatut(criteria(all)));
  }

  @GetMapping("/analyse/utilisateurs")
  public ResponseEntity<List<ApprovGroupStatDTO>> utilisateurs(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().byUtilisateur(criteria(all)));
  }

  @GetMapping("/analyse/top-produits")
  public ResponseEntity<List<ApprovGroupStatDTO>> topProduits(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().topProduits(criteria(all), false));
  }

  @GetMapping("/analyse/produits-rares")
  public ResponseEntity<List<ApprovGroupStatDTO>> produitsRares(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().topProduits(criteria(all), true));
  }

  @GetMapping("/analyse/periode/mensuelle")
  public ResponseEntity<List<ApprovPeriodStatDTO>> mensuelle(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().mensuelle(criteria(all)));
  }

  @GetMapping("/analyse/periode/annuelle")
  public ResponseEntity<List<ApprovPeriodStatDTO>> annuelle(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().annuelle(criteria(all)));
  }

  @GetMapping("/analyse/produits/{produitId}/historique")
  public ResponseEntity<List<ApprovProduitHistoryDTO>> historiqueProduit(
      @PathVariable long produitId,
      @RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().historiqueProduit(produitId, criteria(all)));
  }

  @GetMapping("/anomalies")
  public ResponseEntity<List<ApprovAnomalyDTO>> anomalies(@RequestParam Map<String, String> all) {
    return ResponseEntity.ok(require().anomalies(criteria(all)));
  }

  @GetMapping("/lookup/fournisseurs")
  public ResponseEntity<List<Map<String, Object>>> lookupFournisseurs(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(require().lookupFournisseurs(q, limit, pharmacieId, scope));
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

  private ApprovAnalyticsService require() {
    return service.orElseThrow(() -> new IllegalStateException("Module approvisionnements analytics indisponible"));
  }

  private static ApprovSearchCriteria criteria(Map<String, String> p) {
    return fromParams(
        parseDate(p.get("dateDebut")),
        parseDate(p.get("dateFin")),
        parseLong(p.get("fournisseurId")),
        parseLong(p.get("pharmacieId")),
        parseLong(p.get("utilisateurId")),
        parseLong(p.get("produitId")),
        p.get("statut"),
        p.get("reference"),
        p.get("produitQ"),
        parseDecimal(p.get("montantMin")),
        parseDecimal(p.get("montantMax")),
        p.get("scope"),
        p.get("preset"),
        p.get("anomalyType"),
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

  private static int parseInt(String v, int def) {
    if (v == null || v.isBlank()) {
      return def;
    }
    return Integer.parseInt(v);
  }
}
