package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UserPermissionsResponse;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UtilisateurResponse;
import cd.shad.erp.cmk.cmkerp.platform.mapper.UserPermissionsMapper;
import cd.shad.erp.cmk.cmkerp.platform.mapper.UtilisateurMapper;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model.Pharmacie;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository.PharmacieRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.DroitPharmacie;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.DroitPharmacieRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des utilisateurs (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées aux utilisateurs.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UtilisateurQueryService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final DroitPharmacieRepository droitPharmacieRepository;
    private final PharmacieRepository pharmacieRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final UserPermissionsMapper userPermissionsMapper;

    /**
     * Récupère une page d'utilisateurs.
     */
    public Page<UtilisateurResponse> findAll(Pageable pageable) {
        return findAll(pageable, null);
    }

    /**
     * Récupère une page d'utilisateurs avec recherche.
     */
    public Page<UtilisateurResponse> findAll(Pageable pageable, String searchTerm) {
        return findAll(pageable, searchTerm, null);
    }

    /**
     * Récupère une page d'utilisateurs avec recherche et filtre locked.
     */
    public Page<UtilisateurResponse> findAll(Pageable pageable, String searchTerm, Boolean locked) {
        log.debug("Récupération paginée des utilisateurs: page={}, size={}, searchTerm={}, locked={}",
                pageable.getPageNumber(), pageable.getPageSize(), searchTerm, locked);

        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<Utilisateur> utilisateurs;
        long total;

        if (searchTerm != null && !searchTerm.trim().isEmpty() || locked != null) {
            utilisateurs = utilisateurRepository.findAll(offset, limit,
                    searchTerm != null ? searchTerm.trim() : null, locked);
            total = utilisateurRepository.count(
                    searchTerm != null ? searchTerm.trim() : null, locked);
        } else {
            utilisateurs = utilisateurRepository.findAll(offset, limit);
            total = utilisateurRepository.count();
        }

        List<UtilisateurResponse> responses = utilisateurs.stream()
                .map(utilisateur -> {
                    // Charger le rôle pour chaque utilisateur
                    Role role = null;
                    if (utilisateur.getFkRole() != null) {
                        role = roleRepository.findById(utilisateur.getFkRole()).orElse(null);
                    }
                    return utilisateurMapper.toResponse(utilisateur, role);
                })
                .collect(Collectors.toList());

        // 🎯 VALIDATION: S'assurer que le nombre d'éléments retournés ne dépasse pas le total disponible
        // Si l'offset dépasse le total, la liste devrait être vide
        long offsetValue = pageable.getOffset();
        if (offsetValue >= total && !responses.isEmpty()) {
            log.warn("PAGE WARNING: offset={} >= total={} mais responses.size()={}, vidage de la liste",
                    offsetValue, total, responses.size());
            responses = List.of();
        }

        // 🎯 VALIDATION: Sur la dernière page, limiter le nombre d'éléments au reste disponible
        // Exemple: total=201, size=100, page=2 → offset=200, reste=1 → on ne doit retourner que 1 élément
        if (offsetValue < total && !responses.isEmpty()) {
            long remainingElements = total - offsetValue;
            if (responses.size() > remainingElements) {
                log.warn("PAGE WARNING: responses.size()={} > remainingElements={}, limitation à {} éléments",
                        responses.size(), remainingElements, remainingElements);
                responses = responses.subList(0, (int) remainingElements);
            }
        }

        log.debug("PAGE DEBUG: page={}, size={}, offset={}, contentSize={}, total={}, remainingElements={}",
                pageable.getPageNumber(), pageable.getPageSize(), offsetValue, responses.size(), total,
                offsetValue < total ? total - offsetValue : 0);

        return new PageImpl<>(responses, pageable, total);
    }

    /**
     * Récupère un utilisateur par son ID.
     */
    public UtilisateurResponse findById(Long id) {
        log.debug("Récupération de l'utilisateur ID: {}", id);
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", id));

        // Charger le rôle pour remplir roleName
        Role role = null;
        if (utilisateur.getFkRole() != null) {
            role = roleRepository.findById(utilisateur.getFkRole()).orElse(null);
        }

        return utilisateurMapper.toResponse(utilisateur, role);
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur.
     */
    public UtilisateurResponse findByUsername(String username) {
        log.debug("Récupération de l'utilisateur par username: {}", username);
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", username));

        // Charger le rôle pour remplir roleName
        Role role = null;
        if (utilisateur.getFkRole() != null) {
            role = roleRepository.findById(utilisateur.getFkRole()).orElse(null);
        }

        return utilisateurMapper.toResponse(utilisateur, role);
    }

    /**
     * Récupère les permissions d'un utilisateur.
     */
    public UserPermissionsResponse getPermissions(Long userId) {
        log.debug("Récupération des permissions de l'utilisateur ID: {}", userId);

        // Charger l'utilisateur
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", userId));

        // Charger le rôle
        Role role = utilisateur.getFkRole() != null
                ? roleRepository.findById(utilisateur.getFkRole()).orElse(null)
                : null;

        // Charger les permissions du rôle
        List<Permission> permissions = List.of();
        if (role != null) {
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRole(role.getId());
            List<Long> permissionIds = rolePermissions.stream()
                    .map(RolePermission::getFkPermission)
                    .collect(Collectors.toList());

            permissions = permissionIds.stream()
                    .map(permissionRepository::findById)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .collect(Collectors.toList());
        }

        // Charger les droits pharmacies
        List<DroitPharmacie> droitsPharmacies = droitPharmacieRepository.findByUtilisateur(userId);

        // Construire UserPermissions via SecurityMapper
        UserPermissions userPermissions = SecurityMapper.mapToUserPermissions(
                utilisateur, role, permissions, droitsPharmacies);

        return userPermissionsMapper.toResponse(userPermissions);
    }

    /**
     * Récupère les pharmacies auxquelles un utilisateur a accès.
     */
    public List<PharmacieResponse> getPharmacies(Long userId) {
        log.debug("Récupération des pharmacies pour l'utilisateur ID: {}", userId);

        // Vérifier que l'utilisateur existe
        utilisateurRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", userId));

        // Charger les droits pharmacies
        List<DroitPharmacie> droits = droitPharmacieRepository.findByUtilisateur(userId);

        // Récupérer les pharmacies
        return droits.stream()
                .map(droit -> pharmacieRepository.findById(droit.getFkPharmacie()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(this::pharmacieToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une Pharmacie (domain) en PharmacieResponse (DTO).
     */
    private PharmacieResponse pharmacieToResponse(Pharmacie pharmacie) {
        if (pharmacie == null) {
            return null;
        }

        return PharmacieResponse.builder()
                .id(pharmacie.getId())
                .designation(pharmacie.getDesignation())
                .typePharmacie(pharmacie.getTypePharmacie())
                .fkSite(pharmacie.getFkSite())
                .dateCreate(pharmacie.getDateCreate())
                .dateUpdate(pharmacie.getDateUpdate())
                .build();
    }
}
