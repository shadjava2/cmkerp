package cd.shad.erp.cmk.cmkerp.platform.events.outbox;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Repository pour la table outbox_events.
 *

 */
@Repository
public class OutboxEventRepository {

  private static final RowMapper<OutboxEvent> OUTBOX_EVENT_MAPPER = (rs, rowNum) -> OutboxEvent.builder()
      .id(rs.getLong("id"))
      .eventType(rs.getString("event_type"))
      .topic(rs.getString("topic"))
      .eventKey(rs.getString("event_key"))
      .eventPayload(rs.getString("event_payload"))
      .status(rs.getString("status"))
      .retryCount(rs.getInt("retry_count"))
      .createdAt(rs.getTimestamp("created_at") != null
          ? rs.getTimestamp("created_at").toLocalDateTime()
          : null)
      .publishedAt(rs.getTimestamp("published_at") != null
          ? rs.getTimestamp("published_at").toLocalDateTime()
          : null)
      .errorMessage(rs.getString("error_message"))
      .build();

  private final JdbcTemplate jdbcTemplate;

  public OutboxEventRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Sauvegarde un événement Outbox.
   */
  public Long save(OutboxEvent event) {
    String sql = "INSERT INTO outbox_events (event_type, topic, event_key, event_payload, status, retry_count, created_at) "
        + "VALUES (?, ?, ?, ?, 'PENDING', 0, NOW())";
    jdbcTemplate.update(sql, event.getEventType(), event.getTopic(), event.getEventKey(), event.getEventPayload());
    return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
  }

  /**
   * Récupère les événements en attente de publication.
   */
  public List<OutboxEvent> findPendingEvents(int limit) {
    String sql = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT ?";
    return jdbcTemplate.query(sql, OUTBOX_EVENT_MAPPER, limit);
  }

  /**
   * Marque un événement comme publié.
   */
  public void markAsPublished(Long id) {
    String sql = "UPDATE outbox_events SET status = 'PUBLISHED', published_at = NOW() WHERE id = ?";
    jdbcTemplate.update(sql, id);
  }

  /**
   * Marque un événement comme échoué et incrémente le retry count.
   */
  public void markAsFailed(Long id, String errorMessage, int maxRetries) {
    String sql = "UPDATE outbox_events SET retry_count = retry_count + 1, error_message = ?, "
        + "status = CASE WHEN retry_count + 1 >= ? THEN 'FAILED' ELSE 'PENDING' END "
        + "WHERE id = ?";
    jdbcTemplate.update(sql, errorMessage, maxRetries, id);
  }
}

