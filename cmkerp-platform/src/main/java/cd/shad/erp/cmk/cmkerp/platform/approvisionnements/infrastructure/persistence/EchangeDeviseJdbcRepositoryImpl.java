package cd.shad.erp.cmk.cmkerp.platform.approvisionnements.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.model.EchangeDevise;
import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.repository.EchangeDeviseRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC du repository EchangeDevise.
 */
@Repository
@Slf4j
public class EchangeDeviseJdbcRepositoryImpl extends AbstractJdbcRepository implements EchangeDeviseRepository {

    public EchangeDeviseJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<EchangeDevise> ECHANGE_DEVISE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return EchangeDevise.builder()
                .id(rs.getLong("id"))
                .monnaieprincipale(rs.getString("monnaieprincipale"))
                .tauxechange(rs.getObject("tauxechange", Float.class))
                .monnaieechange(rs.getString("monnaieechange"))
                .symbole(rs.getString("symbole"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<EchangeDevise> findById(Long id) {
        String sql = "SELECT id, monnaieprincipale, tauxechange, monnaieechange, symbole, datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM echange_devise WHERE id = ?";
        return queryForOptional(sql, ECHANGE_DEVISE_MAPPER, id);
    }

    @Override
    public List<EchangeDevise> findAll() {
        String sql = "SELECT id, monnaieprincipale, tauxechange, monnaieechange, symbole, datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM echange_devise ORDER BY monnaieechange";
        return jdbcTemplate.query(sql, ECHANGE_DEVISE_MAPPER);
    }
}

