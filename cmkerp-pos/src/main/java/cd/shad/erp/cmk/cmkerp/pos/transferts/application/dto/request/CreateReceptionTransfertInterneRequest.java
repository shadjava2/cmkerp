package cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO de requête pour créer une réception de transfert interne (module POS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReceptionTransfertInterneRequest {
    @NotNull(message = "Le transfert interne est obligatoire")
    private Long fkTransfertInterne;

    private List<CreateLigneReceptionTransfertInterneRequest> lignes;
}

