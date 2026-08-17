package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de requête pour la création et la mise à jour d'un utilisateur.
 */
@Data
public class UtilisateurRequest {

  @NotBlank(message = "Le nom d'utilisateur est obligatoire")
  private String username;

  private String nom;
  private String postnom;
  private String prenom;
  private String sexe; // "M" pour Masculin, "F" pour Féminin
  private String specialite;
  private String carted;

  @NotNull(message = "Le rôle est obligatoire")
  private Long fkRole;

  private Boolean locked;
  private Boolean initPassword;
  private Boolean isLoginCard;
}

