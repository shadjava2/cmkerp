package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * DTO de requête pour la création et la mise à jour d'un fournisseur.
 */
@Data
public class FournisseurRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String adresse;

    private String telephone;

    @Email(message = "L'email doit être valide")
    private String email;
}

