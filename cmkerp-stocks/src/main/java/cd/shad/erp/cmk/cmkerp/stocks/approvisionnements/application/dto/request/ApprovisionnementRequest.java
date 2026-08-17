package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * DTO de requête pour la création et la mise à jour d'un approvisionnement.
 */
@Data
public class ApprovisionnementRequest {

    @NotNull(message = "Le fournisseur est obligatoire")
    private Long fkFournisseur;

    @NotNull(message = "La pharmacie est obligatoire")
    private Long fkPharmacie;

    private Long fkEchangeDevise;

    private String numbonliv;

    /** Taux de change (accepte les décimales, arrondi à l'enregistrement). */
    private Integer taux;

    private LocalDate datebonliv;
}
