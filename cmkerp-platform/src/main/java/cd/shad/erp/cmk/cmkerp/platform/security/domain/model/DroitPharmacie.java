package cd.shad.erp.cmk.cmkerp.platform.security.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité représentant le droit d'accès d'un utilisateur à une pharmacie.
 *
 * <p>Cette entité fait partie du domaine Security mais établit une relation
 * avec le domaine Pharmacie. Elle représente les droits d'accès granulaire
 * qu'un utilisateur peut avoir sur une pharmacie spécifique.
 */
@Entity
@Table(name = "droits_pharmacies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroitPharmacie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fkUtilisateur")
    private Long fkUtilisateur;

    @Column(name = "fkPharmacie")
    private Long fkPharmacie;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    /** Non persisté : la table {@code droits_pharmacies} n'a pas cette colonne. */
    @Transient
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    /** Non persisté : la table {@code droits_pharmacies} n'a pas cette colonne. */
    @Transient
    private Long userUpdatedId;
}

