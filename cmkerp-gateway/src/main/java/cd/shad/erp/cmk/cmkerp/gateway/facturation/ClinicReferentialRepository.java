package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
public class ClinicReferentialRepository {

  private static final RowMapper<ClinicReferentialRecord> MAPPER = (rs, i) -> new ClinicReferentialRecord(
      rs.getLong("id"),
      rs.getString("referential_key"),
      rs.getString("natural_key"),
      rs.getString("label"),
      rs.getString("payload_json"),
      rs.getString("sync_fingerprint"),
      rs.getBoolean("actif"),
      rs.getString("source_system"),
      rs.getString("legacy_key"),
      rs.getString("sync_status"),
      toLdt(rs.getTimestamp("last_synced_at")),
      rs.getInt("version"),
      toLdt(rs.getTimestamp("datecreate")),
      toLdt(rs.getTimestamp("dateupdate")));

  private final JdbcTemplate jdbc;

  public ClinicReferentialRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<ClinicReferentialRecord> search(String referentialKey, String q, Boolean actif, int limit, int offset) {
    StringBuilder sql = new StringBuilder(
        """
            SELECT id, referential_key, natural_key, label, payload_json, sync_fingerprint, actif,
                   source_system, legacy_key, sync_status, last_synced_at, version, datecreate, dateupdate
            FROM erp_clinic_referentials WHERE referential_key = ?
            """);
    List<Object> args = new ArrayList<>();
    args.add(referentialKey);
    appendFilters(sql, args, q, actif);
    sql.append(" ORDER BY natural_key LIMIT ? OFFSET ?");
    args.add(limit);
    args.add(offset);
    return jdbc.query(sql.toString(), MAPPER, args.toArray());
  }

  public long count(String referentialKey, String q, Boolean actif) {
    StringBuilder sql = new StringBuilder(
        "SELECT COUNT(*) FROM erp_clinic_referentials WHERE referential_key = ?");
    List<Object> args = new ArrayList<>();
    args.add(referentialKey);
    appendFilters(sql, args, q, actif);
    Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n;
  }

  public Optional<ClinicReferentialRecord> findById(String referentialKey, long id) {
    List<ClinicReferentialRecord> rows = jdbc.query(
        """
            SELECT id, referential_key, natural_key, label, payload_json, sync_fingerprint, actif,
                   source_system, legacy_key, sync_status, last_synced_at, version, datecreate, dateupdate
            FROM erp_clinic_referentials WHERE referential_key = ? AND id = ?
            """,
        MAPPER,
        referentialKey,
        id);
    return rows.stream().findFirst();
  }

  public Optional<ClinicReferentialRecord> findByNaturalKey(String referentialKey, String naturalKey) {
    List<ClinicReferentialRecord> rows = jdbc.query(
        """
            SELECT id, referential_key, natural_key, label, payload_json, sync_fingerprint, actif,
                   source_system, legacy_key, sync_status, last_synced_at, version, datecreate, dateupdate
            FROM erp_clinic_referentials WHERE referential_key = ? AND natural_key = ?
            """,
        MAPPER,
        referentialKey,
        naturalKey);
    return rows.stream().findFirst();
  }

  public Map<String, Snap> index(String referentialKey) {
    Map<String, Snap> out = new LinkedHashMap<>();
    jdbc.query(
        """
            SELECT natural_key, label, sync_fingerprint, actif
            FROM erp_clinic_referentials WHERE referential_key = ?
            """,
        rs -> {
          out.put(rs.getString("natural_key"), new Snap(
              rs.getString("label"),
              rs.getString("sync_fingerprint"),
              rs.getBoolean("actif")));
        },
        referentialKey);
    return out;
  }

  public Map<String, Snap> findSnaps(String referentialKey, List<String> keys) {
    Map<String, Snap> out = new LinkedHashMap<>();
    if (keys == null || keys.isEmpty()) {
      return out;
    }
    for (int i = 0; i < keys.size(); i += 100) {
      List<String> chunk = keys.subList(i, Math.min(i + 100, keys.size()));
      String placeholders = String.join(",", java.util.Collections.nCopies(chunk.size(), "?"));
      List<Object> args = new ArrayList<>();
      args.add(referentialKey);
      args.addAll(chunk);
      jdbc.query(
          """
              SELECT natural_key, label, sync_fingerprint, actif
              FROM erp_clinic_referentials
              WHERE referential_key = ? AND natural_key IN ("""
              + placeholders + ")",
          rs -> {
            out.put(rs.getString("natural_key"), new Snap(
                rs.getString("label"),
                rs.getString("sync_fingerprint"),
                rs.getBoolean("actif")));
          },
          args.toArray());
    }
    return out;
  }

  public long insert(
      String referentialKey,
      String naturalKey,
      String label,
      String payloadJson,
      String fingerprint,
      boolean actif,
      String sourceSystem,
      String legacyKey,
      String syncStatus,
      Long userId) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          """
              INSERT INTO erp_clinic_referentials
                (referential_key, natural_key, label, payload_json, sync_fingerprint, actif,
                 source_system, legacy_key, sync_status, version, usercreateid)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
              """,
          Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, referentialKey);
      ps.setString(2, naturalKey);
      ps.setString(3, label);
      ps.setString(4, payloadJson);
      ps.setString(5, fingerprint);
      ps.setBoolean(6, actif);
      ps.setString(7, sourceSystem);
      ps.setString(8, legacyKey);
      ps.setString(9, syncStatus);
      if (userId == null) {
        ps.setObject(10, null);
      } else {
        ps.setLong(10, userId);
      }
      return ps;
    }, keys);
    Number key = keys.getKey();
    if (key == null) {
      throw new IllegalStateException("Identifiant ERP absent");
    }
    return key.longValue();
  }

  /**
   * @return 1 si créé, 0 si la clé existe déjà
   */
  public int insertIfAbsent(
      String referentialKey,
      String naturalKey,
      String label,
      String payloadJson,
      String fingerprint) {
    return jdbc.update(
        """
            INSERT INTO erp_clinic_referentials
              (referential_key, natural_key, label, payload_json, sync_fingerprint, actif,
               source_system, legacy_key, sync_status, version)
            SELECT ?, ?, ?, ?, ?, 1, 'IMPORT_CLINIQUE', ?, 'IMPORTED', 1
            FROM DUAL
            WHERE NOT EXISTS (
              SELECT 1 FROM erp_clinic_referentials
              WHERE referential_key = ? AND natural_key = ?
            )
            """,
        referentialKey,
        naturalKey,
        label,
        payloadJson,
        fingerprint,
        naturalKey,
        referentialKey,
        naturalKey);
  }

  public int updatePayload(
      String referentialKey,
      long id,
      String label,
      String payloadJson,
      String fingerprint,
      String syncStatus,
      int version,
      Long userId) {
    return jdbc.update(
        """
            UPDATE erp_clinic_referentials
            SET label = ?, payload_json = ?, sync_fingerprint = ?, sync_status = ?,
                version = version + 1, userupdateid = ?, dateupdate = NOW()
            WHERE referential_key = ? AND id = ? AND version = ?
            """,
        label,
        payloadJson,
        fingerprint,
        syncStatus,
        userId,
        referentialKey,
        id,
        version);
  }

  public int updateActif(String referentialKey, long id, boolean actif, int version, Long userId) {
    return jdbc.update(
        """
            UPDATE erp_clinic_referentials
            SET actif = ?, version = version + 1, userupdateid = ?, dateupdate = NOW()
            WHERE referential_key = ? AND id = ? AND version = ?
            """,
        actif,
        userId,
        referentialKey,
        id,
        version);
  }

  public void updateSyncStatus(long id, String syncStatus) {
    jdbc.update(
        "UPDATE erp_clinic_referentials SET sync_status = ?, dateupdate = NOW() WHERE id = ?",
        syncStatus,
        id);
  }

  public void markSynced(long id, String legacyKey) {
    jdbc.update(
        """
            UPDATE erp_clinic_referentials
            SET sync_status = 'SYNCED', legacy_key = ?, last_synced_at = NOW(), dateupdate = NOW()
            WHERE id = ?
            """,
        legacyKey,
        id);
  }

  public void enqueueOutbox(
      String operationId,
      String entityType,
      long entityId,
      String legacyKey,
      String operationType,
      String payloadJson,
      String correlationId,
      String status) {
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
        status,
        correlationId);
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

  public boolean hasBusyOutbox(long entityId, String entityType) {
    Long n = jdbc.queryForObject(
        """
            SELECT COUNT(*) FROM erp_sync_outbox
            WHERE entity_id = ? AND entity_type = ?
              AND status IN ('IN_PROGRESS', 'UNCERTAIN')
            """,
        Long.class,
        entityId,
        entityType);
    return n != null && n > 0;
  }

  public int replaceOpenPayload(long entityId, String entityType, String operationType, String payloadJson, String legacyKey) {
    return jdbc.update(
        """
            UPDATE erp_sync_outbox
            SET payload_json = ?, operation_type = ?, legacy_key = ?, dateupdate = NOW()
            WHERE entity_id = ? AND entity_type = ? AND status IN ('PENDING', 'SUSPENDED')
            """,
        payloadJson,
        operationType,
        legacyKey,
        entityId,
        entityType);
  }

  public Map<String, Long> outboxCounts(String entityType) {
    Map<String, Long> counts = new LinkedHashMap<>();
    jdbc.query(
        "SELECT status, COUNT(*) AS n FROM erp_sync_outbox WHERE entity_type = ? GROUP BY status",
        rs -> {
          counts.put(rs.getString("status"), rs.getLong("n"));
        },
        entityType);
    return counts;
  }

  private static void appendFilters(StringBuilder sql, List<Object> args, String q, Boolean actif) {
    if (actif != null) {
      sql.append(" AND actif = ?");
      args.add(actif ? 1 : 0);
    }
    if (q != null && !q.isBlank()) {
      String needle = "%" + q.trim().replace("%", "").replace("_", "") + "%";
      sql.append(" AND (natural_key LIKE ? OR label LIKE ?)");
      args.add(needle);
      args.add(needle);
    }
  }

  private static LocalDateTime toLdt(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  public record Snap(String label, String fingerprint, boolean actif) {
  }
}
