package cd.shad.erp.cmk.cmkerp.platform.events.outbox;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service de publication des événements Outbox vers Kafka.
 *
 * <p>
 * Polling périodique (toutes les 5 secondes) pour récupérer les événements PENDING
 * et les publier vers Kafka. Garantit la cohérence transactionnelle.
 *

 */
@Component
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OutboxEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
  private static final int MAX_RETRIES = 3;
  private static final int BATCH_SIZE = 50;

  private final OutboxEventRepository outboxEventRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public OutboxEventPublisher(OutboxEventRepository outboxEventRepository,
      KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * Publie les événements Outbox en attente vers Kafka.
   * Exécuté toutes les 5 secondes.
   */
  @Scheduled(fixedDelay = 5000, initialDelay = 10000)
  public void publishPendingEvents() {
    try {
      List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(BATCH_SIZE);

      if (pendingEvents.isEmpty()) {
        return;
      }

      log.debug("Publication de {} événements Outbox vers Kafka", pendingEvents.size());

      for (OutboxEvent event : pendingEvents) {
        try {
          // Désérialiser le payload JSON
          Object eventObject = objectMapper.readValue(event.getEventPayload(), Object.class);

          // Publier vers Kafka
          kafkaTemplate.send(event.getTopic(), event.getEventKey(), eventObject);

          // Marquer comme publié
          outboxEventRepository.markAsPublished(event.getId());

          log.debug("Événement Outbox publié -> id: {}, topic: {}, key: {}", event.getId(), event.getTopic(),
              event.getEventKey());
        } catch (Exception e) {
          log.error("Erreur lors de la publication de l'événement Outbox -> id: {}, topic: {}", event.getId(),
              event.getTopic(), e);
          outboxEventRepository.markAsFailed(event.getId(), e.getMessage(), MAX_RETRIES);
        }
      }
    } catch (Exception e) {
      log.error("Erreur lors de la publication des événements Outbox", e);
    }
  }
}

