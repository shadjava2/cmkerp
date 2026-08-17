package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record RetourStockProduitHistoryDTO(
    long retourId,
    String reference,
    String dateRetour,
    String pharmacieSource,
    String pharmacieDestination,
    String statut,
    String statutReception,
    Boolean perime,
    BigDecimal quantiteTransferee,
    BigDecimal quantiteRecue,
    String encodeur) {}
