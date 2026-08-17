package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransfertDetailDTO(
    long id,
    String reference,
    Long requisitionId,
    String statut,
    Long pharmacieSourceId,
    String pharmacieSource,
    Long pharmacieDestinationId,
    String pharmacieDestination,
    String dateTransfert,
    String encodeur,
    String encodeurUsername,
    String dateCreate,
    String dateUpdate,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    String produitPlusTransfere,
    List<TransfertLineDetailDTO> lignes,
    TransfertQualityFlagsDTO qualite) {}
