package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record StockIntelligenceSummaryDTO(
    int totalAvecMouvement,
    int totalStockSansMouvement,
    int totalRuptureSansMouvement,
    int totalRuptures,
    int totalProduitsAnalyses
) {
}
