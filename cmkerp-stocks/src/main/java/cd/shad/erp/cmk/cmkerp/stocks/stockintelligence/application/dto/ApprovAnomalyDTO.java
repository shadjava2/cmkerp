package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record ApprovAnomalyDTO(
    long approvisionnementId,
    String reference,
    String typeAnomalie,
    String statut,
    String fournisseur,
    String pharmacie,
    String dateApprovisionnement,
    String detail) {}
