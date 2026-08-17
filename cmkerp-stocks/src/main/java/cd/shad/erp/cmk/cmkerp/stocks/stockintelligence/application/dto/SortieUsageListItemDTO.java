package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record SortieUsageListItemDTO(
    long id,
    String reference,
    String statut,
    Long pharmacieId,
    String pharmacie,
    String demandeur,
    String raisonSortie,
    String dateSortie,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String encodeur,
    String dateCreate,
    String dateUpdate) {}
