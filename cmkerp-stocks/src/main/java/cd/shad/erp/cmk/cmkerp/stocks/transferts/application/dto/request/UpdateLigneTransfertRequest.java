package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import lombok.Data;

/**
 * DTO de requête pour la mise à jour d'une ligne de transfert.
 */
@Data
public class UpdateLigneTransfertRequest {

    private Long fkStock;

    private Double quantite;
}

