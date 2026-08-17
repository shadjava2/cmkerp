package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un transfert de stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransfertStockResponse {
    private Long id;
    private Long fkRequisition;
    private Long requisitionNumero;
    private String statut; // EN ATTENTE, ANNULEE, TRANSFEREE, RECEPTIONNEE
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
    private String pharmacieDemandeurNom;
}

