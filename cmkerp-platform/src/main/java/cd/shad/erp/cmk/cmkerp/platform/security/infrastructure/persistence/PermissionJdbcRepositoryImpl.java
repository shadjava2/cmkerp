package cd.shad.erp.cmk.cmkerp.platform.security.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC du repository Permission.
 */
@Repository
@Slf4j
public class PermissionJdbcRepositoryImpl extends AbstractJdbcRepository implements PermissionRepository {

    public PermissionJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Permission> PERMISSION_MAPPER = (rs, rowNum) -> {
        Long parent = null;
        try {
            long raw = rs.getLong("parent");
            parent = rs.wasNull() ? 0L : raw;
        } catch (Exception ignored) {
            parent = 0L;
        }
        return Permission.builder()
            .id(rs.getLong("id"))
            .nom(rs.getString("nom"))
            .description(rs.getString("description"))
            .parent(parent)
            .dateCreate(rs.getTimestamp("datecreate") != null
                ? rs.getTimestamp("datecreate").toLocalDateTime()
                : null)
            .dateUpdate(rs.getTimestamp("dateupdate") != null
                ? rs.getTimestamp("dateupdate").toLocalDateTime()
                : null)
            .userCreatedId(rs.getLong("usercreateid"))
            .userUpdatedId(rs.getLong("userupdateid"))
            .build();
    };

    private static final String PROJECTION =
            "SELECT id, nom, description, parent, datecreate, dateupdate, usercreateid, userupdateid FROM permissions";

    @Override
    public Optional<Permission> findById(Long id) {
        return queryForOptional(PROJECTION + " WHERE id = ?", PERMISSION_MAPPER, id);
    }

    @Override
    public Optional<Permission> findByNom(String nom) {
        return queryForOptional(PROJECTION + " WHERE nom = ? LIMIT 1", PERMISSION_MAPPER, nom);
    }

    @Override
    public List<Permission> findAll() {
        return queryForList(PROJECTION + " ORDER BY parent ASC, id ASC", PERMISSION_MAPPER);
    }
}

