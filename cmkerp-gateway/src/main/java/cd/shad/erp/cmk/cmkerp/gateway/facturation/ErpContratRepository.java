package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
      // Ne jamais réactiver ni écraser le statut actif ERP.
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

  /**
   * Import sélectif : insertion uniquement si le code est absent. Aucune mise à jour,
   * aucune réactivation du statut actif.
   *
   * @return 1 si créé, 0 si déjà présent / ignoré
   */
  public int insertIfAbsentFromImport(String code, String designation) {
    String c = normalizeCode(code);
    if (c == null || c.isBlank()) {
      return 0;
    }
    if (findByCode(c).isPresent()) {
      return 0;
    }
    insert(c, trimTo(designation, 50), true, "IMPORT_CLINIQUE", c, "IMPORTED", null);
    return 1;
  }

  public void enqueueOutbox(String operationId, String entityType, Long entityId, String legacyKey,
      String operationType, String payloadJson, String correlationId, String status) {
    jdbc.update(
        """
            INSERT INTO erp_sync_outbox
              (operation_id, entity_type, entity_id, legacy_key, source_system, target_system,
               operation_type, payload_json, status, attempt_count, correlation_id)
            VALUES (?, ?, ?, ?, 'ERP', 'CLINIQUE_BEACH', ?, ?, ?, 0, ?)
            """,
        operationId,
        entityType,
        entityId,
        legacyKey,
        operationType,
        payloadJson,
        status == null || status.isBlank() ? "PENDING" : status,
        correlationId);
  }

  public long countOutboxByStatus(String status) {
    Long n = jdbc.queryForObject(
        "SELECT COUNT(*) FROM erp_sync_outbox WHERE status = ?", Long.class, status);
    return n == null ? 0 : n;
  }

  public long countBySourceSystem(String sourceSystem) {
    Long n = jdbc.queryForObject(
        "SELECT COUNT(*) FROM erp_contrats WHERE source_system = ?", Long.class, sourceSystem);
    return n == null ? 0 : n;
  }

  public long countBySyncStatus(String syncStatus) {
    Long n = jdbc.queryForObject(
        "SELECT COUNT(*) FROM erp_contrats WHERE sync_status = ?", Long.class, syncStatus);
    return n == null ? 0 : n;
  }

  public boolean hasOpenOutbox(long entityId, String entityType) {
    Long n = jdbc.queryForObject(
        """
            SELECT COUNT(*) FROM erp_sync_outbox
            WHERE entity_id = ? AND entity_type = ?
              AND status IN ('PENDING', 'SUSPENDED', 'IN_PROGRESS', 'UNCERTAIN')
            """,
        Long.class,
        entityId,
        entityType);
    return n != null && n > 0;
  }

  public List<ErpContratRecord> listBySourceSystem(String sourceSystem) {
    return jdbc.query(
        """
            SELECT id, code, designation, actif, source_system, legacy_code, sync_status,
                   last_synced_at, version, datecreate, dateupdate
            FROM erp_contrats WHERE source_system = ? ORDER BY code
            """,
        MAPPER,
        sourceSystem);
  }

  public List<Map<String, Object>> listOutbox(String status, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT id, operation_id, entity_type, entity_id, legacy_key, operation_type, status,
                   attempt_count, last_error, correlation_id, datecreate, datesynced, dateupdate
            FROM erp_sync_outbox WHERE 1=1
            """);
    var args = new java.util.ArrayList<Object>();
    if (status != null && !status.isBlank()) {
      sql.append(" AND status = ?");
      args.add(status);
    }
    sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
    args.add(Math.min(Math.max(limit, 1), 200));
    args.add(Math.max(offset, 0));
    return jdbc.query(sql.toString(), (rs, i) -> {
      Map<String, Object> row = new java.util.LinkedHashMap<>();
      row.put("id", rs.getLong("id"));
      row.put("operationId", rs.getString("operation_id"));
      row.put("entityType", rs.getString("entity_type"));
      row.put("entityId", rs.getObject("entity_id"));
      row.put("legacyKey", rs.getString("legacy_key"));
      row.put("operationType", rs.getString("operation_type"));
      row.put("status", rs.getString("status"));
      row.put("attemptCount", rs.getInt("attempt_count"));
      row.put("lastError", rs.getString("last_error"));
      row.put("correlationId", rs.getString("correlation_id"));
      row.put("createdAt", rs.getTimestamp("datecreate"));
      row.put("syncedAt", rs.getTimestamp("datesynced"));
      row.put("updatedAt", rs.getTimestamp("dateupdate"));
      return row;
    }, args.toArray());
  }

  public int promoteSuspendedToPending() {
    return jdbc.update(
        """
            UPDATE erp_sync_outbox
            SET status = 'PENDING', dateupdate = NOW()
            WHERE status = 'SUSPENDED'
            """);
  }

  public List<Map<String, Object>> claimPendingOutbox(int limit) {
    List<Map<String, Object>> rows = jdbc.query(
        """
            SELECT id, operation_id, entity_type, entity_id, legacy_key, operation_type, payload_json,
                   status, attempt_count, correlation_id
            FROM erp_sync_outbox
            WHERE status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= NOW())
            ORDER BY id ASC
            LIMIT ?
            """,
        (rs, i) -> {
          Map<String, Object> row = new java.util.LinkedHashMap<>();
          row.put("id", rs.getLong("id"));
          row.put("operationId", rs.getString("operation_id"));
          row.put("entityType", rs.getString("entity_type"));
          row.put("entityId", rs.getObject("entity_id"));
          row.put("legacyKey", rs.getString("legacy_key"));
          row.put("operationType", rs.getString("operation_type"));
          row.put("payloadJson", rs.getString("payload_json"));
          row.put("attemptCount", rs.getInt("attempt_count"));
          row.put("correlationId", rs.getString("correlation_id"));
          return row;
        },
        Math.min(Math.max(limit, 1), 50));
    for (Map<String, Object> row : rows) {
      jdbc.update(
          """
              UPDATE erp_sync_outbox
              SET status = 'IN_PROGRESS', attempt_count = attempt_count + 1, dateupdate = NOW()
              WHERE id = ? AND status = 'PENDING'
              """,
          row.get("id"));
    }
    return rows;
  }

  public void markOutboxSynced(long id) {
    jdbc.update(
        """
            UPDATE erp_sync_outbox
            SET status = 'SYNCED', datesynced = NOW(), last_error = NULL, dateupdate = NOW()
            WHERE id = ?
            """,
        id);
  }

  public void markOutboxFailed(long id, String error) {
    jdbc.update(
        """
            UPDATE erp_sync_outbox
            SET status = 'FAILED', last_error = ?, next_attempt_at = DATE_ADD(NOW(), INTERVAL (attempt_count * 2) MINUTE),
                dateupdate = NOW()
            WHERE id = ?
            """,
        error == null ? null : (error.length() > 900 ? error.substring(0, 900) : error),
        id);
  }

  public void markContratSynced(long id, String legacyCode) {
    jdbc.update(
        """
            UPDATE erp_contrats
            SET sync_status = 'SYNCED', legacy_code = ?, last_synced_at = NOW(), dateupdate = NOW()
            WHERE id = ?
            """,
        legacyCode,
        id);
  }

  public void updateSyncStatus(long id, String syncStatus) {
    jdbc.update(
        "UPDATE erp_contrats SET sync_status = ?, dateupdate = NOW() WHERE id = ?",
        syncStatus,
        id);
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
