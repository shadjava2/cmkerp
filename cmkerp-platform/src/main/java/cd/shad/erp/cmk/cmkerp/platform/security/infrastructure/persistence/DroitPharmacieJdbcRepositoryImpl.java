package cd.shad.erp.cmk.cmkerp.platform.security.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.DroitPharmacie;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.DroitPharmacieRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC du repository DroitPharmacie.
 */
@Repository
@Slf4j
public class DroitPharmacieJdbcRepositoryImpl extends AbstractJdbcRepository implements DroitPharmacieRepository {

    public DroitPharmacieJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    /**
     * Colonnes réelles de {@code droits_pharmacies} (V1__baseline_cmkerp_schema.sql).
     * Pas de dateupdate / userupdateid sur cette table.
     */
    private static final String SELECT_COLUMNS =
            "id, fkUtilisateur, fkPharmacie, datecreate, usercreateid";

    private static DroitPharmacie mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        return DroitPharmacie.builder()
                .id(rs.getLong("id"))
                .fkUtilisateur(rs.getLong("fkUtilisateur"))
                .fkPharmacie(rs.getLong("fkPharmacie"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(null)
                .build();
    }

    private static final RowMapper<DroitPharmacie> DROIT_PHARMACIE_MAPPER = DroitPharmacieJdbcRepositoryImpl::mapRow;

    @Override
    public Optional<DroitPharmacie> findById(Long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM droits_pharmacies WHERE id = ?";
        return queryForOptional(sql, DROIT_PHARMACIE_MAPPER, id);
    }

    @Override
    public List<DroitPharmacie> findByUtilisateur(Long utilisateurId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM droits_pharmacies WHERE fkUtilisateur = ?";
        return queryForList(sql, DROIT_PHARMACIE_MAPPER, utilisateurId);
    }

    @Override
    public List<DroitPharmacie> findByPharmacie(Long pharmacieId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM droits_pharmacies WHERE fkPharmacie = ?";
        return queryForList(sql, DROIT_PHARMACIE_MAPPER, pharmacieId);
    }

    @Override
    public Optional<DroitPharmacie> findByUtilisateurAndPharmacie(Long utilisateurId, Long pharmacieId) {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM droits_pharmacies WHERE fkUtilisateur = ? AND fkPharmacie = ?";
        return queryForOptional(sql, DROIT_PHARMACIE_MAPPER, utilisateurId, pharmacieId);
    }

    @Override
    public int save(DroitPharmacie droitPharmacie) {
        String sql = "INSERT INTO droits_pharmacies (fkUtilisateur, fkPharmacie, datecreate, usercreateid) VALUES (?, ?, ?, ?)";

        LocalDateTime now = droitPharmacie.getDateCreate() != null ? droitPharmacie.getDateCreate() : LocalDateTime.now();

        return update(sql,
            droitPharmacie.getFkUtilisateur(),
            droitPharmacie.getFkPharmacie(),
            java.sql.Timestamp.valueOf(now),
            droitPharmacie.getUserCreatedId());
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM droits_pharmacies WHERE id = ?";
        return update(sql, id);
    }

    @Override
    public int deleteByUtilisateurAndPharmacie(Long utilisateurId, Long pharmacieId) {
        String sql = "DELETE FROM droits_pharmacies WHERE fkUtilisateur = ? AND fkPharmacie = ?";
        return update(sql, utilisateurId, pharmacieId);
    }
}

