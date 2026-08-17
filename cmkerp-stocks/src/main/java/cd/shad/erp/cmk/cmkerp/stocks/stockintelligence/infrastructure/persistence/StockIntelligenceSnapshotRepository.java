package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class StockIntelligenceSnapshotRepository {

  private final JdbcTemplate jdbc;

  public StockIntelligenceSnapshotRepository(
      @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Long save(String reportType, Long pharmacieId, String snapshotJson, String aiAnalysisJson) {
    String sql = """
        INSERT INTO stock_intelligence_snapshots
          (report_type, pharmacie_id, snapshot_json, ai_analysis_json, generated_at)
        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(connection -> {
      var ps = connection.prepareStatement(sql, new String[] {"id"});
      ps.setString(1, reportType);
      if (pharmacieId != null) {
        ps.setLong(2, pharmacieId);
      } else {
        ps.setObject(2, null);
      }
      ps.setString(3, snapshotJson);
      ps.setString(4, aiAnalysisJson);
      return ps;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  public Optional<String> findLatestSnapshotJson() {
    String sql = """
        SELECT snapshot_json FROM stock_intelligence_snapshots
        ORDER BY generated_at DESC LIMIT 1
        """;
    return jdbc.query(sql, rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.empty());
  }

  public java.util.List<String> findMorningRecipientEmails() {
    return findRecipientEmails("receive_morning_report");
  }

  public java.util.List<String> findEveningRecipientEmails() {
    return findRecipientEmails("receive_evening_report");
  }

  private java.util.List<String> findRecipientEmails(String flagColumn) {
    String sql = """
        SELECT email FROM stock_intelligence_recipients
        WHERE active = 1 AND %s = 1 AND email IS NOT NULL AND email <> ''
        """.formatted(flagColumn);
    return jdbc.queryForList(sql, String.class);
  }
}
