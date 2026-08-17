package cd.shad.erp.cmk.cmkerp.platform.security.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité d'association entre Role et Permission.
 *
 * <p>Cette entité fait partie de l'agrégat Role et représente
 * l'association entre un rôle et une permission.
 */
@Entity
@Table(name = "roles_permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fkRole")
    private Long fkRole;

    @Column(name = "fkPermission")
    private Long fkPermission;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdatedid")
    private Long userUpdatedId;
}

