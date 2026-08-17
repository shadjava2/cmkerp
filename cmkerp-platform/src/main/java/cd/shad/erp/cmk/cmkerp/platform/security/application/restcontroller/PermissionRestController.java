package cd.shad.erp.cmk.cmkerp.platform.security.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.PERMISSIONS_BASE;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PermissionResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.PermissionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour la gestion des permissions (lecture seule).
 * Utilise le Query Service de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(PERMISSIONS_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - Utilisateurs", description = "Gestion des utilisateurs, rôles, permissions")
@Validated
public class PermissionRestController {

  private final PermissionQueryService permissionQueryService;

  /**
   * Récupère toutes les permissions.
   */
  @GetMapping
  @Operation(summary = "Liste toutes les permissions")
  public ResponseEntity<List<PermissionResponse>> findAll() {
    List<PermissionResponse> permissions = permissionQueryService.findAll();
    return ResponseEntity.ok(permissions);
  }

  /**
   * Récupère une permission par son ID.
   */
  @GetMapping("/{id}")
  @Operation(summary = "Récupère une permission par son ID")
  public ResponseEntity<PermissionResponse> findById(@PathVariable Long id) {
    PermissionResponse permission = permissionQueryService.findById(id);
    return ResponseEntity.ok(permission);
  }
}

