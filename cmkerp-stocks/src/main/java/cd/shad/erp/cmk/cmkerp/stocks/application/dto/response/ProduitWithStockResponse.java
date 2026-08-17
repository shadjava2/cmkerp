package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un produit avec stock et péremption.
 * Utilisé pour la liste des produits avec droits, stock en cours et dates de péremption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitWithStockResponse {
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
    private Long stockId; // ID du stock (st.id) - utilisé pour les opérations sur les dates de péremption
    private Float stockencours;
    private Boolean isactif; // operationnel

    // Péremption
    private String peremption; // GROUP_CONCAT des dates de péremption

    // Produit
    private BigDecimal prixachat;
    private Float qtealert;
    private Float qtcritique;
    private Boolean perimable;

    // Métadonnées pour pagination cursor-based
    private LocalDateTime dateCreate; // Clé de tri pour cursor-based pagination

    // Date d'approvisionnement (pour achat risqué)
    private LocalDateTime dateApprov;
}

