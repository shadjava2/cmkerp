package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.RoleRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PermissionResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.RoleResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.service.RoleDomainService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service pour la gestion des rôles.
 *
 * <p>Ce service orchestre les use cases liés aux rôles :
 * <ul>
 *   <li>Utilise les Domain Services pour la logique métier pure</li>
 *   <li>Utilise les méthodes métier des agrégats</li>
 *   <li>Gère les transactions</li>
 *   <li>Convertit entre DTOs et domain models</li>
 *   <li>Gère le cache</li>
 * </ul>
 *
 * <p>Ce service ne contient PAS de logique métier pure (celle-ci est dans RoleDomainService
 * et dans l'agrégat Role lui-même).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleDomainService roleDomainService;

    /**
     * Récupère tous les rôles.
     * Résultats mis en cache pour améliorer les performances.
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#id")
    public RoleResponse findById(Long id) {
        log.debug("Récupération du rôle ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Role", id));

        return roleToResponse(role);
    }

    /**
     * Crée un nouveau rôle.
     *
     * <p>Utilise le Domain Service pour valider l'unicité du nom,
     * puis utilise les méthodes métier de l'agrégat Role.
     * Invalide le cache "all" après création.
     */
    @CacheEvict(value = "roles", key = "'all'")
    public RoleResponse create(RoleRequest request, Long currentUserId) {
        log.debug("Création d'un nouveau rôle: {}", request.getNom());

        // Validation métier via Domain Service
        roleDomainService.validerNomUnique(request.getNom(), null);

        // Créer l'agrégat Role
        Role role = Role.builder()
                .nom(request.getNom())
                .description(request.getDescription())
                .userCreatedId(currentUserId)
                .dateCreate(LocalDateTime.now())
                .build();

        // Utiliser les méthodes métier de l'agrégat si nécessaire
        if (request.getNom() != null) {
            role.changerNom(request.getNom()); // Valide et met à jour
        }
        if (request.getDescription() != null) {
            role.changerDescription(request.getDescription());
        }

        // Sauvegarder via le repository
        int rows = roleRepository.save(role);
        if (rows == 0) {
            throw new BusinessException("Échec de la création du rôle");
        }

        // Récupérer le rôle créé avec son ID
        Role created = roleRepository.findByNom(request.getNom())
                .orElseThrow(() -> new BusinessException("Erreur lors de la récupération du rôle créé"));

        log.info("Rôle créé avec succès: ID={}, nom={}", created.getId(), created.getNom());
        return roleToResponse(created);
    }

    /**
     * Met à jour un rôle existant.
     *
     * <p>Utilise le Domain Service pour valider l'unicité si le nom change,
     * puis utilise les méthodes métier de l'agrégat Role.
     * Invalide les caches du rôle modifié et de la liste complète.
     */
    @CacheEvict(value = "roles", key = "#id + 'all'")
    public RoleResponse update(Long id, RoleRequest request, Long currentUserId) {
        log.debug("Mise à jour du rôle ID: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Role", id));

        // Validation métier via Domain Service si le nom change
        if (request.getNom() != null && !request.getNom().equals(role.getNom())) {
            roleDomainService.validerNomUnique(request.getNom(), id);
            role.changerNom(request.getNom()); // Utilise la méthode métier de l'agrégat
        }

        if (request.getDescription() != null) {
            role.changerDescription(request.getDescription()); // Utilise la méthode métier
        }

        role.setUserUpdatedId(currentUserId);
        role.setDateUpdate(LocalDateTime.now());

        // Sauvegarder via le repository
        int rows = roleRepository.update(role);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour du rôle");
        }

        log.info("Rôle mis à jour avec succès: ID={}", role.getId());
        return roleToResponse(role);
    }

    /**
     * Supprime un rôle.
     *
     * <p>Utilise le Domain Service pour valider que le rôle peut être supprimé.
     * Invalide les caches du rôle supprimé et de la liste complète.
     */
    @CacheEvict(value = "roles", key = "#id + 'all'")
    public void deleteById(Long id) {
        log.debug("Suppression du rôle ID: {}", id);

        // Vérifier que le rôle existe
        roleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Role", id));

        // Validation métier via Domain Service
        roleDomainService.validerSuppressionRole(id);

        // Supprimer via le repository
        int rows = roleRepository.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("Échec de la suppression du rôle");
        }

        log.info("Rôle supprimé avec succès: ID={}", id);
    }

    /**
     * Récupère les permissions d'un rôle.
     */
    @Transactional(readOnly = true)
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
     *
     * <p>Note: Cette méthode utilise PermissionMapper qui attend l'ancien modèle.
     * Une fois que PermissionMapper sera mis à jour, on pourra l'utiliser directement.
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

