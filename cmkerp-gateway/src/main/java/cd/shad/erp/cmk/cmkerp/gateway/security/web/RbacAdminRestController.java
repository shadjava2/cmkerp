package cd.shad.erp.cmk.cmkerp.gateway.security.web;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.RBAC_BASE;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.AdminIntegrityResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.RoleApplicationService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints d'intégrité et de réparation du rôle système ADMIN.
 */
@RestController
@RequestMapping(RBAC_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - RBAC", description = "Intégrité et gouvernance des autorisations")
@Validated
public class RbacAdminRestController {

  private final RoleApplicationService roleApplicationService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping("/admin/integrity")
  @RequirePermission({"ADMIN_ROLES_MANAGE", "OUVRIR_ADMINISTRATION", "manage_roles"})
  @Operation(summary = "Vérifie que ADMIN possède toutes les permissions du catalogue")
  public ResponseEntity<AdminIntegrityResponse> adminIntegrity() {
    return ResponseEntity.ok(roleApplicationService.checkAdminIntegrity());
  }

  @PostMapping("/admin/repair-permissions")
  @RequirePermission({"ADMIN_ROLES_MANAGE", "OUVRIR_ADMINISTRATION", "manage_roles"})
  @Operation(summary = "Synchronise toutes les permissions manquantes vers ADMIN")
  public ResponseEntity<AdminIntegrityResponse> repairAdminPermissions(HttpServletRequest request) {
    Long currentUserId = AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    return ResponseEntity.ok(roleApplicationService.repairAdminPermissions(currentUserId));
  }
}
