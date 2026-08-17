package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de requête pour la création d'un transfert (traiter une requête).
 */
@Data
public class CreateTransfertRequest {

    @NotNull(message = "La requête est obligatoire")
    private Long fkRequisition;
}

