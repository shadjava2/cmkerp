package cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité pour les lignes d'inventaire.
 *
 * <p>Une LigneInventaire représente un produit dans un inventaire avec :
 * <ul>
 *   <li>Référence à l'inventaire (fkInventaire)</li>
 *   <li>Référence au stock/produit (fkStock)</li>
 *   <li>Quantité théorique (quantite_theorique) - depuis le stock</li>
 *   <li>Quantité physique (quantite_physique) - saisie manuelle</li>
 *   <li>Écart (ecart) - colonne virtuelle calculée (quantite_physique - quantite_theorique)</li>
 *   <li>Commentaire (commentaire) - optionnel</li>
 * </ul>
 *
 * <p>Note: Les lignes sont créées automatiquement par une procédure stockée
 * lors de la création de l'inventaire. On ne peut que les mettre à jour.
 */
@Entity
@Table(name = "lignes_inventaire", indexes = {
    @Index(name = "index_ligne_inventaire_fkinventaire", columnList = "fkInventaire,datecreate"),
    @Index(name = "index_ligne_inventaire_fkstock", columnList = "fkStock"),
    @Index(name = "index_optim_ligne_invent", columnList = "fkInventaire,fkStock")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneInventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkInventaire", nullable = false)
    private Long fkInventaire;

    @Column(name = "fkStock", nullable = false)
    private Long fkStock;

    @Column(name = "quantite_theorique", nullable = false)
    private Float quantite_theorique;

    @Column(name = "quantite_physique", nullable = false)
    private Float quantite_physique;

    // ecart est une colonne virtuelle (GENERATED ALWAYS AS) - pas besoin de la mapper

    @Column(name = "commentaire", length = 255)
    private String commentaire;

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
    public void mettreAJour(Float quantite_physique, String commentaire, Long userId) {
        if (quantite_physique != null) {
            this.quantite_physique = quantite_physique;
        }
        this.commentaire = commentaire;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }
}

