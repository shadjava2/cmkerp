package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour créer une ligne de réception de transfert interne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLigneReceptionTransfertInterneRequest {
    @NotNull(message = "Le stock est obligatoire")
    private Long fkStock;

    private Long fkAlertePeremption;

    private Float quantiteDemandee; // Depuis le transfert interne

    private Float quantiteTransferee; // Depuis le transfert interne

    @NotNull(message = "La quantité à réceptionner est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private Float quantite; // Quantité à réceptionner
}

