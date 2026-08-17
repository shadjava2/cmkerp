package cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité pour les lignes de vente.
 *
 * <p>Une LigneVente représente un produit dans une sortie pour usage avec :
 * <ul>
 *   <li>Référence à la vente (fkVente)</li>
 *   <li>Référence au stock/produit (fkStock)</li>
 *   <li>Quantité (qt)</li>
 *   <li>Prix de vente unitaire (prixventes)</li>
 *   <li>Hors convention (horsconvention) - 0 ou 1</li>
 * </ul>
 */
@Entity
@Table(name = "lignes_vente", indexes = {
    @Index(name = "index_ligne_vente_fkvente", columnList = "fkVente,datecreate"),
    @Index(name = "index_ligne_vente_fkstock", columnList = "fkStock")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkVente", nullable = false)
    private Long fkVente;

    @Column(name = "fkStock")
    private Long fkStock;

    @Column(name = "qt")
    private Float qt;

    @Column(name = "prixventes", precision = 10, scale = 2)
    private BigDecimal prixventes;

    @Column(name = "horsconvention", length = 1)
    @Builder.Default
    private Integer horsconvention = 0;

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
     * Met à jour la ligne avec les nouvelles valeurs.
     */
    public void mettreAJour(Float qt, BigDecimal prixventes, Integer horsconvention, Long userId) {
        this.qt = qt;
        this.prixventes = prixventes;
        this.horsconvention = horsconvention != null ? horsconvention : 0;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }
}

