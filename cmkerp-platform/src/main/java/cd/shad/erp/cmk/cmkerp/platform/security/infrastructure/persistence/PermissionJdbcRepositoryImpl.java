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

    private static final RowMapper<Permission> PERMISSION_MAPPER = (rs, rowNum) -> Permission.builder()
            .id(rs.getLong("id"))
            .nom(rs.getString("nom"))
            .description(rs.getString("description"))
            .dateCreate(rs.getTimestamp("datecreate") != null
                ? rs.getTimestamp("datecreate").toLocalDateTime()
                : null)
            .dateUpdate(rs.getTimestamp("dateupdate") != null
                ? rs.getTimestamp("dateupdate").toLocalDateTime()
                : null)
            .userCreatedId(rs.getLong("usercreateid"))
            .userUpdatedId(rs.getLong("userupdateid"))
            .build();

    @Override
    public Optional<Permission> findById(Long id) {
        // Facebook-Grade: Projection explicite pour éviter SELECT * (meilleure performance)
        String sql = "SELECT id, nom, description, datecreate, dateupdate, usercreateid, userupdateid " +
                "FROM permissions WHERE id = ?";
        return queryForOptional(sql, PERMISSION_MAPPER, id);
    }

    @Override
    public Optional<Permission> findByNom(String nom) {
        // Facebook-Grade: Utilise l'index UNIQUE sur nom (uniquepermission) pour performance optimale
        // Projection explicite pour éviter SELECT * (meilleure performance)
        String sql = "SELECT id, nom, description, datecreate, dateupdate, usercreateid, userupdateid " +
                "FROM permissions WHERE nom = ? LIMIT 1";
        return queryForOptional(sql, PERMISSION_MAPPER, nom);
    }

    @Override
    public List<Permission> findAll() {
        // Facebook-Grade: Projection explicite + ORDER BY id utilise l'index primaire (clustered index)
        String sql = "SELECT id, nom, description, datecreate, dateupdate, usercreateid, userupdateid " +
                "FROM permissions ORDER BY id";
        return queryForList(sql, PERMISSION_MAPPER);
    }
}

