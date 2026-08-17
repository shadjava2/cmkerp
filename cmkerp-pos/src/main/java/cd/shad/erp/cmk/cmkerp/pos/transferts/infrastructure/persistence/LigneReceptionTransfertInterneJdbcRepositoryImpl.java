package cd.shad.erp.cmk.cmkerp.pos.transferts.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository.LigneReceptionTransfertInterneRepository;
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
 * Implémentation JDBC du repository LigneReceptionTransfertInterne (module POS).
 */
@Repository("posLigneReceptionTransfertInterneJdbcRepositoryImpl")
@Slf4j
public class LigneReceptionTransfertInterneJdbcRepositoryImpl extends AbstractJdbcRepository implements LigneReceptionTransfertInterneRepository {

    public LigneReceptionTransfertInterneJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<LigneReceptionTransfertInterne> LIGNE_RECEPTION_TRANSFERT_INTERNE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        // Utiliser fkReceptionStock (nom de colonne dans la base de données)
        // mais mapper vers fkReceptionTransfertInterne dans l'entité Java
        Long fkReceptionId = null;
        try {
            // Essayer d'abord fkReceptionStock (nom réel dans la DB)
            fkReceptionId = rs.getLong("fkReceptionStock");
        } catch (Exception e) {
            // Fallback vers fkReceptionTransfertInterne si la colonne a été renommée
            try {
                fkReceptionId = rs.getLong("fkReceptionTransfertInterne");
            } catch (Exception e2) {
                log.warn("Impossible de lire fkReceptionStock ou fkReceptionTransfertInterne");
            }
        }

        return LigneReceptionTransfertInterne.builder()
                .id(rs.getLong("id"))
                .fkReceptionTransfertInterne(fkReceptionId)
                .fkStock(rs.getLong("fkStock"))
                .fkAlertePeremption(rs.getObject("fkAlertePeremption", Long.class))
                .quantiteDemandee(rs.getObject("quantiteDemandee", Float.class))
                .quantiteTransferee(rs.getObject("quantiteTransferee", Float.class))
                .quantite(rs.getObject("quantite", Float.class))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<LigneReceptionTransfertInterne> findById(Long id) {
        // Utiliser fkReceptionStock (nom réel dans la base de données)
        String sql = "SELECT id, fkReceptionStock, fkStock, fkAlertePeremption, quantiteDemandee, quantiteTransferee, quantite, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM lignes_reception_transfert_interne WHERE id = ?";
        return queryForOptional(sql, LIGNE_RECEPTION_TRANSFERT_INTERNE_MAPPER, id);
    }

    @Override
    public List<LigneReceptionTransfertInterne> findByFkReceptionTransfertInterne(Long fkReceptionTransfertInterne) {
        // Utiliser fkReceptionStock (nom réel dans la base de données)
        String sql = "SELECT id, fkReceptionStock, fkStock, fkAlertePeremption, quantiteDemandee, quantiteTransferee, quantite, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM lignes_reception_transfert_interne WHERE fkReceptionStock = ? ORDER BY datecreate";
        return jdbcTemplate.query(sql, LIGNE_RECEPTION_TRANSFERT_INTERNE_MAPPER, fkReceptionTransfertInterne);
    }

    @Override
    public int save(LigneReceptionTransfertInterne ligneReceptionTransfertInterne) {
        // Utiliser fkReceptionStock (nom réel dans la base de données)
        String sql = "INSERT INTO lignes_reception_transfert_interne (fkReceptionStock, fkStock, fkAlertePeremption, quantiteDemandee, quantiteTransferee, quantite, "
                + "datecreate, usercreateid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, ligneReceptionTransfertInterne.getFkReceptionTransfertInterne());
            ps.setLong(2, ligneReceptionTransfertInterne.getFkStock());
            if (ligneReceptionTransfertInterne.getFkAlertePeremption() != null) {
                ps.setLong(3, ligneReceptionTransfertInterne.getFkAlertePeremption());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            if (ligneReceptionTransfertInterne.getQuantiteDemandee() != null) {
                ps.setFloat(4, ligneReceptionTransfertInterne.getQuantiteDemandee());
            } else {
                ps.setNull(4, java.sql.Types.FLOAT);
            }
            if (ligneReceptionTransfertInterne.getQuantiteTransferee() != null) {
                ps.setFloat(5, ligneReceptionTransfertInterne.getQuantiteTransferee());
            } else {
                ps.setNull(5, java.sql.Types.FLOAT);
            }
            if (ligneReceptionTransfertInterne.getQuantite() != null) {
                ps.setFloat(6, ligneReceptionTransfertInterne.getQuantite());
            } else {
                ps.setNull(6, java.sql.Types.FLOAT);
            }
            ps.setTimestamp(7, Timestamp.valueOf(ligneReceptionTransfertInterne.getDateCreate() != null ? ligneReceptionTransfertInterne.getDateCreate() : LocalDateTime.now()));
            if (ligneReceptionTransfertInterne.getUserCreatedId() != null) {
                ps.setLong(8, ligneReceptionTransfertInterne.getUserCreatedId());
            } else {
                ps.setNull(8, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        // Récupérer l'ID généré et l'assigner à l'objet
        if (rowsAffected > 0 && keyHolder.getKey() != null) {
            Long generatedId = keyHolder.getKey().longValue();
            ligneReceptionTransfertInterne.setId(generatedId);
            log.debug("ID généré pour la ligne de réception de transfert interne: {}", generatedId);
        }

        return rowsAffected;
    }

    @Override
    public int update(LigneReceptionTransfertInterne ligneReceptionTransfertInterne) {
        String sql = "UPDATE lignes_reception_transfert_interne SET quantite = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                ligneReceptionTransfertInterne.getQuantite(),
                Timestamp.valueOf(ligneReceptionTransfertInterne.getDateUpdate() != null ? ligneReceptionTransfertInterne.getDateUpdate() : LocalDateTime.now()),
                ligneReceptionTransfertInterne.getUserUpdatedId(),
                ligneReceptionTransfertInterne.getId());
    }

    @Override
    public int delete(Long id) {
        String sql = "DELETE FROM lignes_reception_transfert_interne WHERE id = ?";
        return update(sql, id);
    }

    @Override
    public int deleteByFkReceptionTransfertInterne(Long fkReceptionTransfertInterne) {
        // Utiliser fkReceptionStock (nom réel dans la base de données)
        String sql = "DELETE FROM lignes_reception_transfert_interne WHERE fkReceptionStock = ?";
        return update(sql, fkReceptionTransfertInterne);
    }
}

