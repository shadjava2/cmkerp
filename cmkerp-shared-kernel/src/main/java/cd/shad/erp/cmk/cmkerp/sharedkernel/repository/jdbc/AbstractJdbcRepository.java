package cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Classe de base abstraite pour les repositories JDBC utilisant la datasource primaire.
 */
public abstract class AbstractJdbcRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final NamedParameterJdbcTemplate namedJdbcTemplate;
    protected AbstractJdbcRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.namedJdbcTemplate = Objects.requireNonNull(namedJdbcTemplate, "namedJdbcTemplate must not be null");
    }

    protected <T> Optional<T> queryForOptional(String sql, RowMapper<T> mapper, Object... args) {
        Objects.requireNonNull(sql, "sql must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");

        T single = jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapper.mapRow(rs, 0);
            }
            return null;
        }, args);

        return Optional.ofNullable(single);
    }

    protected <T> List<T> queryForList(String sql, RowMapper<T> mapper, Object... args) {
        Objects.requireNonNull(sql, "sql must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        return jdbcTemplate.query(sql, mapper, args);
    }

    protected boolean exists(String sql, Object... args) {
        Objects.requireNonNull(sql, "sql must not be null");
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    protected int update(String sql, Object... args) {
        Objects.requireNonNull(sql, "sql must not be null");
        return jdbcTemplate.update(sql, args);
    }

    protected int updateRequired(String sql, Object... args) {
        int rows = update(sql, args);
        if (rows == 0) {
            throw new RuntimeException("No rows affected by update: " + sql);
        }
        return rows;
    }
}

