package cd.shad.erp.cmk.cmkerp.platform.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cd.shad.erp.cmk.cmkerp.platform.events.outbox.OutboxEvent;
import cd.shad.erp.cmk.cmkerp.platform.events.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publisher d'événements transactionnel utilisant le pattern Outbox.
 *
 * <p>
 * Persiste les événements dans la table outbox_events dans la même transaction DB,
 * garantissant que l'événement sera publié même en cas de crash après le commit.
 *

 */
@Component
@Primary
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class TransactionalDomainEventPublisher implements IDomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(TransactionalDomainEventPublisher.class);

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public TransactionalDomainEventPublisher(OutboxEventRepository outboxEventRepository,
      ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void publishUserEvent(UserEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      OutboxEvent outboxEvent = OutboxEvent.builder()
          .eventType("USER_EVENT")
          .topic("cmkerp-user-events")
          .eventKey(event.getUserId().toString())
          .eventPayload(payload)
          .build();
      outboxEventRepository.save(outboxEvent);
      log.debug("Événement utilisateur enregistré dans Outbox -> userId: {}, type: {}", event.getUserId(),
          event.getType());
    } catch (Exception e) {
      log.error("Erreur lors de l'enregistrement de l'événement utilisateur dans Outbox", e);
      // Ne pas propager l'exception pour ne pas interrompre la transaction métier
    }
  }

  @Override
  @Transactional
  public void publishAuditEvent(AuditEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      OutboxEvent outboxEvent = OutboxEvent.builder()
          .eventType("AUDIT_EVENT")
          .topic("cmkerp-audit-events")
          .eventKey(event.getUserId().toString())
          .eventPayload(payload)
          .build();
      outboxEventRepository.save(outboxEvent);
      log.debug("Événement audit enregistré dans Outbox -> userId: {}, action: {}", event.getUserId(),
          event.getAction());
    } catch (Exception e) {
      log.error("Erreur lors de l'enregistrement de l'événement audit dans Outbox", e);
    }
  }

  @Override
  @Transactional
  public void publishEvent(String topic, String key, DomainEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      OutboxEvent outboxEvent = OutboxEvent.builder()
          .eventType(event.getEventType())
          .topic(topic)
          .eventKey(key)
          .eventPayload(payload)
          .build();
      outboxEventRepository.save(outboxEvent);
      log.debug("Événement générique enregistré dans Outbox -> topic: {}, key: {}", topic, key);
    } catch (Exception e) {
      log.error("Erreur lors de l'enregistrement de l'événement générique dans Outbox", e);
    }
  }
}

