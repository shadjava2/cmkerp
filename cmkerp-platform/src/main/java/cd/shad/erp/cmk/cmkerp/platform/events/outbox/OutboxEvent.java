package cd.shad.erp.cmk.cmkerp.platform.events.outbox;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement Outbox pour garantir la publication transactionnelle vers Kafka.
 *
 * <p>
 * Pattern Outbox : persiste l'événement dans la même transaction DB que l'entité métier,
 * puis un processus séparé publie vers Kafka. Cela garantit que l'événement est publié
 * même en cas de crash après le commit.
 *

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

  private Long id;
  private String eventType; // USER_CREATED, NOTIFICATION_SENT, etc.
  private String topic; // cmkerp-user-events, cmkerp-audit-events, etc.
  private String eventKey; // Clé de partition Kafka (userId, etc.)
  private String eventPayload; // JSON de l'événement
  private String status; // PENDING, PUBLISHED, FAILED
  private Integer retryCount;
  private LocalDateTime createdAt;
  private LocalDateTime publishedAt;
  private String errorMessage;
}

