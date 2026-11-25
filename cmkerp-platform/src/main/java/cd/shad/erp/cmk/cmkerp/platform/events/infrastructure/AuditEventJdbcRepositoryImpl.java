package cd.shad.erp.cmk.cmkerp.platform.events.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.RolePermissionAuditEntry;
import cd.shad.erp.cmk.cmkerp.platform.events.AuditEvent;
import cd.shad.erp.cmk.cmkerp.platform.events.AuditEventRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Implémentation JDBC du repository AuditEvent.
 */
@Repository
@Slf4j
public class AuditEventJdbcRepositoryImpl extends AbstractJdbcRepository implements AuditEventRepository {

    public AuditEventJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    @Override
    public int save(AuditEvent event) {
        String sql = "INSERT INTO audit_events (event_id, event_type, event_timestamp, user_id, username, action, resource_type, resource_id, details) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Convertir Instant en java.sql.Timestamp pour MySQL
        Instant timestamp = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();
        java.sql.Timestamp sqlTimestamp = java.sql.Timestamp.from(timestamp);

        try {
            int rowsAffected = update(sql,
                    event.getEventId(),
                    event.getEventType(),
                    sqlTimestamp,
                    event.getUserId(),
                    event.getUsername(),
                    event.getAction(),
                    event.getResourceType(),
                    event.getResourceId(),
                    event.getDetails());

            if (rowsAffected > 0) {
                log.debug("Événement d'audit persisté avec succès: eventId={}, userId={}, action={}",
                        event.getEventId(), event.getUserId(), event.getAction());
            }

            return rowsAffected;
        } catch (Exception e) {
            log.error("Erreur lors de la persistance de l'événement d'audit: eventId={}, userId={}, action={}",
                    event.getEventId(), event.getUserId(), event.getAction(), e);
            throw new RuntimeException("Échec de la persistance de l'événement d'audit", e);
        }
    }

    @Override
    public List<RolePermissionAuditEntry> findRolePermissionChanges(Long roleId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = """
                SELECT event_id, event_timestamp, user_id, username, action, resource_id, details
                FROM audit_events
                WHERE resource_type = 'ROLE'
                  AND resource_id = ?
                  AND action = 'ROLE_PERMISSIONS_UPDATE'
                ORDER BY event_timestamp DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("event_timestamp");
            return RolePermissionAuditEntry.builder()
                    .eventId(rs.getString("event_id"))
                    .timestamp(ts != null ? ts.toInstant() : null)
                    .actorUserId(rs.getObject("user_id") != null ? rs.getLong("user_id") : null)
                    .actorUsername(rs.getString("username"))
                    .action(rs.getString("action"))
                    .roleId(rs.getObject("resource_id") != null ? rs.getLong("resource_id") : null)
                    .details(rs.getString("details"))
                    .build();
        }, roleId, safeLimit);
    }
}

