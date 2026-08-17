package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record TransfertAnomalyDTO(
    long transfertId,
    String reference,
    String typeAnomalie,
    String statut,
    String pharmacieSource,
    String pharmacieDestination,
    String dateTransfert,
    String detail) {}
