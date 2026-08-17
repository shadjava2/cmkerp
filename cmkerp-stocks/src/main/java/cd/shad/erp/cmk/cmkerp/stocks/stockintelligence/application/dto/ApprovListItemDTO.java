package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record ApprovListItemDTO(
    long id,
    String reference,
    String statut,
    Long fournisseurId,
    String fournisseur,
    Long pharmacieId,
    String pharmacie,
    String dateApprovisionnement,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String encodeur,
    String dateCreate,
    String dateUpdate) {}
