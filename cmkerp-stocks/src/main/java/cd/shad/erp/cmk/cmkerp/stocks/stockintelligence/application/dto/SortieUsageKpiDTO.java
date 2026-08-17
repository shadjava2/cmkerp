package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record SortieUsageKpiDTO(
    long total,
    long sortiesUsage,
    long enAttente,
    long annules,
    long payees,
    long facturees,
    long pharmaciesDistinctes,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal quantiteMoyenne,
    BigDecimal montantTotal,
    BigDecimal montantMoyen,
    String derniereSortie,
    String premiereSortie,
    String periodeDebut,
    String periodeFin) {}
