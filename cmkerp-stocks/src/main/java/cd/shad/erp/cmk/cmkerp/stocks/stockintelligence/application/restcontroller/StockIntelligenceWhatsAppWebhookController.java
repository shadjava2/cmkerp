package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockIntelligenceService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.WhatsAppMessageAsyncProcessor;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK)
@RequiredArgsConstructor
@Slf4j
public class StockIntelligenceWhatsAppWebhookController {

  private final StockIntelligenceProperties properties;
  private final Optional<StockIntelligenceService> stockIntelligenceService;
  private final Optional<WhatsAppMessageAsyncProcessor> whatsAppAsyncProcessor;

  @GetMapping
  public ResponseEntity<String> verify(
      @RequestParam(name = "hub.mode", required = false) String mode,
      @RequestParam(name = "hub.verify_token", required = false) String token,
      @RequestParam(name = "hub.challenge", required = false) String challenge) {
    if ("subscribe".equals(mode) && properties.getWhatsapp().getVerifyToken().equals(token)) {
      return ResponseEntity.ok(challenge);
    }
    return ResponseEntity.status(403).body("Forbidden");
  }

  @PostMapping
  public ResponseEntity<Map<String, String>> receive(@RequestBody JsonNode payload) {
    if (stockIntelligenceService.isEmpty() || whatsAppAsyncProcessor.isEmpty()) {
      log.warn("Webhook WhatsApp reçu mais stock-intelligence désactivé");
      return ResponseEntity.ok(Map.of("status", "disabled"));
    }
    int queued = 0;
    try {
      for (JsonNode entry : payload.path("entry")) {
        for (JsonNode change : entry.path("changes")) {
          JsonNode value = change.path("value");
          for (JsonNode msg : value.path("messages")) {
            if (!"text".equals(msg.path("type").asText())) {
              log.debug("Webhook WhatsApp — message non texte ignoré (type={})", msg.path("type").asText());
              continue;
            }
            String from = msg.path("from").asText();
            String messageId = msg.path("id").asText();
            String body = msg.path("text").path("body").asText();
            log.info("Webhook WhatsApp — message entrant de {} (id={}): {}", from, messageId, truncate(body, 80));
            whatsAppAsyncProcessor.get().processTextMessage(from, messageId, body);
            queued++;
          }
          for (JsonNode status : value.path("statuses")) {
            log.debug("Webhook WhatsApp — statut livraison {} pour {}", status.path("status").asText(), status.path("recipient_id").asText());
          }
        }
      }
      if (queued == 0) {
        log.debug("Webhook WhatsApp POST sans message texte (ping Meta ou statut livraison)");
      }
    } catch (Exception e) {
      log.error("Erreur parsing webhook WhatsApp", e);
    }
    return ResponseEntity.ok(Map.of("status", "ok"));
  }

  private static String truncate(String s, int max) {
    if (s == null || s.length() <= max) {
      return s;
    }
    return s.substring(0, max - 3) + "...";
  }
}
