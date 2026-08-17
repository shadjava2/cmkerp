package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record RetourStockAnomalyDTO(
    long retourId,
    String reference,
    String typeAnomalie,
    String statut,
    String pharmacieSource,
    String pharmacieDestination,
    String dateRetour,
    String detail) {}
