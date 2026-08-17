package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un inventaire.
 * Inclut les désignations des références (pharmacie).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventaireResponse {
    private Long id;
    private Long fkPharmacie;
    private String pharmacieNom; // Désignation de la pharmacie
    private LocalDateTime date_debut;
    private LocalDateTime date_fin; // Mise à jour automatique lors de la clôture
    private String statut; // EN COURS, TERMINE, ANNULE
    private String commentaire;
    private String typeinventaire; // PHYSIQUE, AJUSTEMENT, MENSUEL, PERIME
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

