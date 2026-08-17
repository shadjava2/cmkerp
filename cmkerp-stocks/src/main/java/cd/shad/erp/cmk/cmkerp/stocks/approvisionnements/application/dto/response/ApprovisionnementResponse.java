package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un approvisionnement.
 * Inclut les désignations des références (fournisseur, pharmacie, echangeDevise).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovisionnementResponse {
    private Long id;
    private Long fkFournisseur;
    private String fournisseurNom; // Désignation du fournisseur
    private Long fkPharmacie;
    private String pharmacieNom; // Désignation de la pharmacie
    private Long fkEchangeDevise;
    private String echangeDeviseMonnaie; // Monnaie d'échange
    private String statut; // EN_ATTENTE, VALIDEE, ANNULEE, ANNULEE_SANS_MODIFICATION
    private String numbonliv;
    private Short taux;
    private LocalDate datebonliv;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
    private String userCreateNom;
    private String userUpdateNom;
    private Boolean peutEtreAnnule; // Calculé : true si peut être annulé (dans les 24h)
    private Boolean necessiteAutorisationAnnulation;
    private Boolean demandeAnnulationEnCours;
}

