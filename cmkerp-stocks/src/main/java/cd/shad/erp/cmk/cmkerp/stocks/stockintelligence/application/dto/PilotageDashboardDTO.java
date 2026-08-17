package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.InventoryStatsResponse;

public record PilotageDashboardDTO(
    InventoryStatsResponse inventoryStats,
    PendingOperationsDTO pending,
    AlertSummaryDTO alerts,
    String generatedAt) {}
