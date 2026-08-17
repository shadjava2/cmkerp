package cd.shad.erp.cmk.cmkerp.platform.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour un produit avec stock et péremption.
 * Utilisé dans le module platform pour éviter les dépendances circulaires.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitWithStockDTO {
    private Long id;
    private String codebarre;
    private String nomcommercial;
    private String nomscientifique;

    // Références (désignations)
    private String forme;
    private String dosage;
    private String conditionnement;
    private String categorie;

    // Stock
    private Long stockId;
    private Float stockencours;
    private Boolean isactif;

    // Péremption
    private String peremption;

    // Produit
    private BigDecimal prixachat;
    private Float qtealert;
    private Float qtcritique;
    private Boolean perimable;

    // Métadonnées
    private LocalDateTime dateCreate;

    // Date d'approvisionnement (pour achat risqué)
    private LocalDateTime dateApprov;
}

