package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionAuthorizationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Moteur RBAC centralisé — source de vérité = tables {@code roles_permissions} / {@code permissions}.
 *
 * <p>Les claims JWT ne constituent pas une autorité définitive : chaque vérification
 * lit le cache local (invalidé à chaque mutation de rôle) puis la base.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionAuthorizationService implements PermissionAuthorizationPort {

  private final UtilisateurRepository utilisateurRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PermissionRepository permissionRepository;

  /** Cache userId → permissions effectives. */
  private final ConcurrentHashMap<Long, Set<String>> userPermissionCache = new ConcurrentHashMap<>();

  /** Cache userId → version monotoone (incrémentée à chaque invalidation). */
  private final ConcurrentHashMap<Long, Long> userPermissionVersion = new ConcurrentHashMap<>();

  /** Cache roleId → userIds connus (pour invalidation en cascade). */
  private final ConcurrentHashMap<Long, Set<Long>> roleUsersIndex = new ConcurrentHashMap<>();

  @Override
  @Transactional(readOnly = true)
  public boolean hasPermission(Long userId, String permissionCode) {
    if (userId == null || permissionCode == null || permissionCode.isBlank()) {
      return false;
    }
    return getEffectivePermissions(userId).contains(permissionCode);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAnyPermission(Long userId, Collection<String> permissionCodes) {
    if (userId == null || permissionCodes == null || permissionCodes.isEmpty()) {
      return false;
    }
    Set<String> effective = getEffectivePermissions(userId);
    return permissionCodes.stream().filter(Objects::nonNull).anyMatch(effective::contains);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAllPermissions(Long userId, Collection<String> permissionCodes) {
    if (userId == null || permissionCodes == null || permissionCodes.isEmpty()) {
      return false;
    }
    Set<String> effective = getEffectivePermissions(userId);
    return permissionCodes.stream().filter(Objects::nonNull).allMatch(effective::contains);
  }

  @Override
  @Transactional(readOnly = true)
  public void requirePermission(Long userId, String permissionCode) {
    if (!hasPermission(userId, permissionCode)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Vous ne disposez pas des autorisations nécessaires pour effectuer cette opération.");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getEffectivePermissions(Long userId) {
    if (userId == null) {
      return Set.of();
    }
    Set<String> cached = userPermissionCache.get(userId);
    if (cached != null) {
      return cached;
    }
    return loadAndCache(userId);
  }

  @Override
  public long getPermissionsVersion(Long userId) {
    if (userId == null) {
      return 0L;
    }
    return userPermissionVersion.getOrDefault(userId, 0L);
  }

  @Override
  public void invalidateUserPermissions(Long userId) {
    if (userId == null) {
      return;
    }
    userPermissionCache.remove(userId);
    userPermissionVersion.merge(userId, 1L, Long::sum);
    log.info("RBAC : invalidation permissions userId={} version={}", userId,
        userPermissionVersion.get(userId));
  }

  @Override
  public void invalidateRolePermissions(Long roleId) {
    if (roleId == null) {
      return;
    }
    Set<Long> known = new HashSet<>(roleUsersIndex.getOrDefault(roleId, Set.of()));
    // Balayage paginé des utilisateurs du rôle (source de vérité)
    try {
      int offset = 0;
      final int pageSize = 200;
      while (true) {
        var page = utilisateurRepository.findAll(offset, pageSize);
        if (page.isEmpty()) {
          break;
        }
        for (Utilisateur u : page) {
          if (roleId.equals(u.getFkRole()) && u.getId() != null) {
            known.add(u.getId());
          }
        }
        if (page.size() < pageSize) {
          break;
        }
        offset += pageSize;
      }
    } catch (Exception ex) {
      log.warn("RBAC : invalidation rôle {} partielle — {}", roleId, ex.getMessage());
    }
    for (Long userId : known) {
      invalidateUserPermissions(userId);
    }
    log.info("RBAC : invalidation permissions roleId={} users={}", roleId, known.size());
  }

  private Set<String> loadAndCache(Long userId) {
    Utilisateur user = utilisateurRepository.findById(userId).orElse(null);
    if (user == null || Boolean.TRUE.equals(user.getLocked()) || user.getFkRole() == null) {
      Set<String> empty = Set.of();
      userPermissionCache.put(userId, empty);
      return empty;
    }

    roleUsersIndex
        .computeIfAbsent(user.getFkRole(), id -> ConcurrentHashMap.newKeySet())
        .add(userId);

    List<RolePermission> rolePermissions = rolePermissionRepository.findByRole(user.getFkRole());
    Set<String> codes = rolePermissions.stream()
        .map(rp -> permissionRepository.findById(rp.getFkPermission()))
        .filter(java.util.Optional::isPresent)
        .map(opt -> opt.get().getNom())
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(HashSet::new));

    Set<String> immutable = Collections.unmodifiableSet(codes);
    userPermissionCache.put(userId, immutable);
    userPermissionVersion.putIfAbsent(userId, 0L);
    return immutable;
  }
}
