package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record SortieUsageDetailDTO(
    long id,
    String reference,
    String statut,
    Long pharmacieId,
    String pharmacie,
    String demandeur,
    String raisonSortie,
    String dateSortie,
    String encodeur,
    String encodeurUsername,
    String dateCreate,
    String dateUpdate,
    int lignesCount,
    int produitsDistinct,
    BigDecimal quantiteTotale,
    BigDecimal montantTotal,
    String produitPlusSorti,
    List<SortieUsageLineDetailDTO> lignes,
    SortieUsageQualityFlagsDTO qualite) {}
