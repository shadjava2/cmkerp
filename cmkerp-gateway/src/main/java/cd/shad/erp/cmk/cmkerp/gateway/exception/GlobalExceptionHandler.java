package cd.shad.erp.cmk.cmkerp.gateway.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import cd.shad.erp.cmk.cmkerp.gateway.dto.response.ApiError;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.CmkBaseException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions pour l'API REST.
 *
 * <p>Centralise la gestion des erreurs et retourne des réponses HTTP cohérentes
 * avec un format d'erreur standardisé (ApiError).
 *
 * <p>Exceptions gérées :
 * <ul>
 *   <li>MethodArgumentNotValidException : HTTP 400 (validation)</li>
 *   <li>HttpMessageNotReadableException : HTTP 400 (JSON invalide)</li>
 *   <li>DataAccessException : HTTP 503 (problèmes DB)</li>
 *   <li>BusinessException : HTTP 400 (Bad Request)</li>
 *   <li>NotFoundException : HTTP 404 (Not Found)</li>
 *   <li>BadCredentialsException : HTTP 401 (Unauthorized)</li>
 *   <li>IllegalArgumentException : HTTP 400 (Bad Request)</li>
 *   <li>Exception : HTTP 500 (fallback)</li>
 * </ul>
 */
@RestControllerAdvice
@Hidden
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gère les erreurs de validation (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        List<ApiError.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapToValidationError)
                .collect(Collectors.toList());

        ApiError error = buildApiError(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Les données fournies sont invalides",
                getRequestPath(request),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les erreurs de contrainte de validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        List<ApiError.ValidationError> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(this::mapToValidationError)
                .collect(Collectors.toList());

        ApiError error = buildApiError(
                HttpStatus.BAD_REQUEST,
                "Constraint Violation",
                "Les contraintes de validation ne sont pas respectées",
                getRequestPath(request),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les erreurs de lecture de message HTTP (JSON invalide, etc.).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("HTTP message not readable: {}", ex.getMessage());

        ApiError error = buildApiError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Le format de la requête est invalide",
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les exceptions d'accès aux données (problèmes DB).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        String path = getRequestPath(request);
        String correlationId = MDC.get("correlationId");
        log.error("Data access exception -> correlationId: {}, path: {}, exception: {}, message: {}",
                correlationId, path, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        String raw = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        if (raw != null && raw.toLowerCase().contains("stock insuffisant")) {
            ApiError business = buildApiError(
                    HttpStatus.BAD_REQUEST,
                    "Business Error",
                    "Impossible d'annuler ce bon : le stock a déjà été consommé. "
                            + "Régularisez le stock (inventaire / retour) ou conservez le bon validé.",
                    path,
                    null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(business);
        }

        String errorMessage = extractReadableErrorMessage(ex);
        ApiError error = buildApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database Error",
                errorMessage,
                path,
                null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Extrait un message d'erreur lisible depuis une exception SQL.
     * Évite d'inclure des requêtes SQL complètes.
     */
    private String extractReadableErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "Une erreur de base de données s'est produite";
        }

        // Si le message contient une requête SQL, essayer d'extraire seulement la partie erreur
        if (message.contains("PreparedStatementCallback") || message.contains("SQLException")) {
            // Extraire la partie après "SQLException" ou avant "for SQL"
            int sqlIndex = message.indexOf("for SQL");
            if (sqlIndex > 0) {
                // Prendre la partie avant "for SQL" qui contient généralement le message d'erreur
                String beforeSql = message.substring(0, sqlIndex).trim();
                // Chercher le dernier ";" ou ":" pour isoler le message d'erreur
                int lastColon = beforeSql.lastIndexOf(":");
                if (lastColon > 0 && lastColon < beforeSql.length() - 1) {
                    String extracted = beforeSql.substring(lastColon + 1).trim();
                    // Nettoyer le message (enlever les retours à la ligne multiples)
                    extracted = extracted.replaceAll("\\s+", " ").trim();
                    if (extracted.length() > 0) {
                        return extracted.length() > 500 ? extracted.substring(0, 500) + "..." : extracted;
                    }
                }
                return beforeSql.length() > 500 ? beforeSql.substring(0, 500) + "..." : beforeSql;
            }

            // Si on trouve "SQLException", extraire le message après
            int sqlExceptionIndex = message.indexOf("SQLException");
            if (sqlExceptionIndex > 0) {
                String afterException = message.substring(sqlExceptionIndex + "SQLException".length()).trim();
                // Prendre jusqu'à 200 caractères ou jusqu'à "for SQL"
                int forSqlIndex = afterException.indexOf("for SQL");
                if (forSqlIndex > 0) {
                    return afterException.substring(0, Math.min(forSqlIndex, 200)).trim();
                }
                return afterException.length() > 200 ? afterException.substring(0, 200) + "..." : afterException;
            }
        }

        // Limiter la longueur du message pour éviter les messages trop longs
        if (message.length() > 500) {
            return message.substring(0, 500) + "...";
        }

        return message;
    }

    /**
     * Gère les exceptions métier (BusinessException).
     *
     * <p>Si le message contient des mots-clés d'authentification, retourne HTTP 401 (Unauthorized)
     * au lieu de HTTP 400 (Bad Request).
     *
     * <p>Détection spécifique pour les refresh tokens expirés/invalides.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex, WebRequest request) {
        String message = ex.getMessage();
        if (message == null) {
            message = "Erreur métier inconnue";
        }

        String messageLower = message.toLowerCase();
        boolean isAuthError = messageLower.contains("authentification") ||
            messageLower.contains("token") ||
            messageLower.contains("refresh") ||
            messageLower.contains("connecter") ||
            messageLower.contains("login") ||
            messageLower.contains("unauthorized") ||
            messageLower.contains("session") ||
            messageLower.contains("expiré") ||
            messageLower.contains("expire") ||
            messageLower.contains("invalide");

        HttpStatus status = isAuthError ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        String errorTitle = isAuthError ? "Authentication Error" : "Business Error";

        // Logger le message complet sans troncature pour les erreurs d'authentification
        if (isAuthError) {
            log.warn("Business exception [{} {}]: {}", status.value(), status.getReasonPhrase(), message);
        } else {
            log.warn("Business exception [{}]: {}", status, message);
        }

        ApiError error = buildApiError(
                status,
                errorTitle,
                message,
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Gère les exceptions de ressource non trouvée (NotFoundException).
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(
            NotFoundException ex, WebRequest request) {
        log.warn("Not found exception: {}", ex.getMessage());

        ApiError error = buildApiError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gère les exceptions de refresh token expiré (RefreshTokenExpiredException).
     *
     * <p>Retourne HTTP 401 avec le message "SESSION_EXPIRED" pour permettre au frontend
     * de détecter une session expirée et rediriger vers la page de login sans retry.
     */
    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiError> handleRefreshTokenExpired(
            RefreshTokenExpiredException ex,
            WebRequest request) {
        // Pas de log WARN: c'est un état normal (session expirée)
        // Logger uniquement en DEBUG pour le debugging
        if (log.isDebugEnabled()) {
            log.debug("Refresh token expiré: {}", getRequestPath(request));
        }

        ApiError error = buildApiError(
                HttpStatus.UNAUTHORIZED,
                "Authentication Error",
                "SESSION_EXPIRED",
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Gère les exceptions d'authentification (BadCredentialsException).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());

        ApiError error = buildApiError(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Identifiants incorrects",
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Gère les exceptions d'autorisation (AuthorizationDeniedException).
     *
     * <p>Cette exception est lancée par Spring Security quand un utilisateur authentifié
     * n'a pas les permissions nécessaires pour accéder à une ressource.
     *
     * <p>Retourne HTTP 403 (Forbidden) si l'utilisateur est authentifié mais n'a pas les droits,
     * ou HTTP 401 (Unauthorized) si le token est expiré ou invalide.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex, WebRequest request) {
        String path = getRequestPath(request);
        String correlationId = MDC.get("correlationId");

        // Vérifier si c'est un problème d'authentification (token expiré/invalide) ou d'autorisation
        boolean isAuthProblem = ex.getMessage() != null && (
            ex.getMessage().toLowerCase().contains("access denied") ||
            ex.getMessage().toLowerCase().contains("authentication") ||
            ex.getMessage().toLowerCase().contains("token")
        );

        // Si l'utilisateur n'est pas authentifié, c'est 401, sinon 403
        HttpStatus status = isAuthProblem ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        String message = isAuthProblem
            ? "Session expirée ou token invalide. Veuillez vous reconnecter."
            : "Vous n'avez pas les permissions nécessaires pour accéder à cette ressource.";

        // Logger uniquement en WARN pour éviter les logs trop verbeux
        log.warn("Authorization denied -> correlationId: {}, path: {}, status: {}, message: {}",
                correlationId, path, status, message);

        ApiError error = buildApiError(
                status,
                status == HttpStatus.UNAUTHORIZED ? "Unauthorized" : "Forbidden",
                message,
                path,
                null
        );

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Gère les exceptions d'accès refusé (AccessDeniedException).
     *
     * <p>Retourne HTTP 403 (Forbidden) pour les utilisateurs authentifiés sans permissions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        String path = getRequestPath(request);
        String correlationId = MDC.get("correlationId");

        log.warn("Access denied -> correlationId: {}, path: {}, message: {}",
                correlationId, path, ex.getMessage());

        ApiError error = buildApiError(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Vous n'avez pas les permissions nécessaires pour accéder à cette ressource.",
                path,
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gère les exceptions de base CMK (CmkBaseException).
     */
    @ExceptionHandler(CmkBaseException.class)
    public ResponseEntity<ApiError> handleCmkBaseException(
            CmkBaseException ex, WebRequest request) {
        log.warn("CMK exception: {}", ex.getMessage());

        ApiError error = buildApiError(
                HttpStatus.BAD_REQUEST,
                "CMK Error",
                ex.getMessage(),
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les exceptions d'argument invalide (IllegalArgumentException).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ApiError error = buildApiError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                getRequestPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Route API introuvable (ex. contrôleur non déployé).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ApiError error = buildApiError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "Ressource introuvable",
                getRequestPath(request),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gère toutes les autres exceptions non gérées (fallback).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, WebRequest request) {
        // Log structuré avec correlationId, path, exception type
        String path = getRequestPath(request);
        String correlationId = MDC.get("correlationId");

        // Optimisation : Logger seulement les informations essentielles pour éviter les stack traces trop longues
        // Ne logger la stack trace complète qu'en développement ou pour les erreurs critiques
        String exceptionName = ex.getClass().getSimpleName();
        String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : "No message";

        // Limiter la longueur du message pour éviter les logs trop longs
        String truncatedMessage = exceptionMessage.length() > 200
            ? exceptionMessage.substring(0, 200) + "..."
            : exceptionMessage;

        // Logger seulement les 3 premières lignes de la stack trace pour optimisation
        String stackTrace = "";
        if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
            StringBuilder sb = new StringBuilder();
            int maxLines = 3;
            for (int i = 0; i < Math.min(maxLines, ex.getStackTrace().length); i++) {
                sb.append(ex.getStackTrace()[i].toString()).append("\n");
            }
            stackTrace = sb.toString();
        }

        log.error("Unexpected exception -> correlationId: {}, path: {}, exception: {}, message: {}\nStack trace (first 3 lines):\n{}",
                correlationId, path, exceptionName, truncatedMessage, stackTrace);

        ApiError error = buildApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Une erreur interne s'est produite",
                path,
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==========================================
    // Méthodes utilitaires
    // ==========================================

    private ApiError buildApiError(
            HttpStatus status,
            String error,
            String message,
            String path,
            List<ApiError.ValidationError> validationErrors) {
        return new ApiError(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path,
                MDC.get("correlationId"),
                validationErrors
        );
    }

    private ApiError.ValidationError mapToValidationError(FieldError fieldError) {
        return new ApiError.ValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }

    private ApiError.ValidationError mapToValidationError(ConstraintViolation<?> violation) {
        return new ApiError.ValidationError(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()
        );
    }

    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            return httpRequest.getRequestURI();
        }
        return "unknown";
    }
}
