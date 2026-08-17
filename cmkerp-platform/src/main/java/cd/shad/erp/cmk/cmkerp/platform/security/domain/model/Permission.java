package cd.shad.erp.cmk.cmkerp.platform.security.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité du domaine Security - Permission.
 *
 * <p>Une Permission représente un droit d'accès dans le système.
 * Elle peut être associée à un ou plusieurs rôles via RolePermission.
 */
@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String description;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;

    /**
     * Valide que le nom de la permission respecte les règles métier.
     *
     * @param nom le nom à valider
     * @throws IllegalArgumentException si le nom est invalide
     */
    public static void validerNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la permission ne peut pas être vide");
        }
        if (nom.length() > 100) {
            throw new IllegalArgumentException("Le nom de la permission ne peut pas dépasser 100 caractères");
        }
    }
}

