package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.time.LocalDateTime;

public record WhatsAppChatLogEntryDTO(
    Long id,
    String direction,
    String fromNumber,
    String messageText,
    String aiResponse,
    String status,
    String errorDetail,
    LocalDateTime createdAt) {}
