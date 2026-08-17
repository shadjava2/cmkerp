package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceReportType;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockIntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnStockIntelligenceEnabled
@ConditionalOnProperty(prefix = "cmkerp.stock-intelligence", name = "evening-report-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StockIntelligenceEveningJob {

  private final StockIntelligenceService stockIntelligenceService;

  @Scheduled(cron = "${cmkerp.stock-intelligence.evening-cron:0 0 18 * * ?}")
  public void sendEveningReport() {
    log.info("Démarrage job rapport stock intelligence (soir)");
    try {
      stockIntelligenceService.runFullReport(StockIntelligenceReportType.EVENING, java.util.List.of());
    } catch (Exception e) {
      log.error("Échec job rapport stock soir", e);
    }
  }
}
