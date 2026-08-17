package cd.shad.erp.cmk.cmkerp.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête pour rafraîchir les tokens JWT.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Le refresh token est obligatoire") String refreshToken) {
}
