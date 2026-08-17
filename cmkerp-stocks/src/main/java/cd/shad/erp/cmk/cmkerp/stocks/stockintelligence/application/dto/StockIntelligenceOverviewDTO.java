package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StockIntelligenceOverviewDTO(
    LocalDateTime generatedAt,
    String scope,
    Long filteredPharmacieId,
    int centralPharmacyCount,
    StockIntelligenceSummaryDTO resumeGlobal,
    List<StockPharmacyOverviewDTO> pharmacies
) {
}
