package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * DTO de requête pour la création d'une ligne de transfert interne.
 */
@Data
public class CreateLigneTransfertInterneRequest {

    @NotNull(message = "Le stock est obligatoire")
    private Long fkStock;

    private Long fkAlertePeremption;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private Float quantite;
}

