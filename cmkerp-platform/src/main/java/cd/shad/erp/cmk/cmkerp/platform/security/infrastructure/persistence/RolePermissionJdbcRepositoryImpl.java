package cd.shad.erp.cmk.cmkerp.platform.security.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC du repository RolePermission.
 */
@Repository
@Slf4j
public class RolePermissionJdbcRepositoryImpl extends AbstractJdbcRepository implements RolePermissionRepository {

    public RolePermissionJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<RolePermission> ROLE_PERMISSION_MAPPER = (rs, rowNum) -> RolePermission.builder()
            .id(rs.getLong("id"))
            .fkRole(rs.getLong("fkRole"))
            .fkPermission(rs.getLong("fkPermission"))
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
    public Optional<RolePermission> findById(Long id) {
        String sql = "SELECT * FROM roles_permissions WHERE id = ?";
        return queryForOptional(sql, ROLE_PERMISSION_MAPPER, id);
    }

    @Override
    public List<RolePermission> findByRole(Long roleId) {
        // Facebook-Grade: Utilise l'index idx_roles_permissions_fkRole (créé dans V4) pour performance optimale
        // Projection explicite pour éviter SELECT * (meilleure performance)
        String sql = "SELECT id, fkRole, fkPermission, datecreate, dateupdate, usercreateid, userupdateid " +
                "FROM roles_permissions WHERE fkRole = ?";
        return queryForList(sql, ROLE_PERMISSION_MAPPER, roleId);
    }

    @Override
    public List<RolePermission> findByPermission(Long permissionId) {
        // Facebook-Grade: Utilise l'index idx_roles_permissions_fkPermission (créé dans V4) pour performance optimale
        // Projection explicite pour éviter SELECT * (meilleure performance)
        String sql = "SELECT id, fkRole, fkPermission, datecreate, dateupdate, usercreateid, userupdateid " +
                "FROM roles_permissions WHERE fkPermission = ?";
        return queryForList(sql, ROLE_PERMISSION_MAPPER, permissionId);
    }

    @Override
    public Optional<RolePermission> findByRoleAndPermission(Long roleId, Long permissionId) {
        // Facebook-Grade: Utilise l'index composite existant (index_roleperimis_unique) pour performance optimale
        // Projection explicite pour éviter SELECT * (meilleure performance)
        String sql = "SELECT id, fkRole, fkPermission, datecreate, dateupdate, usercreateid, userupdateid " +
                "FROM roles_permissions WHERE fkRole = ? AND fkPermission = ? LIMIT 1";
        return queryForOptional(sql, ROLE_PERMISSION_MAPPER, roleId, permissionId);
    }

    @Override
    public int save(RolePermission rolePermission) {
        String sql = "INSERT INTO roles_permissions (fkRole, fkPermission, datecreate, usercreateid) VALUES (?, ?, ?, ?)";

        LocalDateTime now = rolePermission.getDateCreate() != null ? rolePermission.getDateCreate() : LocalDateTime.now();

        return update(sql,
            rolePermission.getFkRole(),
            rolePermission.getFkPermission(),
            java.sql.Timestamp.valueOf(now),
            rolePermission.getUserCreatedId());
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM roles_permissions WHERE id = ?";
        return update(sql, id);
    }

    @Override
    public int deleteByRoleAndPermission(Long roleId, Long permissionId) {
        String sql = "DELETE FROM roles_permissions WHERE fkRole = ? AND fkPermission = ?";
        return update(sql, roleId, permissionId);
    }
}

