package cd.shad.erp.cmk.cmkerp.stocks.application.exception;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Réponse d'erreur standardisée pour l'API.
 *
 * Suit le format RFC 7807 (Problem Details for HTTP APIs) pour une gestion cohérente des erreurs.
 */
@Data
@Builder
public class ErrorResponse {

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
   * Message d'erreur.
   */
  private String message;

  /**
   * Chemin de la requête qui a causé l'erreur.
   */
  private String path;
}


























