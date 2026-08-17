package cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité pour les lignes de transfert interne.
 *
 * <p>Une LigneTransfertInterne représente un produit dans un transfert interne avec :
 * <ul>
 *   <li>Référence au transfert interne (fkTransfertInterne)</li>
 *   <li>Référence au stock/produit (fkStock)</li>
 *   <li>Quantité (quantite)</li>
 * </ul>
 */
@Entity
@Table(name = "lignes_transfert_interne", indexes = {
    @Index(name = "index_ligne_transfert_interne_fktransfert", columnList = "fkTransfertInterne,datecreate"),
    @Index(name = "index_ligne_transfert_interne_fkstock", columnList = "fkStock")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneTransfertInterne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkTransfertInterne", nullable = false)
    private Long fkTransfertInterne;

    @Column(name = "fkStock", nullable = false)
    private Long fkStock;

    @Column(name = "fkAlertePeremption")
    private Long fkAlertePeremption;

    @Column(name = "quantite", nullable = false)
    private Float quantite;

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
    public void mettreAJour(Float quantite, Long userId) {
        this.quantite = quantite;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }
}

