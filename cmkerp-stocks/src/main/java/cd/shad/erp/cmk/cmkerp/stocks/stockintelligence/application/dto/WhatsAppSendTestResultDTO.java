package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record WhatsAppSendTestResultDTO(
    String phone,
    String question,
    String answer,
    int productsFound,
    String status,
    String errorDetail) {}
