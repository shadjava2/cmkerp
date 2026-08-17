package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PermissionResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des permissions (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées aux permissions.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PermissionQueryService {

    private final PermissionRepository permissionRepository;

    /**
     * Récupère toutes les permissions.
     * Résultats mis en cache pour améliorer les performances.
     */
    @Cacheable(value = "permissions", key = "'all'")
    public List<PermissionResponse> findAll() {
        log.debug("Récupération de toutes les permissions");
        return permissionRepository.findAll().stream()
                .map(this::permissionToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une permission par son ID.
     * Résultat mis en cache avec la clé basée sur l'ID.
     */
    @Cacheable(value = "permissions", key = "#id")
    public PermissionResponse findById(Long id) {
        log.debug("Récupération de la permission ID: {}", id);
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Permission", id));

        return permissionToResponse(permission);
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
