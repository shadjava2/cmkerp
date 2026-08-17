package cd.shad.erp.cmk.cmkerp.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête pour l'initialisation du mot de passe lors de la première connexion.
 *
 * <p>Utilisé uniquement lorsque initPassword = true (première connexion).
 * Ne nécessite pas le mot de passe actuel.
 */
@Data
public class InitPasswordRequest {

  /**
   * Nouveau mot de passe (obligatoire).
   */
  @NotBlank(message = "Le nouveau mot de passe est obligatoire")
  private String newPassword;
}








