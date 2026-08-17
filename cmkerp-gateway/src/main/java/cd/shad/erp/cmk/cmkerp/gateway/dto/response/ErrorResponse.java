package cd.shad.erp.cmk.cmkerp.gateway.dto.response;

/**
 * DTO de réponse pour les erreurs.
 */
public record ErrorResponse(
    String code,
    String message
) {
}

