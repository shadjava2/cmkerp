package cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité pour les lignes de réception de transfert interne.
 *
 * <p>Une LigneReceptionTransfertInterne représente un produit dans une réception de transfert interne avec :
 * <ul>
 *   <li>Référence à la réception (fkReceptionTransfertInterne)</li>
 *   <li>Référence au stock/produit (fkStock)</li>
 *   <li>Quantité demandée (quantiteDemandee) - depuis le transfert interne</li>
 *   <li>Quantité transférée (quantiteTransferee) - depuis le transfert interne</li>
 *   <li>Quantité à réceptionner (quantite) - modifiable par l'utilisateur</li>
 * </ul>
 */
@Entity
@Table(name = "lignes_reception_transfert_interne", indexes = {
    @Index(name = "index_ligne_reception_fkreception", columnList = "fkReceptionStock,datecreate"),
    @Index(name = "index_ligne_reception_fkstock", columnList = "fkStock")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneReceptionTransfertInterne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkReceptionStock", nullable = false)
    private Long fkReceptionTransfertInterne;

    @Column(name = "fkStock", nullable = false)
    private Long fkStock;

    @Column(name = "fkAlertePeremption")
    private Long fkAlertePeremption;

    @Column(name = "quantiteDemandee")
    private Float quantiteDemandee;

    @Column(name = "quantiteTransferee")
    private Float quantiteTransferee;

    @Column(name = "quantite")
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

