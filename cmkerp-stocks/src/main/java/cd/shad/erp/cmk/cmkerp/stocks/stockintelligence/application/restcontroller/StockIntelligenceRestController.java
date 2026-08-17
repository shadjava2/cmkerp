package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceEmailHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockMovementAnalyticsService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceReportType;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockIntelligenceEmailService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockIntelligenceService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockIntelligenceService.StockIntelligenceReportResult;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.MailingSendRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockIntelligenceEmailLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE)
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence")
public class StockIntelligenceRestController {

  private final StockIntelligenceProperties properties;
  private final Optional<StockIntelligenceService> stockIntelligenceService;
  private final Optional<StockMovementAnalyticsService> analyticsService;
  private final MailingSendRepository mailingSendRepository;
  private final StockIntelligenceEmailLogRepository emailLogRepository;

  @GetMapping("/recipients")
  public ResponseEntity<Map<String, Object>> listActiveRecipients() {
    List<String> emails = mailingSendRepository.findActiveEmails();
    return ResponseEntity.ok(Map.of("source", "mailingsend", "count", emails.size(), "emails", emails));
  }

  @GetMapping("/snapshot")
  @Operation(summary = "Aperçu du snapshot stock par pharmacie centrale (sans IA)")
  public ResponseEntity<?> previewSnapshot(
      @RequestParam(required = false) Long pharmacieId) {
    return ResponseEntity.ok(requireAnalytics().buildOverview(pharmacieId));
  }

  @GetMapping("/notifications/history")
  public ResponseEntity<StockIntelligenceEmailHistoryDTO> notificationHistory(
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) String reportType) {
    return ResponseEntity.ok(new StockIntelligenceEmailHistoryDTO(
        emailLogRepository.countAll(),
        emailLogRepository.countSentTodayAll(),
        emailLogRepository.countFailedTodayAll(),
        emailLogRepository.findRecent(limit, reportType)));
  }

  @PostMapping("/report/run")
  public ResponseEntity<StockIntelligenceReportResult> runReport(
      @RequestBody(required = false) RunReportRequest request) {
    StockIntelligenceReportType type = request != null && request.reportType() != null
        ? request.reportType()
        : StockIntelligenceReportType.ON_DEMAND;
    return ResponseEntity.ok(requireService().runFullReport(type, normalizeRecipients(request)));
  }

  @PostMapping("/report/morning")
  public ResponseEntity<StockIntelligenceReportResult> runMorningReport(
      @RequestBody(required = false) RunReportRequest request) {
    return ResponseEntity.ok(requireService().runFullReport(
        StockIntelligenceReportType.MORNING, normalizeRecipients(request)));
  }

  @PostMapping("/report/evening")
  public ResponseEntity<StockIntelligenceReportResult> runEveningReport(
      @RequestBody(required = false) RunReportRequest request) {
    return ResponseEntity.ok(requireService().runFullReport(
        StockIntelligenceReportType.EVENING, normalizeRecipients(request)));
  }

  private static List<String> normalizeRecipients(RunReportRequest request) {
    if (request == null || request.recipients() == null) {
      return List.of();
    }
    return StockIntelligenceEmailService.normalizeEmails(request.recipients());
  }

  private StockIntelligenceService requireService() {
    return stockIntelligenceService.orElseThrow(() -> new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        moduleDisabledMessage()));
  }

  private StockMovementAnalyticsService requireAnalytics() {
    return analyticsService.orElseThrow(() -> new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        moduleDisabledMessage()));
  }

  private String moduleDisabledMessage() {
    if (!properties.isEnabled()) {
      return "Module stock-intelligence désactivé (cmkerp.stock-intelligence.enabled=false). "
          + "Activez-le dans application-dev.yml ou définissez CMK_STOCK_INTELLIGENCE_ENABLED=true.";
    }
    return "Module stock-intelligence non initialisé — redémarrez le gateway après recompilation "
        + "(mvn -pl cmkerp-gateway -am install -DskipTests).";
  }

  public record RunReportRequest(StockIntelligenceReportType reportType, List<String> recipients) {}
}
