package cd.shad.erp.cmk.cmkerp.stocks.usage.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventEntryResponse;
import cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto.UsageEventItemRequest;

@Repository
public class AppUsageEventRepository {

  private static final Logger log = LoggerFactory.getLogger(AppUsageEventRepository.class);

  private static final RowMapper<UsageEventEntryResponse> ROW_MAPPER = (rs, i) ->
      UsageEventEntryResponse.builder()
          .id(rs.getLong("id"))
          .occurredAt(toLocalDateTime(rs.getTimestamp("occurred_at")))
          .userId(rs.getLong("user_id"))
          .username(rs.getString("username"))
          .eventType(rs.getString("event_type"))
          .module(rs.getString("module"))
          .path(rs.getString("path"))
          .pageTitle(rs.getString("page_title"))
          .actionLabel(rs.getString("action_label"))
          .element(rs.getString("element"))
          .howClient(rs.getString("how_client"))
          .userAgent(rs.getString("user_agent"))
          .pharmacieId(rs.getObject("pharmacie_id") != null ? rs.getLong("pharmacie_id") : null)
          .portal(rs.getString("portal"))
          .detailsJson(rs.getString("details_json"))
          .build();

  private final JdbcTemplate jdbc;

  public AppUsageEventRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public int insertBatch(long userId, String username, List<UsageEventItemRequest> events) {
    if (events == null || events.isEmpty()) {
      return 0;
    }
    String sql = """
        INSERT INTO app_usage_events
          (occurred_at, user_id, username, event_type, module, path, page_title,
           action_label, element, how_client, user_agent, pharmacie_id, portal, details_json)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try {
      List<Object[]> batch = new ArrayList<>(events.size());
      for (UsageEventItemRequest e : events) {
        LocalDateTime at = parseOccurredAt(e.getOccurredAt());
        batch.add(new Object[] {
            Timestamp.valueOf(at),
            userId,
            truncate(username, 80),
            truncate(normalizeType(e.getEventType()), 20),
            truncate(e.getModule(), 60),
            truncate(e.getPath(), 500),
            truncate(e.getPageTitle(), 255),
            truncate(e.getActionLabel(), 255),
            truncate(e.getElement(), 120),
            truncate(e.getHowClient() != null && !e.getHowClient().isBlank() ? e.getHowClient() : "web", 40),
            truncate(e.getUserAgent(), 512),
            e.getPharmacieId(),
            truncate(e.getPortal(), 40),
            truncate(e.getDetailsJson(), 4000)
        });
      }
      int[] counts = jdbc.batchUpdate(sql, batch);
      int total = 0;
      for (int c : counts) {
        total += Math.max(c, 0);
      }
      return total;
    } catch (Exception ex) {
      log.warn("Insert app_usage_events échoué (migration V20 ?): {}", ex.getMessage());
      return 0;
    }
  }

  public long count(
      Long filterUserId,
      String eventType,
      String q,
      LocalDateTime from,
      LocalDateTime to) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM app_usage_events WHERE 1=1");
    List<Object> args = new ArrayList<>();
    appendFilters(sql, args, filterUserId, eventType, q, from, to);
    try {
      Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
      return n != null ? n : 0L;
    } catch (Exception ex) {
      log.warn("Count app_usage_events: {}", ex.getMessage());
      return 0L;
    }
  }

  public List<UsageEventEntryResponse> findPage(
      Long filterUserId,
      String eventType,
      String q,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size) {
    StringBuilder sql = new StringBuilder(
        """
        SELECT id, occurred_at, user_id, username, event_type, module, path, page_title,
               action_label, element, how_client, user_agent, pharmacie_id, portal, details_json
        FROM app_usage_events WHERE 1=1
        """);
    List<Object> args = new ArrayList<>();
    appendFilters(sql, args, filterUserId, eventType, q, from, to);
    sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?");
    args.add(size);
    args.add(Math.max(0, page) * size);
    try {
      return jdbc.query(sql.toString(), ROW_MAPPER, args.toArray());
    } catch (Exception ex) {
      log.warn("Lecture app_usage_events: {}", ex.getMessage());
      return List.of();
    }
  }

  private static void appendFilters(
      StringBuilder sql,
      List<Object> args,
      Long filterUserId,
      String eventType,
      String q,
      LocalDateTime from,
      LocalDateTime to) {
    if (filterUserId != null) {
      sql.append(" AND user_id = ?");
      args.add(filterUserId);
    }
    if (eventType != null && !eventType.isBlank()) {
      sql.append(" AND event_type = ?");
      args.add(eventType.trim().toUpperCase());
    }
    if (from != null) {
      sql.append(" AND occurred_at >= ?");
      args.add(Timestamp.valueOf(from));
    }
    if (to != null) {
      sql.append(" AND occurred_at <= ?");
      args.add(Timestamp.valueOf(to));
    }
    if (q != null && !q.isBlank()) {
      sql.append(
          " AND (LOWER(IFNULL(username,'')) LIKE ?"
              + " OR LOWER(IFNULL(path,'')) LIKE ?"
              + " OR LOWER(IFNULL(action_label,'')) LIKE ?"
              + " OR LOWER(IFNULL(module,'')) LIKE ?"
              + " OR LOWER(IFNULL(page_title,'')) LIKE ?)");
      String like = "%" + q.trim().toLowerCase() + "%";
      for (int i = 0; i < 5; i++) {
        args.add(like);
      }
    }
  }

  private static String normalizeType(String type) {
    if (type == null || type.isBlank()) {
      return "ACTION";
    }
    String t = type.trim().toUpperCase();
    if ("NAVIGATION".equals(t) || "CLICK".equals(t) || "ACTION".equals(t)) {
      return t;
    }
    return "ACTION";
  }

  private static LocalDateTime parseOccurredAt(String raw) {
    if (raw == null || raw.isBlank()) {
      return LocalDateTime.now();
    }
    String t = raw.trim();
    try {
      if (t.endsWith("Z") || t.contains("+")) {
        return java.time.OffsetDateTime.parse(t).toLocalDateTime();
      }
      if (t.length() >= 19) {
        return LocalDateTime.parse(t.substring(0, 19));
      }
      return LocalDateTime.parse(t);
    } catch (Exception ex) {
      return LocalDateTime.now();
    }
  }

  private static String truncate(String v, int max) {
    if (v == null) {
      return null;
    }
    String t = v.trim();
    if (t.length() <= max) {
      return t;
    }
    return t.substring(0, max);
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts == null ? null : ts.toLocalDateTime();
  }
}
