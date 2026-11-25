package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.RolePermissionsUpdateRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.RoleRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.AdminIntegrityResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PermissionResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.RolePermissionAuditEntry;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.RolePermissionsUpdateResponse;
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
import cd.shad.erp.cmk.cmkerp.platform.events.AuditEvent;
import cd.shad.erp.cmk.cmkerp.platform.events.AuditEventRepository;
import cd.shad.erp.cmk.cmkerp.platform.events.IDomainEventPublisher;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionAuthorizationPort;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.SystemRoleConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final PermissionAuthorizationPort permissionAuthorization;
    private final IDomainEventPublisher eventPublisher;
    private final AuditEventRepository auditEventRepository;

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
    @CacheEvict(value = "roles", allEntries = true)
    public RoleResponse update(Long id, RoleRequest request, Long currentUserId) {
        log.debug("Mise à jour du rôle ID: {}", id);

        roleDomainService.validerRoleNonSysteme(id);

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
    @CacheEvict(value = "roles", allEntries = true)
    public void deleteById(Long id) {
        log.debug("Suppression du rôle ID: {}", id);

        // Vérifier que le rôle existe
        roleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Role", id));

        // Validation métier via Domain Service (inclut protection ADMIN)
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
     * Remplace transactionnellement les permissions d'un rôle (diff add/remove uniquement).
     */
    @CacheEvict(value = {"roles", "permissions"}, allEntries = true)
    public RolePermissionsUpdateResponse updatePermissions(
            Long roleId, RolePermissionsUpdateRequest request, Long currentUserId) {
        roleDomainService.validerRoleNonSysteme(roleId);
        roleRepository.findById(roleId)
                .orElseThrow(() -> NotFoundException.entity("Role", roleId));

        Set<Long> desired = request.getPermissionIds() == null
                ? Set.of()
                : request.getPermissionIds().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));

        for (Long permissionId : desired) {
            permissionRepository.findById(permissionId)
                    .orElseThrow(() -> NotFoundException.entity("Permission", permissionId));
        }

        List<RolePermission> existing = rolePermissionRepository.findByRole(roleId);
        Set<Long> current = existing.stream()
                .map(RolePermission::getFkPermission)
                .collect(Collectors.toCollection(HashSet::new));

        List<Long> toAdd = desired.stream().filter(id -> !current.contains(id)).toList();
        List<Long> toRemove = current.stream().filter(id -> !desired.contains(id)).toList();

        LocalDateTime now = LocalDateTime.now();
        for (Long permissionId : toAdd) {
            rolePermissionRepository.save(RolePermission.builder()
                    .fkRole(roleId)
                    .fkPermission(permissionId)
                    .dateCreate(now)
                    .userCreatedId(currentUserId)
                    .build());
        }
        for (Long permissionId : toRemove) {
            rolePermissionRepository.deleteByRoleAndPermission(roleId, permissionId);
        }

        permissionAuthorization.invalidateRolePermissions(roleId);

        String details = String.format(
                "{\"previous\":%s,\"added\":%s,\"removed\":%s,\"newTotal\":%d,\"result\":\"SUCCESS\"}",
                current, toAdd, toRemove, desired.size());
        try {
            eventPublisher.publishAuditEvent(new AuditEvent(
                    currentUserId,
                    null,
                    "ROLE_PERMISSIONS_UPDATE",
                    "ROLE",
                    roleId,
                    details));
        } catch (Exception ex) {
            log.warn("RBAC audit non publié pour rôle {} — {}", roleId, ex.getMessage());
        }

        log.info("RBAC : rôle {} permissions +{} -{} par user {}",
                roleId, toAdd.size(), toRemove.size(), currentUserId);

        return RolePermissionsUpdateResponse.builder()
                .roleId(roleId)
                .addedPermissionIds(toAdd)
                .removedPermissionIds(toRemove)
                .totalPermissions(desired.size())
                .permissionsVersion(System.currentTimeMillis())
                .build();
    }

    /**
     * Historique administratif des modifications de permissions d'un rôle.
     */
    @Transactional(readOnly = true)
    public List<RolePermissionAuditEntry> listPermissionAudit(Long roleId, int limit) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> NotFoundException.entity("Role", roleId));
        return auditEventRepository.findRolePermissionChanges(roleId, limit);
    }

    /**
     * Contrôle d'intégrité : permissions du catalogue absentes du rôle ADMIN.
     */
    @Transactional(readOnly = true)
    public AdminIntegrityResponse checkAdminIntegrity() {
        List<Permission> catalog = permissionRepository.findAll();
        List<RolePermission> adminLinks =
                rolePermissionRepository.findByRole(SystemRoleConstants.ADMIN_ROLE_ID);
        Set<Long> adminPermIds = adminLinks.stream()
                .map(RolePermission::getFkPermission)
                .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        for (Permission p : catalog) {
            if (!adminPermIds.contains(p.getId())) {
                missing.add(p.getNom());
            }
        }

        return AdminIntegrityResponse.builder()
                .adminRoleId(SystemRoleConstants.ADMIN_ROLE_ID)
                .catalogSize(catalog.size())
                .adminPermissionCount(adminPermIds.size())
                .missingPermissionCodes(missing)
                .healthy(missing.isEmpty())
                .build();
    }

    /**
     * Synchronise toutes les permissions manquantes vers ADMIN (réparation).
     */
    @CacheEvict(value = {"roles", "permissions"}, allEntries = true)
    public AdminIntegrityResponse repairAdminPermissions(Long currentUserId) {
        List<Permission> catalog = permissionRepository.findAll();
        List<RolePermission> adminLinks =
                rolePermissionRepository.findByRole(SystemRoleConstants.ADMIN_ROLE_ID);
        Set<Long> adminPermIds = adminLinks.stream()
                .map(RolePermission::getFkPermission)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        int added = 0;
        for (Permission p : catalog) {
            if (!adminPermIds.contains(p.getId())) {
                rolePermissionRepository.save(RolePermission.builder()
                        .fkRole(SystemRoleConstants.ADMIN_ROLE_ID)
                        .fkPermission(p.getId())
                        .dateCreate(now)
                        .userCreatedId(currentUserId)
                        .build());
                added++;
            }
        }
        permissionAuthorization.invalidateRolePermissions(SystemRoleConstants.ADMIN_ROLE_ID);
        log.info("RBAC : réparation ADMIN — {} permission(s) ajoutée(s) par user {}",
                added, currentUserId);
        return checkAdminIntegrity();
    }

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
                .systemProtected(SystemRoleConstants.isAdminRoleId(role.getId())
                        || SystemRoleConstants.isAdminRoleName(role.getNom()))
                .build();
    }

    private PermissionResponse permissionToResponse(Permission permission) {
        if (permission == null) {
            return null;
        }

        Long parent = permission.getParent();
        return PermissionResponse.builder()
                .id(permission.getId())
                .nom(permission.getNom())
                .description(permission.getDescription())
                .parent(parent == null ? 0L : parent)
                .dateCreate(permission.getDateCreate())
                .dateUpdate(permission.getDateUpdate())
                .build();
    }
}

