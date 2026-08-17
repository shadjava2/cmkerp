package cd.shad.erp.cmk.cmkerp.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour l'authentification.
 */
public record LoginRequest(
    @NotBlank(message = "Le nom d'utilisateur est obligatoire") String username,
    @NotBlank(message = "Le mot de passe est obligatoire") String password,
    Boolean rememberMe
) {
  /**
   * Constructeur avec rememberMe optionnel (par défaut false).
   */
  public LoginRequest {
    if (rememberMe == null) {
      rememberMe = false;
    }
  }
}

