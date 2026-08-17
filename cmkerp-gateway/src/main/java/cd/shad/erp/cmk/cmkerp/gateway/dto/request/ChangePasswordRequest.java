package cd.shad.erp.cmk.cmkerp.gateway.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête pour le changement de mot de passe depuis l'endpoint d'authentification.
 *
 * <p>Utilisé pour permettre à un utilisateur connecté de changer son propre mot de passe.
 * L'ancien mot de passe est optionnel pour permettre le changement initial (initPassword).
 */
@Data
public class ChangePasswordRequest {

  /**
   * Ancien mot de passe (optionnel pour le changement initial).
   * Accepte "currentPassword" depuis le frontend et le mappe vers "oldPassword".
   * Si vide ou null, le changement est considéré comme un changement initial.
   */
  @JsonProperty("currentPassword")
  private String oldPassword;

  /**
   * Nouveau mot de passe (obligatoire).
   */
  @NotBlank(message = "Le nouveau mot de passe est obligatoire")
  private String newPassword;
}




