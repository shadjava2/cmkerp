package cd.shad.erp.cmk.cmkerp.platform.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implémentation no-op de {@link IDomainEventPublisher} utilisée quand Kafka est désactivé.
 *
 * <p>
 * Cette implémentation ne fait rien mais permet au code métier de continuer à fonctionner
 * sans erreur même quand Kafka n'est pas disponible. Les événements sont simplement ignorés.
 *
 * <p>
 * Cette implémentation est conditionnelle : elle ne s'active que si {@code cmkerp.kafka.enabled=false}
 * ou si la propriété n'est pas définie (matchIfMissing = true).
 *

 */
@Component
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpDomainEventPublisher implements IDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpDomainEventPublisher.class);

    /**
     * Publie un événement utilisateur (no-op).
     *
     * @param event l'événement utilisateur (ignoré)
     */
    @Override
    public void publishUserEvent(UserEvent event) {
        log.debug("Kafka désactivé - Événement utilisateur ignoré : {} - {}", event.getType(), event.getUsername());
    }

    /**
     * Publie un événement d'audit (no-op).
     *
     * @param event l'événement d'audit (ignoré)
     */
    @Override
    public void publishAuditEvent(AuditEvent event) {
        log.debug("Kafka désactivé - Événement d'audit ignoré : {} - {} - {}",
                event.getAction(), event.getResourceType(), event.getResourceId());
    }

    /**
     * Publie un événement générique (no-op).
     *
     * @param topic le topic (ignoré)
     * @param key la clé (ignorée)
     * @param event l'événement (ignoré)
     */
    @Override
    public void publishEvent(String topic, String key, DomainEvent event) {
        log.debug("Kafka désactivé - Événement ignoré sur topic {} : {}", topic, event.getEventType());
    }
}

