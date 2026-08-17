package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité pour les lignes d'approvisionnement.
 *
 * <p>Une LigneApprov représente un produit dans un bon de livraison avec :
 * <ul>
 *   <li>Référence à l'approvisionnement (fkApprov)</li>
 *   <li>Référence au stock/produit (fkStock)</li>
 *   <li>Quantité (qt)</li>
 *   <li>Prix d'achat unitaire (prixachat)</li>
 *   <li>Prix d'achat total (prixachattotal) - calculé : qt * prixachat</li>
 *   <li>Total fournisseur (totalfournisseur) - en devise du fournisseur</li>
 * </ul>
 */
@Entity
@Table(name = "lignes_approv", indexes = {
    @Index(name = "index-approv-optimis", columnList = "fkApprov,fkStock,datecreate"),
    @Index(name = "index_approv_index", columnList = "fkStock")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneApprov {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkApprov", nullable = false)
    private Long fkApprov;

    @Column(name = "fkStock")
    private Long fkStock;

    @Column(name = "qt")
    private Float qt;

    @Column(name = "prixachat", precision = 10, scale = 2)
    private BigDecimal prixachat;

    @Column(name = "prixachattotal", precision = 10, scale = 2)
    private BigDecimal prixachattotal;

    @Column(name = "totalfournisseur", precision = 10, scale = 2)
    private BigDecimal totalfournisseur;

    @Column(name = "datecreate", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreate = LocalDateTime.now();

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;

    /**
     * Calcule le prix d'achat total (qt * prixachat).
     */
    public void calculerPrixAchatTotal() {
        if (this.qt != null && this.prixachat != null) {
            this.prixachattotal = this.prixachat.multiply(BigDecimal.valueOf(this.qt));
        }
    }

    /**
     * Met à jour la ligne avec les nouvelles valeurs.
     */
    public void mettreAJour(Float qt, BigDecimal prixachat, Long userId) {
        this.qt = qt;
        this.prixachat = prixachat;
        calculerPrixAchatTotal();
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }
}

