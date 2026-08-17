package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record TransfertLineDetailDTO(
    int lineNumber,
    Long stockId,
    Long produitId,
    String produit,
    String nomScientifique,
    String forme,
    String dosage,
    String categorie,
    BigDecimal quantiteDemandee,
    BigDecimal quantite,
    String pharmacieSource) {}
