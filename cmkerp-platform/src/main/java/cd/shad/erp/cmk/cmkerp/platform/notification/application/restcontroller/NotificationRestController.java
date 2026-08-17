package cd.shad.erp.cmk.cmkerp.platform.notification.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.NOTIFICATIONS_BASE;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.NotificationRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.UpdateNotificationStatusRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.NotificationResponse;
import cd.shad.erp.cmk.cmkerp.platform.notification.application.service.NotificationCommandService;
import cd.shad.erp.cmk.cmkerp.platform.notification.application.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour la gestion des notifications. Utilise les Query/Command Services de la
 * nouvelle architecture DDD.
 */
@RestController
@RequestMapping(NOTIFICATIONS_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - Notifications",
    description = "Envoi, états & tracking des notifications internes")
@Validated
public class NotificationRestController {

  private final NotificationQueryService notificationQueryService;
  private final NotificationCommandService notificationCommandService;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * Récupère les notifications avec filtres optionnels.
   */
  @GetMapping
  @Operation(summary = "Liste les notifications avec filtres optionnels")
  public ResponseEntity<List<NotificationResponse>> findAll(
      @RequestParam(required = false) Long fkUtilisateur,
      @RequestParam(required = false) String statut) {
    List<NotificationResponse> notifications =
        notificationQueryService.findAll(fkUtilisateur, statut);
    return ResponseEntity.ok(notifications);
  }

  /**
   * Récupère une notification par son ID.
   */
  @GetMapping("/{id}")
  @Operation(summary = "Récupère une notification par son ID")
  public ResponseEntity<NotificationResponse> findById(@PathVariable Long id) {
    NotificationResponse notification = notificationQueryService.findById(id);
    return ResponseEntity.ok(notification);
  }

  /**
   * Crée une nouvelle notification.
   */
  @PostMapping
  @Operation(summary = "Crée une nouvelle notification")
  public ResponseEntity<NotificationResponse> create(
      @Valid @RequestBody NotificationRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    NotificationResponse created = notificationCommandService.create(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Met à jour le statut d'une notification.
   */
  @PatchMapping("/{id}/status")
  @Operation(summary = "Met à jour le statut d'une notification")
  public ResponseEntity<NotificationResponse> updateStatus(@PathVariable Long id,
      @Valid @RequestBody UpdateNotificationStatusRequest request, HttpServletRequest httpRequest) {
    Long currentUserId = getCurrentUserId(httpRequest);
    NotificationResponse updated =
        notificationCommandService.updateStatus(id, request, currentUserId);
    return ResponseEntity.ok(updated);
  }

  /**
   * Extrait l'ID de l'utilisateur connecté depuis le JWT token dans l'en-tête Authorization.
   *
   * @param request la requête HTTP
   * @return l'ID de l'utilisateur connecté
   * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si le token est
   *         invalide
   */
  private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }
}





