package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record RetourStockKpiDTO(
    long total,
    long transferes,
    long receptionnes,
    long retoursValides,
    long rejetes,
    long enAttente,
    long annules,
    long perimes,
    long pharmaciesSourceDistinctes,
    long pharmaciesDestinationDistinctes,
    long produitsDistincts,
    BigDecimal quantiteTotale,
    BigDecimal quantiteRecue,
    BigDecimal quantiteMoyenne,
    String dernierRetour,
    String premierRetour,
    String periodeDebut,
    String periodeFin) {}
