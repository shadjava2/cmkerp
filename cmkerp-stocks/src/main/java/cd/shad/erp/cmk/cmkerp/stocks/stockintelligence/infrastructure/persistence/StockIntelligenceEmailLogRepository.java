package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceEmailLogEntryDTO;

@Repository
public class StockIntelligenceEmailLogRepository {

  private static final Logger log = LoggerFactory.getLogger(StockIntelligenceEmailLogRepository.class);

  private static final RowMapper<StockIntelligenceEmailLogEntryDTO> ROW_MAPPER = (rs, rowNum) ->
      new StockIntelligenceEmailLogEntryDTO(
          rs.getLong("id"),
          rs.getString("report_type"),
          rs.getString("recipient"),
          rs.getString("status"),
          rs.getObject("snapshot_id") != null ? rs.getLong("snapshot_id") : null,
          rs.getString("error_detail"),
          toLocalDateTime(rs.getTimestamp("sent_at")));

  private final JdbcTemplate jdbc;

  public StockIntelligenceEmailLogRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void log(String reportType, String recipient, String status, Long snapshotId, String error) {
    try {
      jdbc.update("""
          INSERT INTO stock_intelligence_email_log
            (report_type, recipient, status, snapshot_id, error_detail)
          VALUES (?, ?, ?, ?, ?)
          """, reportType, recipient, status, snapshotId, error);
      log.info("Historique email enregistré — {} -> {} ({})", reportType, recipient, status);
    } catch (Exception e) {
      log.warn("Audit email non enregistré — exécutez la migration Flyway V15: {}", e.getMessage());
    }
  }

  public int countSentToday(String reportType) {
    return countTodayByStatus(reportType, "SENT");
  }

  public int countFailedToday(String reportType) {
    return countTodayByStatus(reportType, "FAILED");
  }

  public int countAll() {
    try {
      Integer count = jdbc.queryForObject(
          "SELECT COUNT(*) FROM stock_intelligence_email_log", Integer.class);
      return count != null ? count : 0;
    } catch (Exception e) {
      return 0;
    }
  }

  public int countSentTodayAll() {
    try {
      Integer count = jdbc.queryForObject("""
          SELECT COUNT(*) FROM stock_intelligence_email_log
          WHERE status = 'SENT' AND DATE(sent_at) = CURDATE()
          """, Integer.class);
      return count != null ? count : 0;
    } catch (Exception e) {
      return 0;
    }
  }

  public int countFailedTodayAll() {
    try {
      Integer count = jdbc.queryForObject("""
          SELECT COUNT(*) FROM stock_intelligence_email_log
          WHERE status = 'FAILED' AND DATE(sent_at) = CURDATE()
          """, Integer.class);
      return count != null ? count : 0;
    } catch (Exception e) {
      return 0;
    }
  }

  public List<StockIntelligenceEmailLogEntryDTO> findRecent(int limit, String reportType) {
    int safeLimit = Math.min(Math.max(limit, 1), 200);
    try {
      if (reportType != null && !reportType.isBlank()) {
        return jdbc.query("""
            SELECT id, report_type, recipient, status, snapshot_id, error_detail, sent_at
            FROM stock_intelligence_email_log
            WHERE report_type = ?
            ORDER BY sent_at DESC, id DESC
            LIMIT ?
            """, ROW_MAPPER, reportType, safeLimit);
      }
      return jdbc.query("""
          SELECT id, report_type, recipient, status, snapshot_id, error_detail, sent_at
          FROM stock_intelligence_email_log
          ORDER BY sent_at DESC, id DESC
          LIMIT ?
          """, ROW_MAPPER, safeLimit);
    } catch (Exception e) {
      log.warn("Impossible de lire stock_intelligence_email_log: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  private int countTodayByStatus(String reportType, String status) {
    try {
      Integer count = jdbc.queryForObject("""
          SELECT COUNT(*) FROM stock_intelligence_email_log
          WHERE report_type = ? AND status = ?
            AND DATE(sent_at) = CURDATE()
          """, Integer.class, reportType, status);
      return count != null ? count : 0;
    } catch (Exception e) {
      return 0;
    }
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime() : null;
  }
}
