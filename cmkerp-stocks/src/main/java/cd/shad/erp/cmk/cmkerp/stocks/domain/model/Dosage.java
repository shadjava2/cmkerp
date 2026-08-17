package cd.shad.erp.cmk.cmkerp.stocks.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité pour le domaine Stocks - Dosage de produit.
 *
 * <p>Un Dosage représente un dosage pharmaceutique (250 MG, 500 MG, etc.).
 * C'est une table de référence utilisée par les produits.
 */
@Entity
@Table(name = "dosages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dosage {

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

