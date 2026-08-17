package cd.shad.erp.cmk.cmkerp.stocks.application.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.CmkBaseException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestionnaire global des exceptions pour l'application Stocks.
 *
 * Centralise la gestion de toutes les exceptions et retourne des réponses HTTP cohérentes.
 *
 * Architecture: Pattern Exception Handler centralisé pour une gestion uniforme des erreurs.
 */
@ControllerAdvice(basePackages = "cd.shad.erp.cmk.cmkerp.stocks")
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Gère les exceptions métier (BusinessException).
   *
   * @param ex l'exception métier
   * @param request la requête HTTP
   * @return réponse HTTP 400 (Bad Request) avec le message d'erreur
   */
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex,
      WebRequest request) {
    log.warn("Business exception: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value()).error("Business Error").message(ex.getMessage())
        .path(getPath(request)).build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /**
   * Gère les exceptions de ressource non trouvée (NotFoundException).
   *
   * @param ex l'exception de ressource non trouvée
   * @param request la requête HTTP
   * @return réponse HTTP 404 (Not Found) avec le message d'erreur
   */
  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex,
      WebRequest request) {
    log.warn("Resource not found: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value())
            .error("Not Found").message(ex.getMessage()).path(getPath(request)).build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /**
   * Gère les exceptions techniques (TechnicalException).
   *
   * @param ex l'exception technique
   * @param request la requête HTTP
   * @return réponse HTTP 500 (Internal Server Error) avec le message d'erreur
   */
  @ExceptionHandler(TechnicalException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponse> handleTechnicalException(TechnicalException ex,
      WebRequest request) {
    log.error("Technical exception: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error("Technical Error")
        .message(ex.getMessage()).path(getPath(request)).build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /**
   * Gère les exceptions de base (CmkBaseException).
   *
   * @param ex l'exception de base
   * @param request la requête HTTP
   * @return réponse HTTP appropriée selon le type d'exception
   */
  @ExceptionHandler(CmkBaseException.class)
  public ResponseEntity<ErrorResponse> handleCmkBaseException(CmkBaseException ex,
      WebRequest request) {
    log.error("CMK base exception: {}", ex.getMessage(), ex);

    // Par défaut, traiter comme une erreur serveur
    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error("Application Error")
        .message(ex.getMessage()).path(getPath(request)).build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /**
   * Gère les erreurs de validation (MethodArgumentNotValidException).
   *
   * @param ex l'exception de validation
   * @param request la requête HTTP
   * @return réponse HTTP 400 (Bad Request) avec les détails de validation
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ValidationErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {
    log.warn("Validation error: {}", ex.getMessage());

    Map<String, String> errors = new HashMap<>();

    // Erreurs de champ
    ex.getBindingResult().getFieldErrors().forEach(error -> {
      errors.put(error.getField(), error.getDefaultMessage());
    });

    // Erreurs globales
    ex.getBindingResult().getGlobalErrors().forEach(error -> {
      errors.put(error.getObjectName(), error.getDefaultMessage());
    });

    ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
        .timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Error").message("Erreurs de validation").path(getPath(request))
        .validationErrors(errors).build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /**
   * Gère les violations d'intégrité (doublons, contraintes FK, etc.).
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
      WebRequest request) {
    log.warn("Data integrity violation: {}", ex.getMessage());
    String message = "Contrainte base de données violée";
    if (ex.getMessage() != null && ex.getMessage().contains("index_appro_unique")) {
      message =
          "Contrainte legacy index_appro_unique : exécutez sql/drop_index_appro_unique.sql pour autoriser plusieurs bons par fournisseur";
    } else if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
      message = "Cet enregistrement existe déjà";
    }

    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value()).error("Data Integrity Error").message(message)
        .path(getPath(request)).build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /**
   * Gère les exceptions IllegalArgumentException.
   *
   * @param ex l'exception
   * @param request la requête HTTP
   * @return réponse HTTP 400 (Bad Request)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
      WebRequest request) {
    log.warn("Illegal argument: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value()).error("Invalid Argument").message(ex.getMessage())
        .path(getPath(request)).build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /**
   * Gère toutes les autres exceptions non gérées.
   *
   * @param ex l'exception
   * @param request la requête HTTP
   * @return réponse HTTP 500 (Internal Server Error)
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
    log.error("Unexpected exception: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error("Internal Server Error")
        .message("Une erreur inattendue s'est produite").path(getPath(request)).build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /**
   * Extrait le chemin de la requête.
   */
  private String getPath(WebRequest request) {
    return request.getDescription(false).replace("uri=", "");
  }
}


