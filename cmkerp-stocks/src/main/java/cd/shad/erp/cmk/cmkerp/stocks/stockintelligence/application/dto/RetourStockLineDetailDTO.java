package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record RetourStockLineDetailDTO(
    int lineNumber,
    Long stockId,
    Long produitId,
    String produit,
    String nomScientifique,
    String forme,
    String dosage,
    String categorie,
    BigDecimal quantiteTransferee,
    BigDecimal quantiteDemandee,
    BigDecimal quantiteRecue,
    String pharmacieSource) {}
