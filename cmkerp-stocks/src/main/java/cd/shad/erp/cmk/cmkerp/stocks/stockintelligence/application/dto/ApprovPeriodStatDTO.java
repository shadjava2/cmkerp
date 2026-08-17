package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record ApprovPeriodStatDTO(
    String periode,
    long nombreApprovisionnements,
    long fournisseursDistincts,
    long pharmaciesDistinctes,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String fournisseurDominant,
    String pharmacieDominante) {}
