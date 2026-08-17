package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record PendingOperationsDTO(
    int requisitionsEnAttente,
    int transfertsEnAttente,
    int approvisionnementsEnAttente,
    int receptionsEnAttente) {}
