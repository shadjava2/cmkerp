package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record SortieUsageLineDetailDTO(
    int lineNumber,
    Long stockId,
    Long produitId,
    String produit,
    String nomScientifique,
    String forme,
    String dosage,
    String categorie,
    BigDecimal quantite,
    BigDecimal prixUnitaire,
    BigDecimal montantLigne) {}
