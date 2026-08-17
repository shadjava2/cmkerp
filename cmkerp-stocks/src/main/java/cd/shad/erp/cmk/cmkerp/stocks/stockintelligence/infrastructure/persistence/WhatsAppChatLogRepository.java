package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppChatLogEntryDTO;

@Repository
public class WhatsAppChatLogRepository {

  private final JdbcTemplate jdbc;

  public WhatsAppChatLogRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void logInbound(String waMessageId, String fromNumber, String text) {
    jdbc.update("""
        INSERT INTO whatsapp_chat_log (wa_message_id, from_number, direction, message_text, status)
        VALUES (?, ?, 'IN', ?, 'RECEIVED')
        """, waMessageId, fromNumber, text);
  }

  public void logOutbound(String fromNumber, String question, String aiResponse, Long snapshotId, String status, String error) {
    jdbc.update("""
        INSERT INTO whatsapp_chat_log
          (from_number, direction, message_text, ai_response, snapshot_id, status, error_detail)
        VALUES (?, 'OUT', ?, ?, ?, ?, ?)
        """, fromNumber, question, aiResponse, snapshotId, status, error);
  }

  public int countAll() {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM whatsapp_chat_log", Integer.class);
    return count != null ? count : 0;
  }

  public List<WhatsAppChatLogEntryDTO> findRecent(int limit) {
    int safeLimit = Math.min(Math.max(limit, 1), 200);
    return jdbc.query("""
        SELECT id, direction, from_number, message_text, ai_response, status, error_detail, created_at
        FROM whatsapp_chat_log
        ORDER BY created_at DESC, id DESC
        LIMIT ?
        """, rowMapper(), safeLimit);
  }

  private static RowMapper<WhatsAppChatLogEntryDTO> rowMapper() {
    return (rs, rowNum) -> new WhatsAppChatLogEntryDTO(
        rs.getLong("id"),
        rs.getString("direction"),
        rs.getString("from_number"),
        rs.getString("message_text"),
        rs.getString("ai_response"),
        rs.getString("status"),
        rs.getString("error_detail"),
        toLocalDateTime(rs.getTimestamp("created_at")));
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime() : null;
  }
}
