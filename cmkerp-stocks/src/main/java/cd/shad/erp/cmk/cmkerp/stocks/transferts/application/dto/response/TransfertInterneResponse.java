package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un transfert interne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransfertInterneResponse {
    private Long id;
    private Long fkPharmacieSource;
    private String pharmacieSourceNom;
    private Long fkPharmacieDestination;
    private String pharmacieDestinationNom;
    private String statut; // EN ATTENTE, TRANSFEREE, ANNULEE, RECEPTIONNEE
    private String commentaire;
    private Boolean perime;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
    private Boolean peutEtreAnnule; // Calculé : true si peut être annulé
}

