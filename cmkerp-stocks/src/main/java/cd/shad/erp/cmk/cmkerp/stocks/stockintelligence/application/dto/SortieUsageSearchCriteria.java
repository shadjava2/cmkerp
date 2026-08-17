package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SortieUsageSearchCriteria(
    LocalDate dateDebut,
    LocalDate dateFin,
    Long pharmacieId,
    Long utilisateurId,
    Long produitId,
    String statut,
    String reference,
    String produitQ,
    String demandeur,
    String raisonSortie,
    BigDecimal quantiteMin,
    BigDecimal quantiteMax,
    BigDecimal montantMin,
    BigDecimal montantMax,
    String scope,
    String preset,
    String anomalyType,
    boolean tousStatuts,
    int limit,
    int offset) {

  public static SortieUsageSearchCriteria defaults() {
    return new SortieUsageSearchCriteria(null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, "CENTRALE", null, null, false, 50, 0);
  }
}
