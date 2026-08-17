package cd.shad.erp.cmk.cmkerp.platform.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @deprecated Utiliser TransactionalDomainEventPublisher pour la cohérence transactionnelle.
 */
@Deprecated
@Component
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class DomainEventPublisher implements IDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    /**
     * @deprecated This constant is deprecated along with the enclosing class.
     * Use {@link TransactionalDomainEventPublisher} instead.
     */
    @Deprecated
    public static final String TOPIC_USER_EVENTS = "cmkerp-user-events";

    /**
     * @deprecated This constant is deprecated along with the enclosing class.
     * Use {@link TransactionalDomainEventPublisher} instead.
     */
    @Deprecated
    public static final String TOPIC_AUDIT_EVENTS = "cmkerp-audit-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * @deprecated This constructor is deprecated along with the enclosing class.
     * Use {@link TransactionalDomainEventPublisher} instead.
     */
    @Deprecated
    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * @deprecated Utiliser TransactionalDomainEventPublisher.
     */
    @Deprecated
    public void publishUserEvent(UserEvent event) {
        try {
            kafkaTemplate.send(TOPIC_USER_EVENTS, event.getUserId().toString(), event);
            log.debug("Événement utilisateur publié : {} - {}", event.getType(), event.getUsername());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement utilisateur : {}", event, e);
            // En cas d'erreur Kafka, on log mais on n'interrompt pas le flux métier
            // (pattern "fire and forget" pour éviter de bloquer l'API)
        }
    }

    /**
     * @deprecated Utiliser TransactionalDomainEventPublisher.
     */
    @Deprecated
    public void publishAuditEvent(AuditEvent event) {
        try {
            kafkaTemplate.send(TOPIC_AUDIT_EVENTS, event.getUserId().toString(), event);
            log.debug("Événement d'audit publié : {} - {} - {}", event.getAction(), event.getResourceType(), event.getResourceId());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement d'audit : {}", event, e);
            // En cas d'erreur Kafka, on log mais on n'interrompt pas le flux métier
        }
    }

    /**
     * @deprecated Utiliser TransactionalDomainEventPublisher.
     */
    @Deprecated
    public void publishEvent(String topic, String key, DomainEvent event) {
        try {
            kafkaTemplate.send(topic, key, event);
            log.debug("Événement publié sur topic {} : {}", topic, event.getEventType());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement sur topic {} : {}", topic, event, e);
        }
    }
}

