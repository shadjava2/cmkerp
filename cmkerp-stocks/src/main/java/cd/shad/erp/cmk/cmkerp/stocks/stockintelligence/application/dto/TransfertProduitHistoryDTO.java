package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record TransfertProduitHistoryDTO(
    long transfertId,
    String reference,
    Long requisitionId,
    String referenceRequisition,
    String dateDemande,
    String dateTransfert,
    String dateReception,
    String pharmacieSource,
    String pharmacieDestination,
    String statut,
    BigDecimal quantiteDemandee,
    BigDecimal quantite,
    String demandeur,
    String encodeur,
    String receptionneur) {}
