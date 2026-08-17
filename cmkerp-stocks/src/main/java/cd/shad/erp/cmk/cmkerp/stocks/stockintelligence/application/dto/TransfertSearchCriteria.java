package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransfertSearchCriteria(
    LocalDate dateDebut,
    LocalDate dateFin,
    Long pharmacieSourceId,
    Long pharmacieDestinationId,
    Long utilisateurId,
    Long produitId,
    String statut,
    String reference,
    String produitQ,
    BigDecimal quantiteMin,
    BigDecimal quantiteMax,
    String scope,
    String preset,
    String anomalyType,
    boolean tousStatuts,
    int limit,
    int offset) {

  public static TransfertSearchCriteria defaults() {
    return new TransfertSearchCriteria(null, null, null, null, null, null, null, null, null,
        null, null, "CENTRALE", null, null, false, 50, 0);
  }
}
