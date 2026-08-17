package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une ligne de transfert de stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneTransfertStockResponse {
    private Long id;
    private Long fkTransfertStock;
    private Long fkStock;
    private String stockNomCommercial;
    private String stockNomScientifique;
    private String stockForme;
    private String stockDosage;
    private String stockConditionnement;
    private Double quantiteDemandee;
    private Double quantite;
    private Double quantiteEnStock; // Quantité disponible dans le stock
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

