package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une réception de transfert interne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionTransfertInterneResponse {
    private Long id;
    private Long fkTransfertInterne;
    private String transfertInterneNumero; // ID du transfert interne formaté
    private Long fkPharmacieSource;
    private String pharmacieSourceNom;
    private Long fkPharmacieDestination;
    private String pharmacieDestinationNom;
    private String statut; // EN ATTENTE, RECEPTIONNEE, ANNULEE
    private Boolean perime;
    private String statutTransfert; // "transfert périmé" si perime=true, sinon "transfert stock"
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
    private Boolean peutEtreAnnule; // Calculé : true si peut être annulé
}

