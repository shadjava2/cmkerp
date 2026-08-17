package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record SortieUsageProduitHistoryDTO(
    long sortieId,
    String reference,
    String dateSortie,
    String pharmacie,
    String demandeur,
    String raisonSortie,
    String statut,
    BigDecimal quantite,
    BigDecimal prixUnitaire,
    BigDecimal montantLigne,
    String encodeur) {}
