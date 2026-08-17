package cd.shad.erp.cmk.cmkerp.platform.site.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.site.domain.model.Site;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.repository.SiteRepository;
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
 * Implémentation JDBC du repository Site.
 */
@Repository
@Slf4j
public class SiteJdbcRepositoryImpl extends AbstractJdbcRepository implements SiteRepository {

    public SiteJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Site> SITE_MAPPER = (rs, rowNum) -> Site.builder()
            .id(rs.getLong("id"))
            .designation(rs.getString("designation"))
            .abbreviation(rs.getString("abbreviation"))
            .adresse(rs.getString("addresse"))
            .bloquer(rs.getBoolean("bloquer"))
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
    public Optional<Site> findById(Long id) {
        String sql = "SELECT * FROM sites WHERE id = ?";
        return queryForOptional(sql, SITE_MAPPER, id);
    }

    @Override
    public Optional<Site> findByDesignation(String designation) {
        String sql = "SELECT * FROM sites WHERE designation = ?";
        return queryForOptional(sql, SITE_MAPPER, designation);
    }

    @Override
    public List<Site> findAll() {
        String sql = "SELECT * FROM sites ORDER BY id";
        return queryForList(sql, SITE_MAPPER);
    }

    @Override
    public int save(Site site) {
        String sql = "INSERT INTO sites (designation, abbreviation, addresse, bloquer, datecreate, usercreateid) VALUES (?, ?, ?, ?, ?, ?)";

        LocalDateTime now = site.getDateCreate() != null ? site.getDateCreate() : LocalDateTime.now();

        return update(sql,
            site.getDesignation(),
            site.getAbbreviation(),
            site.getAdresse(),
            site.getBloquer() != null ? site.getBloquer() : false,
            java.sql.Timestamp.valueOf(now),
            site.getUserCreatedId());
    }

    @Override
    public int update(Site site) {
        String sql = "UPDATE sites SET designation = ?, abbreviation = ?, addresse = ?, bloquer = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";

        LocalDateTime now = site.getDateUpdate() != null ? site.getDateUpdate() : LocalDateTime.now();

        return update(sql,
            site.getDesignation(),
            site.getAbbreviation(),
            site.getAdresse(),
            site.getBloquer(),
            java.sql.Timestamp.valueOf(now),
            site.getUserUpdatedId(),
            site.getId());
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM sites WHERE id = ?";
        return update(sql, id);
    }
}

