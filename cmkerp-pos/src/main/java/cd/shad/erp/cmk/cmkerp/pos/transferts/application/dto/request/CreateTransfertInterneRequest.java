package cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * DTO de requête pour la création d'un transfert interne (module POS).
 */
@Data
public class CreateTransfertInterneRequest {

    @NotNull(message = "La pharmacie source est obligatoire")
    private Long fkPharmacieSource;

    @NotNull(message = "La pharmacie destination est obligatoire")
    private Long fkPharmacieDestination;

    private String commentaire;

    // Les lignes sont optionnelles lors de la création (peuvent être ajoutées après)
    private List<CreateLigneTransfertInterneRequest> lignes;
}

