package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record StockPharmacyOverviewDTO(
    Long pharmacieId,
    String pharmacieLabel,
    StockIntelligenceSummaryDTO resume
) {
}
