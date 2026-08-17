package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record PharmacySummaryRowDTO(
    Long idPharmacie,
    String pharmacie,
    int totalAvecMouvement,
    int totalStockSansMouvement,
    int totalRuptureSansMouvement,
    int totalRuptures,
    int totalProduitsAnalyses
) {
}
