package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_BASE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.CentralPharmacyOptionDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceEmailHistoryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceStatusDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.StockIntelligenceService;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service.WhatsAppConfigHelper;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.CentralPharmacyRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.MailingSendRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockIntelligenceEmailLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(STOCK_INTELLIGENCE_BASE)
@RequiredArgsConstructor
@Tag(name = "Stock Intelligence - Status")
public class StockIntelligenceStatusController {

  private final StockIntelligenceProperties properties;
  private final Optional<StockIntelligenceService> stockIntelligenceService;
  private final Optional<MailingSendRepository> mailingSendRepository;
  private final CentralPharmacyRepository centralPharmacyRepository;
  private final StockIntelligenceEmailLogRepository emailLogRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping("/pharmacies-central")
  @Operation(summary = "Pharmacies centrales accessibles à l'utilisateur connecté")
  public ResponseEntity<List<CentralPharmacyOptionDTO>> listCentralPharmacies(HttpServletRequest request) {
    long userId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    return ResponseEntity.ok(centralPharmacyRepository.findCentralPharmaciesWithStockForUser(userId));
  }

  @GetMapping("/status")
  @Operation(summary = "État du module stock intelligence et destinataires mailingsend")
  public ResponseEntity<StockIntelligenceStatusDTO> status() {
    if (stockIntelligenceService.isPresent()) {
      return ResponseEntity.ok(stockIntelligenceService.get().getStatus());
    }
    List<String> emails = mailingSendRepository.map(MailingSendRepository::findActiveEmails).orElse(Collections.emptyList());
    var wa = properties.getWhatsapp();
    boolean waConfigured = WhatsAppConfigHelper.isSecretConfigured(wa.getToken())
        && WhatsAppConfigHelper.isSecretConfigured(wa.getPhoneNumberId());
    return ResponseEntity.ok(new StockIntelligenceStatusDTO(
        properties.isEnabled(),
        properties.isMorningReportEnabled(),
        properties.isEveningReportEnabled(),
        properties.getOpenai().isEnabled(),
        wa.isEnabled(),
        waConfigured,
        wa.isEnabled() && waConfigured,
        WhatsAppConfigHelper.statusHint(wa.isEnabled(), waConfigured),
        false,
        emails.size(),
        emails,
        0,
        List.of(),
        0,
        0,
        emailLogRepository.countAll(),
        0,
        false,
        null,
        null,
        null));
  }

  @GetMapping("/email-history")
  @Operation(summary = "Historique des notifications email stock intelligence")
  public ResponseEntity<StockIntelligenceEmailHistoryDTO> emailHistory(
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) String reportType) {
    return ResponseEntity.ok(new StockIntelligenceEmailHistoryDTO(
        emailLogRepository.countAll(),
        emailLogRepository.countSentTodayAll(),
        emailLogRepository.countFailedTodayAll(),
        emailLogRepository.findRecent(limit, reportType)));
  }
}
