package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.time.LocalDateTime;

public record OperationListItemDTO(
    Long id,
    String type,
    String statut,
    String reference,
    String pharmacieSource,
    String pharmacieDestination,
    String fournisseur,
    int lignesCount,
    LocalDateTime dateCreate) {}
