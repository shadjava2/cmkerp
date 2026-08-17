package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApprovSearchCriteria(
    LocalDate dateDebut,
    LocalDate dateFin,
    Long fournisseurId,
    Long pharmacieId,
    Long utilisateurId,
    Long produitId,
    String statut,
    String reference,
    String produitQ,
    BigDecimal montantMin,
    BigDecimal montantMax,
    String scope,
    String preset,
    String anomalyType,
    int limit,
    int offset) {

  public static ApprovSearchCriteria defaults() {
    return new ApprovSearchCriteria(null, null, null, null, null, null, null, null, null,
        null, null, "CENTRALE", null, null, 50, 0);
  }
}
