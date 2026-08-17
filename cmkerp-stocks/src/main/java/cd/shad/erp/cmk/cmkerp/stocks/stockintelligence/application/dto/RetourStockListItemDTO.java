package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record RetourStockListItemDTO(
    long id,
    String reference,
    Long receptionId,
    String statut,
    String statutReception,
    Boolean perime,
    Long pharmacieSourceId,
    String pharmacieSource,
    Long pharmacieDestinationId,
    String pharmacieDestination,
    String commentaire,
    String dateRetour,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    BigDecimal quantiteRecue,
    String encodeur,
    String dateCreate,
    String dateUpdate) {}
