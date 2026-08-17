package cd.shad.erp.cmk.cmkerp.platform.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement de domaine générique pour le système d'événements asynchrone.
 *
 * <p>
 * Tous les événements métier héritent de cette classe de base.
 * Utilisé avec Kafka pour la communication asynchrone entre services.
 *
 * <p>
 * Exemples d'événements :
 * <ul>
 * <li>UserCreatedEvent</li>
 * <li>AuditEvent</li>
 * <li>NotificationEvent</li>
 * </ul>
 *

 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant timestamp;
    private final String eventType;

    protected DomainEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }
}

