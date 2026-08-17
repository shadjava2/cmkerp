package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.EmailDeliveryResultDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceAnalysisDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceMultiSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceOverviewDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceReportType;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceStatusDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.MailingSendRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockIntelligenceEmailLogRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockIntelligenceSnapshotRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.WhatsAppChatLogRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.WhatsAppSendRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.whatsapp.WhatsAppCloudApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
@Slf4j
public class StockIntelligenceService {

  private final StockIntelligenceProperties properties;
  private final StockMovementAnalyticsService analyticsService;
  private final StockIntelligenceOpenAiService openAiService;
  private final StockIntelligenceEmailService emailService;
  private final StockIntelligenceSnapshotRepository snapshotRepository;
  private final StockIntelligenceEmailLogRepository emailLogRepository;
  private final MailingSendRepository mailingSendRepository;
  private final WhatsAppCloudApiClient whatsAppClient;
  private final WhatsAppChatLogRepository chatLogRepository;
  private final WhatsAppSendRepository whatsAppSendRepository;
  private final StockWhatsAppChatService whatsAppChatService;
  private final ObjectMapper objectMapper;

  private final ReentrantLock reportLock = new ReentrantLock();
  private volatile StockIntelligenceMultiSnapshotDTO cachedSnapshot;
  private volatile LocalDateTime lastReportAt;
  private volatile String lastReportStatus;
  private volatile StockIntelligenceReportType lastReportType;

  public StockIntelligenceStatusDTO getStatus() {
    List<String> emails = mailingSendRepository.findActiveEmails();
    List<String> waPhones = whatsAppSendRepository.findActivePhones();
    var wa = properties.getWhatsapp();
    boolean waConfigured = WhatsAppConfigHelper.isSecretConfigured(wa.getToken())
        && WhatsAppConfigHelper.isSecretConfigured(wa.getPhoneNumberId());
    boolean waReady = wa.isEnabled() && waConfigured;
    return new StockIntelligenceStatusDTO(
        properties.isEnabled(),
        properties.isMorningReportEnabled(),
        properties.isEveningReportEnabled(),
        properties.getOpenai().isEnabled(),
        wa.isEnabled(),
        waConfigured,
        waReady,
        WhatsAppConfigHelper.statusHint(wa.isEnabled(), waConfigured),
        emailService.isMailConfigured(),
        emails.size(),
        emails,
        waPhones.size(),
        waPhones,
        emailLogRepository.countSentToday("MORNING"),
        emailLogRepository.countSentToday("EVENING"),
        emailLogRepository.countAll(),
        chatLogRepository.countAll(),
        reportLock.isLocked(),
        lastReportStatus,
        lastReportType != null ? lastReportType.name() : null,
        lastReportAt);
  }

  @Transactional
  public StockIntelligenceReportResult runFullReport(
      StockIntelligenceReportType reportType,
      List<String> emailRecipients) {

    if (!reportLock.tryLock()) {
      log.warn("Rapport {} ignoré — un autre rapport est déjà en cours", reportType);
      recordReportOutcome(reportType, "ALREADY_RUNNING");
      return new StockIntelligenceReportResult(reportType, null, null, EmailDeliveryResultDTO.empty(), "ALREADY_RUNNING");
    }
    try {
      return executeReport(reportType, emailRecipients);
    } finally {
      reportLock.unlock();
    }
  }

  private StockIntelligenceReportResult executeReport(
      StockIntelligenceReportType reportType,
      List<String> emailRecipients) {

    StockIntelligenceMultiSnapshotDTO multi;
    StockIntelligenceOverviewDTO overview;
    try {
      multi = analyticsService.buildMultiSnapshot(null);
      cachedSnapshot = multi;
      overview = analyticsService.toOverview(multi);
    } catch (Exception e) {
      log.error("Échec construction snapshot stock", e);
      recordReportOutcome(reportType, "SNAPSHOT_FAILED");
      return new StockIntelligenceReportResult(
          reportType, null, null, EmailDeliveryResultDTO.empty(), "SNAPSHOT_FAILED");
    }

    StockIntelligenceAnalysisDTO analysis = properties.getOpenai().isEnabled()
        ? openAiService.analyzeSnapshot(multi, reportType)
        : null;

    Long snapshotId = null;
    try {
      String snapshotJson = objectMapper.writeValueAsString(multi);
      String analysisJson = analysis != null ? objectMapper.writeValueAsString(analysis) : null;
      snapshotId = snapshotRepository.save(reportType.name(), null, snapshotJson, analysisJson);
    } catch (Exception e) {
      log.warn("Impossible de persister le snapshot", e);
    }

    List<String> normalizedRecipients = StockIntelligenceEmailService.normalizeEmails(emailRecipients);
    EmailDeliveryResultDTO delivery = emailService.sendReport(reportType, multi, analysis, normalizedRecipients, snapshotId);

    String status = resolveDeliveryStatus(delivery, normalizedRecipients);
    recordReportOutcome(reportType, status);
    logReportOutcome(reportType, overview, delivery, status);
    return new StockIntelligenceReportResult(reportType, overview, analysis, delivery, status);
  }

  private static void logReportOutcome(
      StockIntelligenceReportType reportType,
      StockIntelligenceOverviewDTO overview,
      EmailDeliveryResultDTO delivery,
      String status) {
    int products = overview != null ? overview.resumeGlobal().totalProduitsAnalyses() : 0;
    int pharmacies = overview != null ? overview.pharmacies().size() : 0;
    int sent = delivery != null ? delivery.sent() : 0;
    int total = delivery != null ? delivery.totalRecipients() : 0;
    log.info("Rapport {} — statut={}, pharmacies={}, produits={}, emails {}/{}",
        reportType, status, pharmacies, products, sent, total);
    if (delivery != null && !delivery.failedAddresses().isEmpty()) {
      log.warn("Emails en échec ({}): {}", delivery.failed(), delivery.failedAddresses());
    }
  }

  private void recordReportOutcome(StockIntelligenceReportType reportType, String status) {
    lastReportAt = LocalDateTime.now();
    lastReportType = reportType;
    lastReportStatus = status;
  }

  private String resolveDeliveryStatus(EmailDeliveryResultDTO delivery, List<String> requestedRecipients) {
    if (delivery.totalRecipients() == 0) {
      if (!requestedRecipients.isEmpty() && !emailService.isMailConfigured()) {
        return "EMAIL_NOT_CONFIGURED";
      }
      return "OK_NO_RECIPIENTS";
    }
    if (!emailService.isMailConfigured() && delivery.sent() == 0) {
      return "EMAIL_NOT_CONFIGURED";
    }
    if (delivery.failed() == 0) {
      return "OK";
    }
    if (delivery.sent() > 0) {
      return "PARTIAL_EMAIL_FAILURE";
    }
    return "EMAIL_FAILED";
  }

  public StockIntelligenceOverviewDTO getSnapshotPreview(Long pharmacieId) {
    return analyticsService.buildOverview(pharmacieId);
  }

  public void handleWhatsAppTextMessage(String from, String messageId, String text) {
    if (!properties.getWhatsapp().isEnabled()) {
      log.warn("Message WhatsApp ignoré — whatsapp.enabled=false");
      return;
    }
    if (!isAllowedNumber(from)) {
      log.warn("Numéro WhatsApp non autorisé: {} — ajoutez-le dans Chat WhatsApp > Numéros autorisés", from);
      try {
        whatsAppClient.sendTextMessage(from,
            "Votre numéro n'est pas autorisé à utiliser l'assistant stock CMK. Contactez l'administrateur.");
      } catch (Exception e) {
        log.error("Impossible d'informer le numéro non autorisé {}", from, e);
      }
      return;
    }

    log.info("Traitement message WhatsApp entrant — from={}, msgId={}", from, messageId);
    chatLogRepository.logInbound(messageId, from, text);

    String answer;
    String status = "SENT";
    String error = null;
    try {
      answer = whatsAppChatService.answerQuestion(text);
      whatsAppClient.sendTextMessage(from, answer);
    } catch (Exception e) {
      status = "FAILED";
      error = e.getMessage();
      answer = "Erreur lors du traitement de votre demande. Réessayez plus tard.";
      log.error("Erreur traitement WhatsApp", e);
      try {
        whatsAppClient.sendTextMessage(from, answer);
      } catch (Exception sendErr) {
        log.error("Impossible d'envoyer le message d'erreur WhatsApp", sendErr);
      }
    }
    chatLogRepository.logOutbound(from, text, answer, null, status, error);
  }

  private boolean isAllowedNumber(String from) {
    java.util.ArrayList<String> allowed = new java.util.ArrayList<>(whatsAppSendRepository.findActivePhones());
    List<String> yamlList = properties.getWhatsapp().getAllowedNumbers();
    if (yamlList != null) {
      for (String n : yamlList) {
        if (n == null || n.isBlank() || n.contains("XXXX")) {
          continue;
        }
        String digits = n.replaceAll("[^0-9]", "");
        if (digits.length() >= 9) {
          allowed.add(digits);
        }
      }
    }
    if (allowed.isEmpty()) {
      return true;
    }
    String normalized = from.replaceAll("[^0-9]", "");
    return allowed.stream().anyMatch(allowedPhone -> phonesMatch(normalized, allowedPhone));
  }

  /** Compare numéros Meta (243…) avec entrées DB (243… ou format local). */
  private static boolean phonesMatch(String fromDigits, String allowedDigits) {
    String a = allowedDigits.replaceAll("[^0-9]", "");
    if (fromDigits.equals(a)) {
      return true;
    }
    if (fromDigits.endsWith(a) || a.endsWith(fromDigits)) {
      return true;
    }
    if (fromDigits.length() >= 9 && a.length() >= 9) {
      return fromDigits.substring(fromDigits.length() - 9).equals(a.substring(a.length() - 9));
    }
    return false;
  }

  public record StockIntelligenceReportResult(
      StockIntelligenceReportType reportType,
      StockIntelligenceOverviewDTO overview,
      StockIntelligenceAnalysisDTO analysis,
      EmailDeliveryResultDTO emailDelivery,
      String status) {}
}
