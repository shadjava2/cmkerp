package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une ligne d'inventaire.
 * Inclut toutes les informations du produit (nom commercial, nom scientifique, forme, dosage, conditionnement, péremption)
 * et l'écart (colonne virtuelle calculée).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneInventaireResponse {
    private Long id;
    private Long fkInventaire;
    private Long fkStock;

    // Informations du produit (récupérées via JOINs)
    private String produitNom; // Nom commercial (pour compatibilité)
    private String nomcommercial;
    private String nomscientifique;
    private String forme; // Désignation de la forme
    private String dosage; // Désignation du dosage
    private String conditionnement; // Désignation du conditionnement
    private String peremption; // GROUP_CONCAT des dates de péremption
    private String codebarre;

    private Boolean operationnel;

    private Float quantite_theorique;
    private Float quantite_physique;
    private Float ecart; // Colonne virtuelle calculée (quantite_physique - quantite_theorique)
    private String commentaire;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

