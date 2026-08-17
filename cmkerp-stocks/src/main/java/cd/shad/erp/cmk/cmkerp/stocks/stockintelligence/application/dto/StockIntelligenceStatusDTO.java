package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StockIntelligenceStatusDTO(
    boolean enabled,
    boolean morningJobEnabled,
    boolean eveningJobEnabled,
    boolean openAiEnabled,
    boolean whatsAppEnabled,
    boolean whatsAppConfigured,
    boolean whatsAppReady,
    String whatsAppStatusHint,
    boolean emailConfigured,
    int activeMailingSendCount,
    List<String> activeEmails,
    int activeWhatsAppCount,
    List<String> activeWhatsAppNumbers,
    int morningEmailsSentToday,
    int eveningEmailsSentToday,
    int totalEmailHistoryLogged,
    int totalWhatsAppHistoryLogged,
    boolean reportInProgress,
    String lastReportStatus,
    String lastReportType,
    LocalDateTime lastReportAt
) {}
