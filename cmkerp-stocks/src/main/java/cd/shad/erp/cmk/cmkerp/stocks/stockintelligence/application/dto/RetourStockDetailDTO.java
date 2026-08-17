package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record RetourStockDetailDTO(
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
    String encodeur,
    String encodeurUsername,
    String dateCreate,
    String dateUpdate,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    BigDecimal quantiteRecue,
    String produitPlusRetourne,
    List<RetourStockLineDetailDTO> lignes,
    RetourStockQualityFlagsDTO qualite) {}
