package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record TransfertGroupStatDTO(
    String groupKey,
    Long groupId,
    String groupLabel,
    long nombreTransferts,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    String derniereDate,
    String premiereDate,
    String infoComplementaire) {}
