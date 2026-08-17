package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record StockIntelligenceSnapshotDTO(
    LocalDateTime generatedAt,
    Long pharmacieId,
    String pharmacieLabel,
    Map<StockProductCategory, List<StockProductInsightDTO>> produitsParCategorie,
    StockIntelligenceSummaryDTO resume
) {
}
