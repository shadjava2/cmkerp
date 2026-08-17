package cd.shad.erp.cmk.cmkerp.platform.events;

/**
 * Interface de repository pour la persistance des événements d'audit.
 */
public interface AuditEventRepository {

    int save(AuditEvent event);
}


