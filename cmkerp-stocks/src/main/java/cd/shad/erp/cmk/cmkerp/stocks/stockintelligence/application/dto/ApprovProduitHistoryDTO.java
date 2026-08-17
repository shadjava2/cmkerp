package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record ApprovProduitHistoryDTO(
    long approvisionnementId,
    String reference,
    String dateApprovisionnement,
    String fournisseur,
    String pharmacie,
    String statut,
    BigDecimal quantite,
    BigDecimal prixUnitaire,
    BigDecimal montantLigne,
    String encodeur) {}
