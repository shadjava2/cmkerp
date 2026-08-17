package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

public record StockIntelligenceEmailHistoryDTO(
    int totalLogged,
    int sentToday,
    int failedToday,
    List<StockIntelligenceEmailLogEntryDTO> entries
) {}
