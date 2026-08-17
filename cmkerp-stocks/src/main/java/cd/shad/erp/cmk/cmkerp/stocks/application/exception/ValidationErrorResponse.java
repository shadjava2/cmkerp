package cd.shad.erp.cmk.cmkerp.stocks.application.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Réponse d'erreur de validation standardisée.
 *
 * Contient les détails des erreurs de validation par champ.
 */
@Data
@Builder
public class ValidationErrorResponse {

  /**
   * Timestamp de l'erreur.
   */
  private LocalDateTime timestamp;

  /**
   * Code de statut HTTP.
   */
  private Integer status;

  /**
   * Type d'erreur.
   */
  private String error;

  /**
   * Message d'erreur général.
   */
  private String message;

  /**
   * Chemin de la requête qui a causé l'erreur.
   */
  private String path;

  /**
   * Détails des erreurs de validation par champ. Clé: nom du champ, Valeur: message d'erreur
   */
  private Map<String, String> validationErrors;
}


























