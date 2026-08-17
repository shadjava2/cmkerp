package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour le statut d'une requisition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequisitionStatusRequest {

    @NotBlank(message = "Le statut est obligatoire")
    private String statut;

    private String commentaire; // Commentaire optionnel pour expliquer le rejet
}

