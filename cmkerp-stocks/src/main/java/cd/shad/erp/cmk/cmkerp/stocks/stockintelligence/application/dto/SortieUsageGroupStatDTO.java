package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record SortieUsageGroupStatDTO(
    String groupKey,
    Long groupId,
    String groupLabel,
    long nombreSorties,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String derniereDate,
    String premiereDate,
    String infoComplementaire) {}
