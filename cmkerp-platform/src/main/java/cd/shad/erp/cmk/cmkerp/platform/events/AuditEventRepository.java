package cd.shad.erp.cmk.cmkerp.platform.events;

import java.util.List;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.RolePermissionAuditEntry;

/**
 * Interface de repository pour la persistance des événements d'audit.
 */
public interface AuditEventRepository {

    int save(AuditEvent event);

    /**
     * Historique des modifications de permissions d'un rôle (sans données médicales).
     */
    List<RolePermissionAuditEntry> findRolePermissionChanges(Long roleId, int limit);
}


