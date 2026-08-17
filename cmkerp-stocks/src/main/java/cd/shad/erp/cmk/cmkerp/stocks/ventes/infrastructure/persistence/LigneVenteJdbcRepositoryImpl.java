package cd.shad.erp.cmk.cmkerp.stocks.ventes.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.LigneVente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.LigneVenteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Implémentation JDBC du repository LigneVente.
 */
@Repository
@Slf4j
public class LigneVenteJdbcRepositoryImpl extends AbstractJdbcRepository implements LigneVenteRepository {

    public LigneVenteJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<LigneVente> LIGNE_VENTE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return LigneVente.builder()
                .id(rs.getLong("id"))
                .fkVente(rs.getLong("fkVente"))
                .fkStock(rs.getObject("fkStock", Long.class))
                .qt(rs.getObject("qt", Float.class))
                .prixventes(rs.getBigDecimal("prixventes"))
                .horsconvention(rs.getObject("horsconvention", Integer.class))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<LigneVente> findById(Long id) {
        String sql = "SELECT id, fkVente, fkStock, qt, prixventes, horsconvention, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM lignes_vente WHERE id = ?";
        return queryForOptional(sql, LIGNE_VENTE_MAPPER, id);
    }

    @Override
    public List<LigneVente> findByFkVente(Long fkVente) {
        String sql = "SELECT id, fkVente, fkStock, qt, prixventes, horsconvention, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM lignes_vente WHERE fkVente = ? ORDER BY datecreate";
        return jdbcTemplate.query(sql, LIGNE_VENTE_MAPPER, fkVente);
    }

    @Override
    public int save(LigneVente ligneVente) {
        String sql = "INSERT INTO lignes_vente (fkVente, fkStock, qt, prixventes, horsconvention, "
                + "datecreate, dateupdate, usercreateid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateCreate = ligneVente.getDateCreate() != null ? ligneVente.getDateCreate() : now;
        LocalDateTime dateUpdate = ligneVente.getDateUpdate() != null ? ligneVente.getDateUpdate() : dateCreate;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, ligneVente.getFkVente());
            if (ligneVente.getFkStock() != null) {
                ps.setLong(2, ligneVente.getFkStock());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            if (ligneVente.getQt() != null) {
                ps.setFloat(3, ligneVente.getQt());
            } else {
                ps.setNull(3, java.sql.Types.FLOAT);
            }
            if (ligneVente.getPrixventes() != null) {
                ps.setBigDecimal(4, ligneVente.getPrixventes());
            } else {
                ps.setNull(4, java.sql.Types.DECIMAL);
            }
            ps.setInt(5, ligneVente.getHorsconvention() != null ? ligneVente.getHorsconvention() : 0);
            ps.setTimestamp(6, Timestamp.valueOf(dateCreate));
            ps.setTimestamp(7, Timestamp.valueOf(dateUpdate));
            if (ligneVente.getUserCreatedId() != null) {
                ps.setLong(8, ligneVente.getUserCreatedId());
            } else {
                ps.setNull(8, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        // Récupérer l'ID généré et l'assigner à l'objet
        if (rowsAffected > 0 && keyHolder.getKey() != null) {
            Long generatedId = keyHolder.getKey().longValue();
            ligneVente.setId(generatedId);
            ligneVente.setDateCreate(dateCreate);
            ligneVente.setDateUpdate(dateUpdate);
            log.debug("ID généré pour la ligne de vente: {}", generatedId);
        }

        return rowsAffected;
    }

    @Override
    public int update(LigneVente ligneVente) {
        String sql = "UPDATE lignes_vente SET fkVente = ?, fkStock = ?, qt = ?, prixventes = ?, "
                + "horsconvention = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                ligneVente.getFkVente(),
                ligneVente.getFkStock(),
                ligneVente.getQt(),
                ligneVente.getPrixventes(),
                ligneVente.getHorsconvention() != null ? ligneVente.getHorsconvention() : 0,
                Timestamp.valueOf(ligneVente.getDateUpdate() != null ? ligneVente.getDateUpdate() : LocalDateTime.now()),
                ligneVente.getUserUpdatedId(),
                ligneVente.getId());
    }

    @Override
    public int delete(Long id) {
        String sql = "DELETE FROM lignes_vente WHERE id = ?";
        return update(sql, id);
    }
}

