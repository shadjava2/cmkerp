package cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité pour les échanges de devise.
 */
@Entity
@Table(name = "echange_devise")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EchangeDevise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "monnaieprincipale", length = 20)
    private String monnaieprincipale;

    @Column(name = "tauxechange")
    private Float tauxechange;

    @Column(name = "monnaieechange", length = 20)
    private String monnaieechange;

    @Column(name = "symbole", length = 10)
    private String symbole;

    @Column(name = "datecreate", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreate = LocalDateTime.now();

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;
}



