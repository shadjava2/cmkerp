package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record ApprovKpiDTO(
    long total,
    long valides,
    long annules,
    long enAttente,
    long fournisseursDistincts,
    long pharmaciesDistinctes,
    long produitsDistincts,
    BigDecimal montantTotal,
    BigDecimal montantMoyen,
    String dernierApprovisionnement,
    String premierApprovisionnement,
    String periodeDebut,
    String periodeFin) {}
