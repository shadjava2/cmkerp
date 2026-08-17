package cd.shad.erp.cmk.cmkerp.platform.pharmacie.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model.Pharmacie;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository.PharmacieRepository;
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
 * Implémentation JDBC du repository Pharmacie.
 */
@Repository
@Slf4j
public class PharmacieJdbcRepositoryImpl extends AbstractJdbcRepository implements PharmacieRepository {

    public PharmacieJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Pharmacie> PHARMACIE_MAPPER = (rs, rowNum) -> Pharmacie.builder()
            .id(rs.getLong("id"))
            .fkSite(rs.getLong("fkSite"))
            .designation(rs.getString("designation"))
            .typePharmacie(rs.getString("typepharmacie"))
            .codeimmo(rs.getString("codeimmo"))
            .typeHospi(rs.getString("typehospi"))
            .dateCreate(rs.getTimestamp("datecreate") != null
                ? rs.getTimestamp("datecreate").toLocalDateTime()
                : null)
            .dateUpdate(rs.getTimestamp("dateupdate") != null
                ? rs.getTimestamp("dateupdate").toLocalDateTime()
                : null)
            .userCreatedId(rs.getLong("usercreatedid"))
            .userUpdatedId(rs.getLong("userupdateid"))
            .build();

    @Override
    public Optional<Pharmacie> findById(Long id) {
        String sql = "SELECT * FROM pharmacies WHERE id = ?";
        return queryForOptional(sql, PHARMACIE_MAPPER, id);
    }

    @Override
    public Optional<Pharmacie> findByCodeImmo(String codeImmo) {
        String sql = "SELECT * FROM pharmacies WHERE codeimmo = ?";
        return queryForOptional(sql, PHARMACIE_MAPPER, codeImmo);
    }

    @Override
    public List<Pharmacie> findBySite(Long siteId) {
        String sql = "SELECT * FROM pharmacies WHERE fkSite = ?";
        return queryForList(sql, PHARMACIE_MAPPER, siteId);
    }

    @Override
    public List<Pharmacie> findAll() {
        String sql = "SELECT * FROM pharmacies ORDER BY id";
        return queryForList(sql, PHARMACIE_MAPPER);
    }

    @Override
    public int save(Pharmacie pharmacie) {
        String sql = "INSERT INTO pharmacies (fkSite, designation, typepharmacie, codeimmo, typehospi, datecreate, usercreatedid) VALUES (?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime now = pharmacie.getDateCreate() != null ? pharmacie.getDateCreate() : LocalDateTime.now();

        return update(sql,
            pharmacie.getFkSite(),
            pharmacie.getDesignation(),
            pharmacie.getTypePharmacie(),
            pharmacie.getCodeimmo(),
            pharmacie.getTypeHospi(),
            java.sql.Timestamp.valueOf(now),
            pharmacie.getUserCreatedId());
    }

    @Override
    public int update(Pharmacie pharmacie) {
        String sql = "UPDATE pharmacies SET fkSite = ?, designation = ?, typepharmacie = ?, codeimmo = ?, typehospi = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";

        LocalDateTime now = pharmacie.getDateUpdate() != null ? pharmacie.getDateUpdate() : LocalDateTime.now();

        return update(sql,
            pharmacie.getFkSite(),
            pharmacie.getDesignation(),
            pharmacie.getTypePharmacie(),
            pharmacie.getCodeimmo(),
            pharmacie.getTypeHospi(),
            java.sql.Timestamp.valueOf(now),
            pharmacie.getUserUpdatedId(),
            pharmacie.getId());
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM pharmacies WHERE id = ?";
        return update(sql, id);
    }
}

