package cd.shad.erp.cmk.cmkerp.gateway.security.web;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.ROLES_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.RoleRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PermissionResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.RoleResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.RoleApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des rôles (domaine Security).
 *
 * <p>Ce controller expose l'API REST pour les rôles et délègue toute la logique
 * métier au RoleApplicationService.
 *
 * <p>Migration depuis platform.restcontroller vers gateway.security.web
 * conformément à l'architecture DDD/hexagonale.
 */
@RestController
@RequestMapping(ROLES_BASE)
@RequiredArgsConstructor
@Tag(name = "Platform - Utilisateurs", description = "Gestion des utilisateurs, rôles, permissions")
@Validated
public class RoleRestController {

    private final RoleApplicationService roleApplicationService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère tous les rôles.
     */
    @GetMapping
    @Operation(summary = "Liste tous les rôles")
    public ResponseEntity<List<RoleResponse>> findAll() {
        List<RoleResponse> roles = roleApplicationService.findAll();
        return ResponseEntity.ok(roles);
    }

    /**
     * Récupère un rôle par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère un rôle par son ID")
    public ResponseEntity<RoleResponse> findById(@PathVariable Long id) {
        RoleResponse role = roleApplicationService.findById(id);
        return ResponseEntity.ok(role);
    }

    /**
     * Crée un nouveau rôle.
     */
    @PostMapping
    @Operation(summary = "Crée un nouveau rôle")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        RoleResponse created = roleApplicationService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour un rôle existant.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un rôle")
    public ResponseEntity<RoleResponse> update(@PathVariable Long id,
            @Valid @RequestBody RoleRequest request, HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        RoleResponse updated = roleApplicationService.update(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime un rôle.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un rôle")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        roleApplicationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les permissions d'un rôle.
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "Récupère les permissions d'un rôle")
    public ResponseEntity<List<PermissionResponse>> getPermissions(@PathVariable Long id) {
        List<PermissionResponse> permissions = roleApplicationService.getPermissions(id);
        return ResponseEntity.ok(permissions);
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





