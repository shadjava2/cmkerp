package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record TransfertListItemDTO(
    long id,
    String reference,
    Long requisitionId,
    String statut,
    Long pharmacieSourceId,
    String pharmacieSource,
    Long pharmacieDestinationId,
    String pharmacieDestination,
    String dateTransfert,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    String encodeur,
    String dateCreate,
    String dateUpdate) {}
