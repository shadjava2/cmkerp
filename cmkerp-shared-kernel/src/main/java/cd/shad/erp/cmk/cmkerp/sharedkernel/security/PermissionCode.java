package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration of permission codes used in the system. Each value maps to a row in the database
 * table `permissions.nom`. The string code returned by {@link #getCode()} must exactly match
 * the value stored in the `permissions.nom` column in the database.
 */
public enum PermissionCode {

  // User management
  MANAGE_USERS("manage_users"), VIEW_USERS("view_users"),

  // Dashboard
  VIEW_DASHBOARD("view_dashboard"),

  // Stock management
  MANAGE_STOCK("manage_stock"), VIEW_STOCK("view_stock"),

  // Pharmacy management
  MANAGE_PHARMACIES("manage_pharmacies"), VIEW_PHARMACIES("view_pharmacies"),

  // Roles and permissions
  MANAGE_ROLES("manage_roles"), VIEW_ROLES("view_roles"),

  // Site management
  MANAGE_SITES("manage_sites"), VIEW_SITES("view_sites"),

  // Notifications
  MANAGE_NOTIFICATIONS("manage_notifications"), VIEW_NOTIFICATIONS("view_notifications");

  private final String code;

  PermissionCode(String code) {
    this.code = code;
  }

  /**
   * Get the string code representation.
   *
   * @return the permission code string
   */
  public String getCode() {
    return code;
  }

  /**
   * Convert a string code to the corresponding enum constant.
   *
   * @param code the permission code string
   * @return Optional containing the matching enum, or empty if not found
   */
  public static Optional<PermissionCode> fromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(e -> e.code.equals(code)).findFirst();
  }
}
