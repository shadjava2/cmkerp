package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.OperationDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.OperationListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PendingOperationsDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PharmacyScopeOptionDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PilotageAiDecisionDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PilotageDashboardDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ProductMovementEventDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockAlertMetricDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockPilotageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/pilotage")
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - Pilotage")
public class StockPilotageRestController {

  private final Optional<StockPilotageService> pilotageService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping("/pharmacies")
  @Operation(summary = "Pharmacies du portail accessibles à l'utilisateur connecté")
  public ResponseEntity<List<PharmacyScopeOptionDTO>> pharmacies(
      @RequestParam(defaultValue = "CENTRALE") String scope,
      HttpServletRequest request) {
    long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    return ResponseEntity.ok(requireService().listPharmacies(scope, userId));
  }

  @GetMapping("/dashboard")
  @Operation(summary = "Tableau de bord pilotage stock — KPI, opérations en attente, alertes")
  public ResponseEntity<PilotageDashboardDTO> dashboard(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(requireService().getDashboard(pharmacieId, scope));
  }

  @GetMapping("/operations/en-attente")
  public ResponseEntity<PendingOperationsDTO> pending(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(requireService().getPending(pharmacieId, scope));
  }

  @GetMapping("/operations/requisitions")
  public ResponseEntity<List<OperationListItemDTO>> requisitions(
      @RequestParam(required = false) String statut,
      @RequestParam(defaultValue = "30") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(requireService().listRequisitions(statut, limit, pharmacieId, scope));
  }

  @GetMapping("/operations/transferts")
  public ResponseEntity<List<OperationListItemDTO>> transferts(
      @RequestParam(required = false) String statut,
      @RequestParam(defaultValue = "30") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(requireService().listTransferts(statut, limit, pharmacieId, scope));
  }

  @GetMapping("/operations/approvisionnements")
  public ResponseEntity<List<OperationListItemDTO>> approvisionnements(
      @RequestParam(required = false) String statut,
      @RequestParam(defaultValue = "30") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(requireService().listApprovisionnements(statut, limit, pharmacieId, scope));
  }

  @GetMapping("/operations/receptions")
  public ResponseEntity<List<OperationListItemDTO>> receptions(
      @RequestParam(required = false) String statut,
      @RequestParam(defaultValue = "30") int limit,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(requireService().listReceptions(statut, limit, pharmacieId, scope));
  }

  @GetMapping("/operations/{type}/{id}")
  @Operation(summary = "Détail d'une opération avec lignes produits")
  public ResponseEntity<OperationDetailDTO> operationDetail(
      @PathVariable String type,
      @PathVariable Long id) {
    return ResponseEntity.ok(requireService().getOperationDetail(type, id));
  }

  @GetMapping("/mouvements")
  @Operation(summary = "Mouvements d'un produit/stock sur une période (entrées, sorties)")
  public ResponseEntity<List<ProductMovementEventDTO>> mouvements(
      @RequestParam(required = false) Long stockId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "100") int limit) {
    return ResponseEntity.ok(requireService().getMovements(stockId, q, pharmacieId, from, to, limit));
  }

  @GetMapping("/alertes")
  public ResponseEntity<List<StockAlertMetricDTO>> alertes(
      @RequestParam(required = false) String niveau,
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "50") int limit) {
    return ResponseEntity.ok(requireService().listAlerts(niveau, pharmacieId, limit));
  }

  @PostMapping("/alertes/recalculer")
  @Operation(summary = "Recalcule et alimente stock_alert_metrics + settings par défaut")
  public ResponseEntity<MapResponse> recalculate(
      @RequestParam(required = false) Long pharmacieId) {
    int rows = requireService().recalculateAlerts(pharmacieId);
    return ResponseEntity.ok(new MapResponse(rows, "Recalcul terminé"));
  }

  @PostMapping("/ai/decision")
  @Operation(summary = "Aide à la décision IA — rupture, péremption, achats")
  public ResponseEntity<PilotageAiDecisionDTO> aiDecision(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(defaultValue = "CENTRALE") String scope) {
    return ResponseEntity.ok(requireService().generateAiDecision(pharmacieId, scope));
  }

  private StockPilotageService requireService() {
    return pilotageService.orElseThrow(() -> new IllegalStateException("Module stock-intelligence indisponible"));
  }

  public record MapResponse(int rowsUpdated, String message) {}
}
