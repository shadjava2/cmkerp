package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Traitement asynchrone des messages WhatsApp (Meta exige une réponse HTTP rapide au webhook).
 */
@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
@Slf4j
public class WhatsAppMessageAsyncProcessor {

  private final StockIntelligenceService stockIntelligenceService;

  @Async("cmkerpAsyncExecutor")
  public void processTextMessage(String from, String messageId, String text) {
    log.debug("WhatsApp traitement async — from={}, msgId={}", from, messageId);
    stockIntelligenceService.handleWhatsAppTextMessage(from, messageId, text);
  }
}
