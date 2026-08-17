package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record TransfertKpiDTO(
    long total,
    long transferes,
    long receptionnes,
    long sortiesValidees,
    long annules,
    long enAttente,
    long pharmaciesSourceDistinctes,
    long pharmaciesDestinationDistinctes,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal quantiteMoyenne,
    String dernierTransfert,
    String premierTransfert,
    String periodeDebut,
    String periodeFin) {}
