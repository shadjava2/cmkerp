package cd.shad.erp.cmk.cmkerp.stocks.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Modèle de domaine pour un fournisseur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fournisseur {

    private Long id;
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

