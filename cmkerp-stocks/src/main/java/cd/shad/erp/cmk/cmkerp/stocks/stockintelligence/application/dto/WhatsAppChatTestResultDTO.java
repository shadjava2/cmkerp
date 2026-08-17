package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

public record WhatsAppChatTestResultDTO(
    String question,
    String answer,
    int productsFound,
    List<String> searchTerms,
    String mode) {}
