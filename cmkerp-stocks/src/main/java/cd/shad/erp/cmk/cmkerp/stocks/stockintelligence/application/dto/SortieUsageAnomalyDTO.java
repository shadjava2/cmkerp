package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record SortieUsageAnomalyDTO(
    long sortieId,
    String reference,
    String typeAnomalie,
    String statut,
    String pharmacie,
    String demandeur,
    String dateSortie,
    String detail) {}
