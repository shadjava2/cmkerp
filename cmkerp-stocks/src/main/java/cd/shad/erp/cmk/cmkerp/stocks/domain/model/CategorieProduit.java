package cd.shad.erp.cmk.cmkerp.stocks.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité pour le domaine Stocks - Catégorie de produit.
 *
 * <p>Une CategorieProduit représente une catégorie de produit pharmaceutique.
 * C'est une table de référence utilisée par les produits.
 */
@Entity
@Table(name = "categorie_produit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorieProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(length = 20)
    private String abbreviation;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;
}

