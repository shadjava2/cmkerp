package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;

public record OperationLineDTO(
    int lineNumber,
    String produitLabel,
    String nomScientifique,
    Long stockId,
    BigDecimal quantite,
    BigDecimal quantiteDemandee,
    BigDecimal quantiteTransferee,
    BigDecimal prixAchat,
    String forme,
    String dosage) {}
