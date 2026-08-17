package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.scheduler;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceReportType;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockIntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exécution immédiate d'un rapport au démarrage (dev/test).
 * Activer : cmkerp.stock-intelligence.run-on-startup=true
 */
@Component
@ConditionalOnStockIntelligenceEnabled
@ConditionalOnProperty(prefix = "cmkerp.stock-intelligence", name = "run-on-startup", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StockIntelligenceStartupRunner implements ApplicationRunner {

  private final StockIntelligenceService stockIntelligenceService;

  @Override
  public void run(ApplicationArguments args) {
    log.info("Stock intelligence — exécution rapport au démarrage (MORNING)");
    try {
      stockIntelligenceService.runFullReport(StockIntelligenceReportType.MORNING, java.util.List.of());
    } catch (Exception e) {
      log.error("Échec rapport stock intelligence au démarrage", e);
    }
  }
}
