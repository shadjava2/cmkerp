package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une ligne de transfert interne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneTransfertInterneResponse {
    private Long id;
    private Long fkTransfertInterne;
    private Long fkStock;
    private Long fkAlertePeremption;
    private String produitNom; // Désignation du produit
    private Float quantite;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

