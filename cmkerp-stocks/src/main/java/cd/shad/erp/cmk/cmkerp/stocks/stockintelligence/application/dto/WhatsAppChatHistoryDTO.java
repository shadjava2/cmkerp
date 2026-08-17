package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

public record WhatsAppChatHistoryDTO(
    long totalLogged,
    List<WhatsAppChatLogEntryDTO> entries) {}
