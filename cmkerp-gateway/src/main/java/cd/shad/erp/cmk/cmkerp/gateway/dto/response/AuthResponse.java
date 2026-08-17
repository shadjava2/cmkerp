package cd.shad.erp.cmk.cmkerp.gateway.dto.response;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;

/**
 * DTO de réponse pour l'authentification.
 *
 * <p>
 * Contient les tokens JWT (access et refresh) ainsi que les permissions de l'utilisateur.
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserPermissions user
) {
}

