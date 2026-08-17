package cd.shad.erp.cmk.cmkerp.pos.transferts.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository.LigneTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC du repository LigneTransfertInterne (module POS).
 */
@Repository("posLigneTransfertInterneJdbcRepositoryImpl")
@Slf4j
public class LigneTransfertInterneJdbcRepositoryImpl extends AbstractJdbcRepository implements LigneTransfertInterneRepository {

    public LigneTransfertInterneJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<LigneTransfertInterne> LIGNE_TRANSFERT_INTERNE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return LigneTransfertInterne.builder()
                .id(rs.getLong("id"))
                .fkTransfertInterne(rs.getLong("fkTransfertInterne"))
                .fkStock(rs.getLong("fkStock"))
                .fkAlertePeremption(rs.getObject("fkAlertePeremption", Long.class))
                .quantite(rs.getObject("quantite", Float.class))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<LigneTransfertInterne> findById(Long id) {
        String sql = "SELECT id, fkTransfertInterne, fkStock, fkAlertePeremption, quantite, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM lignes_transfert_interne WHERE id = ?";
        return queryForOptional(sql, LIGNE_TRANSFERT_INTERNE_MAPPER, id);
    }

    @Override
    public List<LigneTransfertInterne> findByFkTransfertInterne(Long fkTransfertInterne) {
        String sql = "SELECT id, fkTransfertInterne, fkStock, fkAlertePeremption, quantite, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM lignes_transfert_interne WHERE fkTransfertInterne = ? ORDER BY datecreate";
        return jdbcTemplate.query(sql, LIGNE_TRANSFERT_INTERNE_MAPPER, fkTransfertInterne);
    }

    @Override
    public int save(LigneTransfertInterne ligneTransfertInterne) {
        String sql = "INSERT INTO lignes_transfert_interne (fkTransfertInterne, fkStock, fkAlertePeremption, quantite, "
                + "datecreate, usercreateid) VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, ligneTransfertInterne.getFkTransfertInterne());
            ps.setLong(2, ligneTransfertInterne.getFkStock());
            if (ligneTransfertInterne.getFkAlertePeremption() != null) {
                ps.setLong(3, ligneTransfertInterne.getFkAlertePeremption());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            if (ligneTransfertInterne.getQuantite() != null) {
                ps.setFloat(4, ligneTransfertInterne.getQuantite());
            } else {
                ps.setNull(4, java.sql.Types.FLOAT);
            }
            ps.setTimestamp(5, Timestamp.valueOf(ligneTransfertInterne.getDateCreate() != null ? ligneTransfertInterne.getDateCreate() : LocalDateTime.now()));
            if (ligneTransfertInterne.getUserCreatedId() != null) {
                ps.setLong(6, ligneTransfertInterne.getUserCreatedId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        // Récupérer l'ID généré et l'assigner à l'objet
        if (rowsAffected > 0 && keyHolder.getKey() != null) {
            Long generatedId = keyHolder.getKey().longValue();
            ligneTransfertInterne.setId(generatedId);
            log.debug("ID généré pour la ligne de transfert interne: {}", generatedId);
        }

        return rowsAffected;
    }

    @Override
    public int update(LigneTransfertInterne ligneTransfertInterne) {
        String sql = "UPDATE lignes_transfert_interne SET fkTransfertInterne = ?, fkStock = ?, fkAlertePeremption = ?, quantite = ?, "
                + "dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                ligneTransfertInterne.getFkTransfertInterne(),
                ligneTransfertInterne.getFkStock(),
                ligneTransfertInterne.getFkAlertePeremption(),
                ligneTransfertInterne.getQuantite(),
                Timestamp.valueOf(ligneTransfertInterne.getDateUpdate() != null ? ligneTransfertInterne.getDateUpdate() : LocalDateTime.now()),
                ligneTransfertInterne.getUserUpdatedId(),
                ligneTransfertInterne.getId());
    }

    @Override
    public int delete(Long id) {
        String sql = "DELETE FROM lignes_transfert_interne WHERE id = ?";
        return update(sql, id);
    }
}

