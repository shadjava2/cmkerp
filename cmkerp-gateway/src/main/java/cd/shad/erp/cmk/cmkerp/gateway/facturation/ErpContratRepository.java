package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ErpContratRepository {

  private static final RowMapper<ErpContratRecord> MAPPER = (rs, i) -> new ErpContratRecord(
      rs.getLong("id"),
      trim(rs.getString("code")),
      trim(rs.getString("designation")),
      rs.getBoolean("actif"),
      trim(rs.getString("source_system")),
      trim(rs.getString("legacy_code")),
      trim(rs.getString("sync_status")),
      toLdt(rs.getTimestamp("last_synced_at")),
      rs.getInt("version"),
      toLdt(rs.getTimestamp("datecreate")),
      toLdt(rs.getTimestamp("dateupdate")));

  private final JdbcTemplate jdbc;

  public ErpContratRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<ErpContratRecord> search(String q, Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT id, code, designation, actif, source_system, legacy_code, sync_status,
                   last_synced_at, version, datecreate, dateupdate
            FROM erp_contrats WHERE 1=1
            """);
    var args = new java.util.ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (code LIKE ? OR designation LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
    }
    if (actif != null) {
      sql.append(" AND actif = ?");
      args.add(actif ? 1 : 0);
    }
    sql.append(" ORDER BY code ASC LIMIT ? OFFSET ?");
    args.add(Math.min(Math.max(limit, 1), 500));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), MAPPER, args.toArray());
  }

  public long count(String q, Boolean actif) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM erp_contrats WHERE 1=1");
    var args = new java.util.ArrayList<Object>();
    if (q != null && !q.isBlank()) {
      sql.append(" AND (code LIKE ? OR designation LIKE ?)");
      String like = "%" + q.trim() + "%";
      args.add(like);
      args.add(like);
    }
    if (actif != null) {
      sql.append(" AND actif = ?");
      args.add(actif ? 1 : 0);
    }
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  public Optional<ErpContratRecord> findById(long id) {
    var list = jdbc.query(
        """
            SELECT id, code, designation, actif, source_system, legacy_code, sync_status,
                   last_synced_at, version, datecreate, dateupdate
            FROM erp_contrats WHERE id = ?
            """,
        MAPPER,
        id);
    return list.stream().findFirst();
  }

  public Optional<ErpContratRecord> findByCode(String code) {
    var list = jdbc.query(
        """
            SELECT id, code, designation, actif, source_system, legacy_code, sync_status,
                   last_synced_at, version, datecreate, dateupdate
            FROM erp_contrats WHERE code = ?
            """,
        MAPPER,
        normalizeCode(code));
    return list.stream().findFirst();
  }

  public long insert(String code, String designation, boolean actif, String sourceSystem,
      String legacyCode, String syncStatus, Long userId) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      var ps = con.prepareStatement(
          """
              INSERT INTO erp_contrats
                (code, designation, actif, source_system, legacy_code, sync_status,
                 usercreateid, userupdateid)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)
              """,
          new String[] {"id"});
      ps.setString(1, normalizeCode(code));
      ps.setString(2, trimTo(designation, 50));
      ps.setInt(3, actif ? 1 : 0);
      ps.setString(4, sourceSystem);
      ps.setString(5, legacyCode == null ? null : normalizeCode(legacyCode));
      ps.setString(6, syncStatus);
      if (userId == null) {
        ps.setObject(7, null);
        ps.setObject(8, null);
      } else {
        ps.setLong(7, userId);
        ps.setLong(8, userId);
      }
      return ps;
    }, keys);
    Number key = keys.getKey();
    return key == null ? 0L : key.longValue();
  }

  public int updateDesignation(long id, String designation, int expectedVersion, Long userId) {
    return jdbc.update(
        """
            UPDATE erp_contrats
            SET designation = ?, version = version + 1, userupdateid = ?, dateupdate = NOW()
            WHERE id = ? AND version = ?
            """,
        trimTo(designation, 50),
        userId,
        id,
        expectedVersion);
  }

  public int updateActif(long id, boolean actif, int expectedVersion, Long userId) {
    return jdbc.update(
        """
            UPDATE erp_contrats
            SET actif = ?, version = version + 1, userupdateid = ?, dateupdate = NOW()
            WHERE id = ? AND version = ?
            """,
        actif ? 1 : 0,
        userId,
        id,
        expectedVersion);
  }

  public int upsertFromImport(String code, String designation) {
    String c = normalizeCode(code);
    String d = trimTo(designation, 50);
    Optional<ErpContratRecord> existing = findByCode(c);
    if (existing.isPresent()) {
      return jdbc.update(
          """
              UPDATE erp_contrats
              SET designation = ?, legacy_code = ?, source_system = 'IMPORT_CLINIQUE',
                  sync_status = 'IMPORTED', last_synced_at = NOW(), dateupdate = NOW()
              WHERE code = ?
              """,
          d, c, c);
    }
    insert(c, d, true, "IMPORT_CLINIQUE", c, "IMPORTED", null);
    return 1;
  }

  public void enqueueOutbox(String operationId, String entityType, Long entityId, String legacyKey,
      String operationType, String payloadJson, String correlationId) {
    jdbc.update(
        """
            INSERT INTO erp_sync_outbox
              (operation_id, entity_type, entity_id, legacy_key, source_system, target_system,
               operation_type, payload_json, status, attempt_count, correlation_id)
            VALUES (?, ?, ?, ?, 'ERP', 'CLINIQUE_BEACH', ?, ?, 'PENDING', 0, ?)
            """,
        operationId,
        entityType,
        entityId,
        legacyKey,
        operationType,
        payloadJson,
        correlationId);
  }

  public long countOutboxByStatus(String status) {
    Long n = jdbc.queryForObject(
        "SELECT COUNT(*) FROM erp_sync_outbox WHERE status = ?", Long.class, status);
    return n == null ? 0 : n;
  }

  static String normalizeCode(String code) {
    if (code == null) {
      return null;
    }
    String t = code.trim();
    if (t.length() > 3) {
      throw new IllegalArgumentException("Code contrat trop long (max 3): " + t);
    }
    return t;
  }

  private static String trim(String v) {
    return v == null ? null : v.trim();
  }

  private static String trimTo(String v, int max) {
    if (v == null) {
      return null;
    }
    String t = v.trim();
    return t.length() <= max ? t : t.substring(0, max);
  }

  private static LocalDateTime toLdt(Timestamp ts) {
    return ts == null ? null : ts.toLocalDateTime();
  }
}
