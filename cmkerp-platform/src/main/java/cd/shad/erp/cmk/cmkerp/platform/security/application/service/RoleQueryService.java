package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PermissionResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.RoleResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des rôles (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées aux rôles.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoleQueryService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Récupère tous les rôles.
     * Résultats mis en cache pour améliorer les performances.
     */
    @Cacheable(value = "roles", key = "'all'")
    public List<RoleResponse> findAll() {
        log.debug("Récupération de tous les rôles");
        return roleRepository.findAll().stream()
                .map(this::roleToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un rôle par son ID.
     * Résultat mis en cache avec la clé basée sur l'ID.
     */
    @Cacheable(value = "roles", key = "#id")
    public RoleResponse findById(Long id) {
        log.debug("Récupération du rôle ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Role", id));

        return roleToResponse(role);
    }

    /**
     * Récupère les permissions d'un rôle.
     */
    public List<PermissionResponse> getPermissions(Long roleId) {
        log.debug("Récupération des permissions du rôle ID: {}", roleId);

        // Vérifier que le rôle existe
        roleRepository.findById(roleId)
                .orElseThrow(() -> NotFoundException.entity("Role", roleId));

        // Charger les permissions via le repository
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRole(roleId);

        // Convertir en PermissionResponse
        return rolePermissions.stream()
                .map(rp -> permissionRepository.findById(rp.getFkPermission()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(this::permissionToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit un Role (domain) en RoleResponse (DTO).
     */
    private RoleResponse roleToResponse(Role role) {
        if (role == null) {
            return null;
        }

        return RoleResponse.builder()
                .id(role.getId())
                .nom(role.getNom())
                .description(role.getDescription())
                .dateCreate(role.getDateCreate())
                .dateUpdate(role.getDateUpdate())
                .build();
    }

    /**
     * Convertit une Permission (domain) en PermissionResponse (DTO).
     */
    private PermissionResponse permissionToResponse(Permission permission) {
        if (permission == null) {
            return null;
        }

        return PermissionResponse.builder()
                .id(permission.getId())
                .nom(permission.getNom())
                .description(permission.getDescription())
                .build();
    }
}

