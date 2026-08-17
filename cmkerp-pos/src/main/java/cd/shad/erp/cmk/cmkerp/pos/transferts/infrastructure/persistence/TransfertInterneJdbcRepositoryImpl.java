package cd.shad.erp.cmk.cmkerp.pos.transferts.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository.TransfertInterneRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC du repository TransfertInterne (module POS).
 */
@Repository("posTransfertInterneJdbcRepositoryImpl")
@Slf4j
public class TransfertInterneJdbcRepositoryImpl extends AbstractJdbcRepository implements TransfertInterneRepository {

    public TransfertInterneJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<TransfertInterne> TRANSFERT_INTERNE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return TransfertInterne.builder()
                .id(rs.getLong("id"))
                .fkPharmacieSource(rs.getLong("fkPharmacieSource"))
                .fkPharmacieDestination(rs.getLong("fkPharmacieDestination"))
                .statut(convertStatutFromDatabase(rs.getString("statut")))
                .commentaire(rs.getString("commentaire"))
                .perime(rs.getObject("perime", Boolean.class))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<TransfertInterne> findById(Long id) {
        String sql = "SELECT id, fkPharmacieSource, fkPharmacieDestination, statut, commentaire, perime, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM transfert_interne WHERE id = ?";
        return queryForOptional(sql, TRANSFERT_INTERNE_MAPPER, id);
    }

    @Override
    public List<TransfertInterne> findAll(int offset, int limit, Long fkPharmacieSource, Long fkPharmacieDestination,
            String statut, String searchText) {
        StringBuilder sql = new StringBuilder("SELECT id, fkPharmacieSource, fkPharmacieDestination, statut, commentaire, perime, ")
                .append("datecreate, dateupdate, usercreateid, userupdateid ")
                .append("FROM transfert_interne WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (fkPharmacieSource != null) {
            sql.append(" AND fkPharmacieSource = ?");
            params.add(fkPharmacieSource);
        }

        if (fkPharmacieDestination != null) {
            sql.append(" AND fkPharmacieDestination = ?");
            params.add(fkPharmacieDestination);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }

        // Recherche par commentaire (recherche partielle, insensible à la casse)
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (LOWER(commentaire) LIKE LOWER(?) OR CAST(id AS CHAR) LIKE ?)");
            String searchPattern = "%" + searchText.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), TRANSFERT_INTERNE_MAPPER, params.toArray());
    }

    @Override
    public long count(Long fkPharmacieSource, Long fkPharmacieDestination, String statut, String searchText) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transfert_interne WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fkPharmacieSource != null) {
            sql.append(" AND fkPharmacieSource = ?");
            params.add(fkPharmacieSource);
        }

        if (fkPharmacieDestination != null) {
            sql.append(" AND fkPharmacieDestination = ?");
            params.add(fkPharmacieDestination);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }

        // Recherche par commentaire (recherche partielle, insensible à la casse)
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (LOWER(commentaire) LIKE LOWER(?) OR CAST(id AS CHAR) LIKE ?)");
            String searchPattern = "%" + searchText.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public int save(TransfertInterne transfertInterne) {
        String sql = "INSERT INTO transfert_interne (fkPharmacieSource, fkPharmacieDestination, statut, commentaire, perime, "
                + "datecreate, usercreateid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        // S'assurer que le statut est défini
        TransfertInterne.StatutTransfertInterne statut = transfertInterne.getStatut() != null ? transfertInterne.getStatut() : TransfertInterne.StatutTransfertInterne.EN_ATTENTE;
        String statutDbValue = convertStatutForDatabase(statut);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, transfertInterne.getFkPharmacieSource());
            ps.setLong(2, transfertInterne.getFkPharmacieDestination());
            ps.setString(3, statutDbValue);
            if (transfertInterne.getCommentaire() != null) {
                ps.setString(4, transfertInterne.getCommentaire());
            } else {
                ps.setNull(4, java.sql.Types.LONGVARCHAR);
            }
            if (transfertInterne.getPerime() != null) {
                ps.setBoolean(5, transfertInterne.getPerime());
            } else {
                ps.setNull(5, java.sql.Types.TINYINT);
            }
            ps.setTimestamp(6, Timestamp.valueOf(transfertInterne.getDateCreate() != null ? transfertInterne.getDateCreate() : LocalDateTime.now()));
            if (transfertInterne.getUserCreatedId() != null) {
                ps.setLong(7, transfertInterne.getUserCreatedId());
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        // Récupérer l'ID généré et l'assigner à l'objet
        if (rowsAffected > 0 && keyHolder.getKey() != null) {
            Long generatedId = keyHolder.getKey().longValue();
            transfertInterne.setId(generatedId);
            log.debug("ID généré pour le transfert interne: {}", generatedId);
        }

        return rowsAffected;
    }

    @Override
    public int update(TransfertInterne transfertInterne) {
        String sql = "UPDATE transfert_interne SET fkPharmacieSource = ?, fkPharmacieDestination = ?, statut = ?, "
                + "commentaire = ?, perime = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                transfertInterne.getFkPharmacieSource(),
                transfertInterne.getFkPharmacieDestination(),
                convertStatutForDatabase(transfertInterne.getStatut()),
                transfertInterne.getCommentaire(),
                transfertInterne.getPerime(),
                Timestamp.valueOf(transfertInterne.getDateUpdate() != null ? transfertInterne.getDateUpdate() : LocalDateTime.now()),
                transfertInterne.getUserUpdatedId(),
                transfertInterne.getId());
    }

    /**
     * Convertit l'enum StatutTransfertInterne en valeur de base de données.
     */
    private static String convertStatutForDatabase(TransfertInterne.StatutTransfertInterne statut) {
        if (statut == null) {
            return "EN ATTENTE"; // Valeur par défaut
        }
        return statut.getDbValue();
    }

    /**
     * Convertit la valeur de base de données en enum StatutTransfertInterne.
     */
    private static TransfertInterne.StatutTransfertInterne convertStatutFromDatabase(String statut) {
        return TransfertInterne.StatutTransfertInterne.fromDbValue(statut);
    }
}

