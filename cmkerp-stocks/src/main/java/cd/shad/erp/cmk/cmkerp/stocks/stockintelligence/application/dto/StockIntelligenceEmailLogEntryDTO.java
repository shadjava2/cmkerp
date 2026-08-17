package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.time.LocalDateTime;

public record StockIntelligenceEmailLogEntryDTO(
    Long id,
    String reportType,
    String recipient,
    String status,
    Long snapshotId,
    String errorDetail,
    LocalDateTime sentAt
) {}
