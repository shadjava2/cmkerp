package cd.shad.erp.cmk.cmkerp.platform.events;

import lombok.Getter;

/**
 * Événement d'audit pour tracer les actions utilisateur.
 *
 * <p>
 * Publié pour chaque action importante (création, modification, suppression, etc.)
 * pour permettre un audit trail complet et inviolable.
 *

 */
@Getter
public class AuditEvent extends DomainEvent {

    private final Long userId;
    private final String username;
    private final String action;
    private final String resourceType;
    private final Long resourceId;
    private final String details;

    public AuditEvent(Long userId, String username, String action, String resourceType, Long resourceId, String details) {
        super("AUDIT_EVENT");
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.details = details;
    }
}

