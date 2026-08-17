package cd.shad.erp.cmk.cmkerp.stocks.transferts.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.ReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.ReceptionTransfertInterneRepository;
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
 * Implémentation JDBC du repository ReceptionTransfertInterne.
 */
@Repository
@Slf4j
public class ReceptionTransfertInterneJdbcRepositoryImpl extends AbstractJdbcRepository implements ReceptionTransfertInterneRepository {

    public ReceptionTransfertInterneJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<ReceptionTransfertInterne> RECEPTION_TRANSFERT_INTERNE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return ReceptionTransfertInterne.builder()
                .id(rs.getLong("id"))
                .fkTransfertInterne(rs.getLong("fkTransfertInterne"))
                .statut(convertStatutFromDatabase(rs.getString("statut")))
                .perime(rs.getObject("perime", Boolean.class))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<ReceptionTransfertInterne> findById(Long id) {
        String sql = "SELECT id, fkTransfertInterne, statut, perime, datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM reception_transfert_interne WHERE id = ?";
        return queryForOptional(sql, RECEPTION_TRANSFERT_INTERNE_MAPPER, id);
    }

    @Override
    public Optional<ReceptionTransfertInterne> findByFkTransfertInterne(Long fkTransfertInterne) {
        String sql = "SELECT id, fkTransfertInterne, statut, perime, datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM reception_transfert_interne WHERE fkTransfertInterne = ?";
        return queryForOptional(sql, RECEPTION_TRANSFERT_INTERNE_MAPPER, fkTransfertInterne);
    }

    @Override
    public List<ReceptionTransfertInterne> findAll(int offset, int limit, Long fkPharmacieDestination, String statut) {
        StringBuilder sql = new StringBuilder("SELECT rti.id, rti.fkTransfertInterne, rti.statut, rti.perime, rti.datecreate, rti.dateupdate, rti.usercreateid, rti.userupdateid ")
                .append("FROM reception_transfert_interne rti ")
                .append("INNER JOIN transfert_interne ti ON rti.fkTransfertInterne = ti.id ")
                .append("WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (fkPharmacieDestination != null) {
            sql.append(" AND ti.fkPharmacieDestination = ?");
            params.add(fkPharmacieDestination);
        }

        // Filtrer uniquement les réceptions avec statut ANNULEE ou RECEPTIONNEE
        if (statut != null && !statut.trim().isEmpty()) {
            // Si un statut spécifique est demandé, vérifier qu'il est valide
            if (statut.equals("ANNULEE") || statut.equals("RECEPTIONNEE")) {
                sql.append(" AND rti.statut = ?");
                params.add(statut);
            } else {
                // Si le statut n'est pas valide, ne rien retourner (WHERE 1=0)
                sql.append(" AND 1=0");
            }
        } else {
            // Si aucun statut n'est spécifié, filtrer par défaut sur ANNULEE et RECEPTIONNEE uniquement
            sql.append(" AND rti.statut IN ('ANNULEE', 'RECEPTIONNEE')");
        }

        sql.append(" ORDER BY rti.datecreate DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), RECEPTION_TRANSFERT_INTERNE_MAPPER, params.toArray());
    }

    @Override
    public long count(Long fkPharmacieDestination, String statut) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM reception_transfert_interne rti ")
                .append("INNER JOIN transfert_interne ti ON rti.fkTransfertInterne = ti.id ")
                .append("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fkPharmacieDestination != null) {
            sql.append(" AND ti.fkPharmacieDestination = ?");
            params.add(fkPharmacieDestination);
        }

        // Filtrer uniquement les réceptions avec statut ANNULEE ou RECEPTIONNEE
        if (statut != null && !statut.trim().isEmpty()) {
            // Si un statut spécifique est demandé, vérifier qu'il est valide
            if (statut.equals("ANNULEE") || statut.equals("RECEPTIONNEE")) {
                sql.append(" AND rti.statut = ?");
                params.add(statut);
            } else {
                // Si le statut n'est pas valide, retourner 0
                sql.append(" AND 1=0");
            }
        } else {
            // Si aucun statut n'est spécifié, filtrer par défaut sur ANNULEE et RECEPTIONNEE uniquement
            sql.append(" AND rti.statut IN ('ANNULEE', 'RECEPTIONNEE')");
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public int save(ReceptionTransfertInterne receptionTransfertInterne) {
        // Validation
        if (receptionTransfertInterne.getFkTransfertInterne() == null) {
            log.error("fkTransfertInterne est null pour la réception: {}", receptionTransfertInterne);
            throw new IllegalArgumentException("fkTransfertInterne ne peut pas être null");
        }

        // Vérifier que la table existe
        try {
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'reception_transfert_interne'";
            Long tableExists = jdbcTemplate.queryForObject(checkTableSql, Long.class);
            if (tableExists == null || tableExists == 0) {
                log.error("La table reception_transfert_interne n'existe pas dans la base de données");
                throw new IllegalStateException("La table reception_transfert_interne n'existe pas. Veuillez exécuter la migration Flyway V10.");
            }
        } catch (Exception e) {
            log.warn("Impossible de vérifier l'existence de la table (peut être normal selon la version MySQL): {}", e.getMessage());
        }

        String sql = "INSERT INTO reception_transfert_interne (fkTransfertInterne, statut, perime, datecreate, usercreateid) "
                + "VALUES (?, ?, ?, ?, ?)";

        ReceptionTransfertInterne.StatutReceptionTransfertInterne statut = receptionTransfertInterne.getStatut() != null
                ? receptionTransfertInterne.getStatut()
                : ReceptionTransfertInterne.StatutReceptionTransfertInterne.EN_ATTENTE;
        String statutDbValue = convertStatutForDatabase(statut);

        LocalDateTime dateCreate = receptionTransfertInterne.getDateCreate() != null
                ? receptionTransfertInterne.getDateCreate()
                : LocalDateTime.now();

        log.debug("Création de réception - fkTransfertInterne: {}, statut: {}, dateCreate: {}, userCreatedId: {}",
                receptionTransfertInterne.getFkTransfertInterne(), statutDbValue, dateCreate, receptionTransfertInterne.getUserCreatedId());
        log.debug("SQL: {}", sql);
        log.debug("Paramètres: fkTransfertInterne={}, statut={}, dateCreate={}, userCreatedId={}",
                receptionTransfertInterne.getFkTransfertInterne(), statutDbValue, dateCreate, receptionTransfertInterne.getUserCreatedId());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected;
        try {
            rowsAffected = jdbcTemplate.update(connection -> {
                java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, receptionTransfertInterne.getFkTransfertInterne());
                ps.setString(2, statutDbValue);
                if (receptionTransfertInterne.getPerime() != null) {
                    ps.setBoolean(3, receptionTransfertInterne.getPerime());
                } else {
                    ps.setNull(3, java.sql.Types.TINYINT);
                }
                ps.setTimestamp(4, Timestamp.valueOf(dateCreate));
                if (receptionTransfertInterne.getUserCreatedId() != null) {
                    ps.setLong(5, receptionTransfertInterne.getUserCreatedId());
                } else {
                    ps.setNull(5, java.sql.Types.BIGINT);
                }
                log.debug("PreparedStatement créé avec paramètres: [1]={}, [2]={}, [3]={}, [4]={}",
                        ps.getParameterMetaData().getParameterType(1),
                        ps.getParameterMetaData().getParameterType(2),
                        ps.getParameterMetaData().getParameterType(3),
                        ps.getParameterMetaData().getParameterType(4));
                return ps;
            }, keyHolder);
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            Throwable rootCause = e.getRootCause();
            if (rootCause != null) {
                errorMessage = rootCause.getMessage();
            }
            log.error("Erreur SQL lors de la création de la réception - SQL: '{}', fkTransfertInterne: {}, statut: '{}', dateCreate: {}, userCreatedId: {}, erreur: {}",
                    sql, receptionTransfertInterne.getFkTransfertInterne(), statutDbValue, dateCreate, receptionTransfertInterne.getUserCreatedId(), errorMessage, e);
            throw new RuntimeException("Erreur lors de la création de la réception: " + errorMessage, e);
        }

        if (rowsAffected > 0 && keyHolder.getKey() != null) {
            Long generatedId = keyHolder.getKey().longValue();
            receptionTransfertInterne.setId(generatedId);
            log.debug("ID généré pour la réception de transfert interne: {}", generatedId);
        }

        return rowsAffected;
    }

    @Override
    public int update(ReceptionTransfertInterne receptionTransfertInterne) {
        String sql = "UPDATE reception_transfert_interne SET statut = ?, perime = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                convertStatutForDatabase(receptionTransfertInterne.getStatut()),
                receptionTransfertInterne.getPerime(),
                Timestamp.valueOf(receptionTransfertInterne.getDateUpdate() != null ? receptionTransfertInterne.getDateUpdate() : LocalDateTime.now()),
                receptionTransfertInterne.getUserUpdatedId(),
                receptionTransfertInterne.getId());
    }

    private static String convertStatutForDatabase(ReceptionTransfertInterne.StatutReceptionTransfertInterne statut) {
        if (statut == null) {
            return "EN ATTENTE";
        }
        return statut.getDbValue();
    }

    private static ReceptionTransfertInterne.StatutReceptionTransfertInterne convertStatutFromDatabase(String statut) {
        return ReceptionTransfertInterne.StatutReceptionTransfertInterne.fromDbValue(statut);
    }
}

