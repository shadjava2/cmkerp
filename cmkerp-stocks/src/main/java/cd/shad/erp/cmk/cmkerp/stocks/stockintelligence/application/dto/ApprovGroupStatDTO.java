package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record ApprovGroupStatDTO(
    String groupKey,
    Long groupId,
    String groupLabel,
    long nombreApprovisionnements,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String derniereDate,
    String premiereDate,
    String infoComplementaire) {}
