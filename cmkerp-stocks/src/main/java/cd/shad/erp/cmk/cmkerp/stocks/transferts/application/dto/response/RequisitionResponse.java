package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une requête.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionResponse {
    private Long id;
    private Long fkPharmacie;
    private String pharmacieNom;
    private Long fkPharmacieStock;
    private String pharmacieStockNom;
    private String statut; // EN_ATTENTE, VALIDEE, ANNULEE
    private Integer niveau;
    private String commentaire;
    private Boolean urgent;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
    private Boolean peutEtreTraite; // true si aucun transfert initié ou transfert annulé
}

