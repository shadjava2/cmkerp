package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockAlertMetricDTO(
    Long id,
    Long stockId,
    Long produitId,
    Long pharmacieId,
    String produitLabel,
    String pharmacieLabel,
    BigDecimal stockActuel,
    BigDecimal consommation30j,
    BigDecimal consommationMoyenneJour,
    BigDecimal joursCouverture,
    String niveauAlerte,
    String messageAlerte,
    LocalDate dateCalcul) {}
