package cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * DTO de requête pour la mise à jour d'une ligne de transfert interne (module POS).
 */
@Data
public class UpdateLigneTransfertInterneRequest {

    private Long fkStock;

    private Long fkAlertePeremption;

    @Positive(message = "La quantité doit être positive")
    private Float quantite;
}

