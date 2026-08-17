package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une ligne de réception de transfert interne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneReceptionTransfertInterneResponse {
    private Long id;
    private Long fkReceptionTransfertInterne;
    private Long fkStock;
    private Long fkAlertePeremption;

    // Informations produit
    private String nomCommercial; // Nom commercial du produit
    private String nomScientifique; // Nom scientifique du produit
    private String forme; // Forme du produit
    private String dosage; // Dosage du produit
    private String conditionnement; // Conditionnement du produit
    private String peremption; // Dates de péremption (GROUP_CONCAT)

    // Quantités
    private Float quantiteTransferee; // Quantité transférée depuis le transfert interne
    private Float quantite; // Quantité à réceptionner (modifiable)
    private Float stockActuel; // Stock actuel du produit à la pharmacie destination

    // Métadonnées
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

