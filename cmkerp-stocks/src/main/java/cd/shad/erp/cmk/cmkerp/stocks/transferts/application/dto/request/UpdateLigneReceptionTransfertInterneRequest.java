package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour mettre à jour une ligne de réception de transfert interne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLigneReceptionTransfertInterneRequest {
    @Positive(message = "La quantité doit être positive")
    private Float quantite; // Quantité à réceptionner
}

