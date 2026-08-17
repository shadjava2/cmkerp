package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de requête pour remplacer un produit dans une ligne de transfert.
 */
@Data
public class ReplaceLigneTransfertRequest {

    @NotNull(message = "Le stock est obligatoire")
    private Long fkStock;

    private Double quantite;
}

