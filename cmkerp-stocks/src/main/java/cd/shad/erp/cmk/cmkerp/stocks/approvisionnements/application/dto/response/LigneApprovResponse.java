package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une ligne d'approvisionnement.
 * Inclut les infos produit/stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneApprovResponse {
    private Long id;
    private Long fkApprov;
    private Long fkStock;
    private Long produitId;
    private String produitNom; // Nom du produit (depuis stock)
    private Float qt;
    /** Quantité actuellement en stock pour ce fkStock (stock_produits.qte). */
    private Float stockActuel;
    private BigDecimal prixachat;
    private BigDecimal prixachattotal;
    private BigDecimal totalfournisseur;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

