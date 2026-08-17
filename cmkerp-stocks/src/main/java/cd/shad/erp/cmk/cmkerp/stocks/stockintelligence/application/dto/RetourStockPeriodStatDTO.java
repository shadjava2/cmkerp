package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record RetourStockPeriodStatDTO(
    String periode,
    long nombreRetours,
    long pharmaciesSourceDistinctes,
    long pharmaciesDestinationDistinctes,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal quantiteRecue,
    String pharmacieSourceDominante,
    String pharmacieDestinationDominante) {}
