package cd.shad.erp.cmk.cmkerp.gateway.exception;

/**
 * Exception levée lorsque le refresh token est expiré ou invalide.
 *
 * <p>Cette exception est utilisée pour distinguer les erreurs de refresh token
 * des autres erreurs métier, permettant une gestion spécifique côté handler
 * et une réponse HTTP 401 avec le message "SESSION_EXPIRED".
 */
public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException() {
        super("REFRESH_TOKEN_EXPIRED");
    }

    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}

