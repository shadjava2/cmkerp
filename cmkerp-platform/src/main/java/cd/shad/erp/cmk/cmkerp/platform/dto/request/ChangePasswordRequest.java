package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête pour le changement de mot de passe.
 */
@Data
public class ChangePasswordRequest {

  @NotBlank(message = "Le nouveau mot de passe est obligatoire")
  private String newPassword;
}

