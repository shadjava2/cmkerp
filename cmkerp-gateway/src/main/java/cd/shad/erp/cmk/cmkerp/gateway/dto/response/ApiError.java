package cd.shad.erp.cmk.cmkerp.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO standardisé pour les réponses d'erreur API.
 *
 * <p>Format cohérent pour toutes les erreurs HTTP, incluant :
 * <ul>
 *   <li>Timestamp de l'erreur</li>
 *   <li>Status HTTP et libellé</li>
 *   <li>Message lisible</li>
 *   <li>Chemin de la requête</li>
 *   <li>Correlation ID pour le traçage</li>
 *   <li>Détails de validation (optionnel)</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<ValidationError> validationErrors
) {

    /**
     * Détail d'erreur de validation pour un champ spécifique.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record ValidationError(
            String field,
            String message,
            Object rejectedValue
    ) {
    }
}

