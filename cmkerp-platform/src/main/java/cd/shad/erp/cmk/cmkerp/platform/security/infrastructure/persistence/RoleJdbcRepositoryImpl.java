package cd.shad.erp.cmk.cmkerp.platform.security.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
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
 * Implémentation JDBC du repository Role.
 *
 * <p>Cette classe implémente l'interface RoleRepository (port du domaine)
 * en utilisant JDBC pour accéder à la base de données.
 *
 * <p>Elle manipule l'agrégat Role du domaine Security.
 */
@Repository
@Slf4j
public class RoleJdbcRepositoryImpl extends AbstractJdbcRepository implements RoleRepository {

    public RoleJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Role> ROLE_MAPPER = (rs, rowNum) -> Role.builder()
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
    public Optional<Role> findById(Long id) {
        String sql = "SELECT * FROM roles WHERE id = ?";
        return queryForOptional(sql, ROLE_MAPPER, id);
    }

    @Override
    public Optional<Role> findByNom(String nom) {
        String sql = "SELECT * FROM roles WHERE nom = ?";
        return queryForOptional(sql, ROLE_MAPPER, nom);
    }

    @Override
    public List<Role> findAll() {
        String sql = "SELECT * FROM roles ORDER BY id";
        return queryForList(sql, ROLE_MAPPER);
    }

    @Override
    public int save(Role role) {
        String sql = "INSERT INTO roles (nom, description, datecreate, usercreateid) VALUES (?, ?, ?, ?)";

        LocalDateTime now = role.getDateCreate() != null ? role.getDateCreate() : LocalDateTime.now();

        return update(sql,
            role.getNom(),
            role.getDescription(),
            java.sql.Timestamp.valueOf(now),
            role.getUserCreatedId());
    }

    @Override
    public int update(Role role) {
        String sql = "UPDATE roles SET nom = ?, description = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";

        LocalDateTime now = role.getDateUpdate() != null ? role.getDateUpdate() : LocalDateTime.now();

        return update(sql,
            role.getNom(),
            role.getDescription(),
            java.sql.Timestamp.valueOf(now),
            role.getUserUpdatedId(),
            role.getId());
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM roles WHERE id = ?";
        return update(sql, id);
    }
}

