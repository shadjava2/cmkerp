package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record AlertSummaryDTO(
    int rupture,
    int critique,
    int surveillance,
    int dormant,
    int surstock,
    int normal,
    int totalMetricsToday) {}
