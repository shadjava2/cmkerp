package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StockIntelligenceMultiSnapshotDTO(
    LocalDateTime generatedAt,
    StockIntelligenceSummaryDTO resumeGlobal,
    List<StockIntelligenceSnapshotDTO> pharmacies
) {
}
