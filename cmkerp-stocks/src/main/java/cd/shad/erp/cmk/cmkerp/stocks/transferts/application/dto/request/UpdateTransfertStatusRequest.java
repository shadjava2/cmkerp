package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la mise à jour du statut d'un transfert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransfertStatusRequest {

    @NotBlank(message = "Le statut est obligatoire")
    @Pattern(regexp = "EN ATTENTE|ANNULEE|TRANSFEREE|RECEPTIONNEE",
             message = "Le statut doit être: EN ATTENTE, ANNULEE, TRANSFEREE ou RECEPTIONNEE")
    private String statut;

    private String commentaire;
}

