package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductMovementEventDTO(
    String type,
    LocalDateTime dateMouvement,
    BigDecimal quantite,
    BigDecimal stockApres,
    String reference,
    String detail,
    String pharmacie) {}
