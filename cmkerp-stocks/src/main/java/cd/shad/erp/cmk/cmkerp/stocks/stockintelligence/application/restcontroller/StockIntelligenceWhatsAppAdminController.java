package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK;

import java.util.List;
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

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppChatHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppChatTestResultDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSendTestResultDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSetupDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockWhatsAppChatService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.WhatsAppConfigHelper;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.WhatsAppSendService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.WhatsAppChatLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE + "/whatsapp")
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - WhatsApp")
public class StockIntelligenceWhatsAppAdminController {

  private final StockIntelligenceProperties properties;
  private final Optional<StockWhatsAppChatService> chatService;
  private final WhatsAppSendService whatsAppSendService;
  private final WhatsAppChatLogRepository chatLogRepository;

  @GetMapping("/setup")
  @Operation(summary = "Configuration WhatsApp (webhook Meta, tokens)")
  public ResponseEntity<WhatsAppSetupDTO> setup() {
    var wa = properties.getWhatsapp();
    boolean configured = WhatsAppConfigHelper.isSecretConfigured(wa.getToken())
        && WhatsAppConfigHelper.isSecretConfigured(wa.getPhoneNumberId());
    List<String> activeNumbers = whatsAppSendService.findActivePhones();
    return ResponseEntity.ok(new WhatsAppSetupDTO(
        wa.isEnabled(),
        configured,
        wa.isEnabled() && configured,
        WhatsAppConfigHelper.isSecretConfigured(wa.getToken()),
        WhatsAppConfigHelper.isSecretConfigured(wa.getPhoneNumberId()),
        wa.getVerifyToken(),
        STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK,
        wa.getGraphApiVersion(),
        wa.getAllowedNumbers() != null ? wa.getAllowedNumbers() : List.of(),
        activeNumbers.size(),
        activeNumbers,
        properties.getOpenai().isEnabled(),
        WhatsAppConfigHelper.statusHint(wa.isEnabled(), configured)));
  }

  @GetMapping("/history")
  @Operation(summary = "Historique des échanges WhatsApp (chat expert stock)")
  public ResponseEntity<WhatsAppChatHistoryDTO> history(@RequestParam(defaultValue = "50") int limit) {
    return ResponseEntity.ok(new WhatsAppChatHistoryDTO(
        chatLogRepository.countAll(),
        chatLogRepository.findRecent(limit)));
  }

  @PostMapping("/chat/test")
  @Operation(summary = "Simuler une question WhatsApp (sans envoi Meta)")
  public ResponseEntity<WhatsAppChatTestResultDTO> testChat(@RequestBody ChatTestRequest request) {
    if (request == null || request.question() == null || request.question().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question requise");
    }
    StockWhatsAppChatService service = requireChatService();
    return ResponseEntity.ok(service.testQuestion(request.question().trim()));
  }

  @PostMapping("/send-test")
  @Operation(summary = "Test complet : analyse ERP + envoi WhatsApp réel")
  public ResponseEntity<WhatsAppSendTestResultDTO> sendTest(@RequestBody SendTestRequest request) {
    if (request == null || request.phone() == null || request.phone().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone requis");
    }
    if (request.question() == null || request.question().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question requise");
    }
    return ResponseEntity.ok(requireChatService().sendTestMessage(
        request.phone().trim(), request.question().trim()));
  }

  private StockWhatsAppChatService requireChatService() {
    return chatService.orElseThrow(() -> new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Module stock-intelligence indisponible"));
  }

  public record ChatTestRequest(String question) {}

  public record SendTestRequest(String phone, String question) {}
}
