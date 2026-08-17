package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record RetourStockGroupStatDTO(
    String groupKey,
    Long groupId,
    String groupLabel,
    long nombreRetours,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal quantiteRecue,
    String derniereDate,
    String premiereDate,
    String infoComplementaire) {}
