package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record SortieUsagePeriodStatDTO(
    String periode,
    long nombreSorties,
    long pharmaciesDistinctes,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String pharmacieDominante,
    String demandeurDominant) {}
