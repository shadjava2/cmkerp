package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

import java.util.Collection;
import java.util.Set;

/**
 * Port du moteur RBAC centralisé.
 *
 * <p>Implémenté côté platform ; consommé par gateway / aspects / services.
 */
public interface PermissionAuthorizationPort {

  boolean hasPermission(Long userId, String permissionCode);

  boolean hasAnyPermission(Long userId, Collection<String> permissionCodes);

  boolean hasAllPermissions(Long userId, Collection<String> permissionCodes);

  void requirePermission(Long userId, String permissionCode);

  Set<String> getEffectivePermissions(Long userId);

  long getPermissionsVersion(Long userId);

  void invalidateUserPermissions(Long userId);

  void invalidateRolePermissions(Long roleId);
}
