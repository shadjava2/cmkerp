package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record TransfertPeriodStatDTO(
    String periode,
    long nombreTransferts,
    long pharmaciesSourceDistinctes,
    long pharmaciesDestinationDistinctes,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    String pharmacieDestinationDominante,
    String pharmacieSourceDominante) {}
