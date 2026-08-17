package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une vente.
 * Inclut les désignations des références (pharmacie).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenteResponse {
    private Long id;
    private Long fkEntreprise;
    private Long fkPatient;
    private Long fkPharmacie;
    private String pharmacieNom; // Désignation de la pharmacie
    private String statut; // EN_ATTENTE, VALIDEE, ANNULEE
    private Short taux;
    private String typepaiement;
    private String raisonsortie;
    private String demandeur;
    private String fkPatientMediline;
    private String fkFicheMedicale;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
    private Boolean peutEtreAnnule; // Calculé : true si peut être annulé (dans les 24h)
}

