package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

/**
 * Constantes du rôle système ADMIN — protections structurelles centralisées.
 *
 * <p>Aucune règle métier d'attribution de droits pour les rôles fonctionnels
 * ne doit être codée en dur. Seul ADMIN dispose d'une protection système.
 */
public final class SystemRoleConstants {

  private SystemRoleConstants() {}

  /** Identifiant technique du rôle système ADMIN (source de vérité DB). */
  public static final long ADMIN_ROLE_ID = 5L;

  /** Nom canonique du rôle système. */
  public static final String ADMIN_ROLE_NAME = "ADMIN";

  public static boolean isAdminRoleId(Long roleId) {
    return roleId != null && roleId == ADMIN_ROLE_ID;
  }

  public static boolean isAdminRoleName(String roleName) {
    if (roleName == null) {
      return false;
    }
    String normalized = roleName.trim();
    return ADMIN_ROLE_NAME.equalsIgnoreCase(normalized)
        || "ADMINISTRATEUR".equalsIgnoreCase(normalized);
  }
}
