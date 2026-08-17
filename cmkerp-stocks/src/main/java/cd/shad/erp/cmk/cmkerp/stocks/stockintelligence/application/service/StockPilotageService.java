package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.platform.inventory.application.service.InventoryDashboardQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.AlertSummaryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.OperationDetailDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.OperationListItemDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PendingOperationsDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PharmacyScopeOptionDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PilotageAiDecisionDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PilotageDashboardDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ProductMovementEventDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockAlertMetricDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.openai.OpenAiClient;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.PharmacyScopeRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockPilotageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
@Slf4j
public class StockPilotageService {

  private final StockPilotageRepository pilotageRepository;
  private final PharmacyScopeRepository pharmacyScopeRepository;
  private final InventoryDashboardQueryService inventoryDashboardQueryService;
  private final StockIntelligenceProperties properties;
  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;

  public List<PharmacyScopeOptionDTO> listPharmacies(String scope, long userId) {
    return PortalScope.parse(scope) == PortalScope.CLIENT
        ? pharmacyScopeRepository.findClientPharmaciesForUser(userId)
        : pharmacyScopeRepository.findCentralPharmaciesForUser(userId);
  }

  public PilotageDashboardDTO getDashboard(Long pharmacieId, String scope) {
    String resolvedScope = PortalScope.parse(scope).name();
    var stats = inventoryDashboardQueryService.getDashboardStats(pharmacieId);
    PendingOperationsDTO pending = pilotageRepository.countPending(pharmacieId, resolvedScope);
    AlertSummaryDTO alerts = pilotageRepository.summarizeAlerts(pharmacieId);
    if (alerts.totalMetricsToday() == 0) {
      log.info("Aucune métrique alerte pour aujourd'hui — lancement recalcul automatique");
      pilotageRepository.recalculateMetrics(pharmacieId);
      alerts = pilotageRepository.summarizeAlerts(pharmacieId);
    }
    return new PilotageDashboardDTO(stats, pending, alerts, LocalDateTime.now().toString());
  }

  public PendingOperationsDTO getPending(Long pharmacieId, String scope) {
    return pilotageRepository.countPending(pharmacieId, PortalScope.parse(scope).name());
  }

  public List<OperationListItemDTO> listRequisitions(String statut, int limit, Long pharmacieId, String scope) {
    return pilotageRepository.findRequisitions(statut, limit, pharmacieId, PortalScope.parse(scope).name());
  }

  public List<OperationListItemDTO> listTransferts(String statut, int limit, Long pharmacieId, String scope) {
    return pilotageRepository.findTransferts(statut, limit, pharmacieId, PortalScope.parse(scope).name());
  }

  public List<OperationListItemDTO> listApprovisionnements(String statut, int limit, Long pharmacieId, String scope) {
    return pilotageRepository.findApprovisionnements(statut, limit, pharmacieId, PortalScope.parse(scope).name());
  }

  public List<OperationListItemDTO> listReceptions(String statut, int limit, Long pharmacieId, String scope) {
    return pilotageRepository.findReceptions(statut, limit, pharmacieId, PortalScope.parse(scope).name());
  }

  public OperationDetailDTO getOperationDetail(String type, Long id) {
    OperationListItemDTO header = switch (type.toLowerCase()) {
      case "requisitions", "requisition" -> pilotageRepository.findRequisitionHeader(id);
      case "transferts", "transfert" -> pilotageRepository.findTransfertHeader(id);
      case "approvisionnements", "approvisionnement", "approv" -> pilotageRepository.findApprovHeader(id);
      case "receptions", "reception" -> pilotageRepository.findReceptionHeader(id);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type d'opération inconnu: " + type);
    };
    if (header == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Opération introuvable");
    }
    var lignes = switch (type.toLowerCase()) {
      case "requisitions", "requisition" -> pilotageRepository.findRequisitionLines(id);
      case "transferts", "transfert" -> pilotageRepository.findTransfertLines(id);
      case "approvisionnements", "approvisionnement", "approv" -> pilotageRepository.findApprovLines(id);
      case "receptions", "reception" -> pilotageRepository.findReceptionLines(id);
      default -> List.<cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.OperationLineDTO>of();
    };
    return new OperationDetailDTO(header, lignes);
  }

  public List<ProductMovementEventDTO> getMovements(Long stockId, String search, Long pharmacieId,
      LocalDate from, LocalDate to, int limit) {
    Long resolvedStockId = stockId;
    if (resolvedStockId == null && search != null && !search.isBlank()) {
      resolvedStockId = pilotageRepository.findStockIdBySearch(search, pharmacieId);
    }
    if (resolvedStockId == null) {
      return List.of();
    }
    LocalDate end = to != null ? to : LocalDate.now();
    LocalDate start = from != null ? from : end.minusDays(30);
    return pilotageRepository.findMovements(resolvedStockId, start, end, limit);
  }

  public List<StockAlertMetricDTO> listAlerts(String niveau, Long pharmacieId, int limit) {
    return pilotageRepository.findAlerts(niveau, pharmacieId, limit);
  }

  @Transactional
  public int recalculateAlerts(Long pharmacieId) {
    int updated = pilotageRepository.recalculateMetrics(pharmacieId);
    log.info("Recalcul stock_alert_metrics — {} lignes upsert (pharmacieId={})", updated, pharmacieId);
    return updated;
  }

  public PilotageAiDecisionDTO generateAiDecision(Long pharmacieId, String scope) {
    if (!properties.getOpenai().isEnabled()) {
      return new PilotageAiDecisionDTO(
          "OpenAI désactivé — activez cmkerp.stock-intelligence.openai.enabled",
          "inconnu", List.of(), List.of(), "");
    }
    List<StockAlertMetricDTO> critical = pilotageRepository.findAlerts(null, pharmacieId, 25).stream()
        .filter(a -> !"NORMAL".equals(a.niveauAlerte()))
        .limit(15)
        .toList();
    PendingOperationsDTO pending = pilotageRepository.countPending(pharmacieId, PortalScope.parse(scope).name());
    try {
      String dataJson = objectMapper.writeValueAsString(Map.of(
          "operations_en_attente", pending,
          "alertes_prioritaires", critical));
      String prompt = """
          Tu es un expert en pilotage de stocks hospitaliers CMK (RDC).
          Analyse les données ERP ci-dessous et produis une aide à la décision.
          Réponds UNIQUEMENT en JSON :
          {
            "synthese": "string — 3 phrases max",
            "niveauRisqueGlobal": "faible|modere|eleve|critique",
            "actionsPrioritaires": ["string"],
            "risquesIdentifies": ["string — rupture, péremption, surstock, achat"],
            "commentaireExpert": "string"
          }
          Données :
          """ + dataJson;
      String raw = openAiClient.chatCompletionJson(
          "Tu es un directeur des achats et logistique pharmaceutique.",
          prompt);
      return objectMapper.readValue(extractJson(raw), PilotageAiDecisionDTO.class);
    } catch (Exception e) {
      log.error("Erreur analyse IA pilotage", e);
      return new PilotageAiDecisionDTO(
          "Analyse IA indisponible : " + e.getMessage(),
          "inconnu", List.of(), List.of(), "");
    }
  }

  private static String extractJson(String raw) {
    if (raw == null) {
      return "{}";
    }
    int start = raw.indexOf('{');
    int end = raw.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return raw.substring(start, end + 1);
    }
    return raw;
  }
}
