package cd.shad.erp.cmk.cmkerp.stocks.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité pour le domaine Stocks - Conditionnement de produit.
 *
 * <p>Un Conditionnement représente un type de conditionnement (BTE, FLAC, AMP, etc.).
 * C'est une table de référence utilisée par les produits.
 */
@Entity
@Table(name = "conditionnements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conditionnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String designation;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;
}

