package cd.shad.erp.cmk.cmkerp.stocks.ventes.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.Vente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.VenteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Implémentation JDBC du repository Vente.
 */
@Repository
@Slf4j
public class VenteJdbcRepositoryImpl extends AbstractJdbcRepository implements VenteRepository {

    public VenteJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Vente> VENTE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return Vente.builder()
                .id(rs.getLong("id"))
                .fkEntreprise(rs.getObject("fkEntreprise", Long.class))
                .fkPatient(rs.getObject("fkPatient", Long.class))
                .fkPharmacie(rs.getLong("fkPharmacie"))
                .statut(convertStatutFromDatabase(rs.getString("statut")))
                .taux(rs.getObject("taux", Short.class))
                .typepaiement(rs.getString("typepaiement"))
                .raisonsortie(rs.getString("raisonsortie"))
                .demandeur(rs.getString("demandeur"))
                .fkPatientMediline(rs.getString("fkPatientMediline"))
                .fkFicheMedicale(rs.getString("fkFicheMedicale"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<Vente> findById(Long id) {
        String sql = "SELECT id, fkEntreprise, fkPatient, fkPharmacie, statut, taux, typepaiement, raisonsortie, "
                + "demandeur, fkPatientMediline, fkFicheMedicale, datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM ventes WHERE id = ?";
        return queryForOptional(sql, VENTE_MAPPER, id);
    }

    @Override
    public List<Vente> findAll(int offset, int limit, Long fkPharmacie, String statut, Long fkPatient,
            java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText) {
        StringBuilder sql = new StringBuilder("SELECT id, fkEntreprise, fkPatient, fkPharmacie, statut, taux, typepaiement, raisonsortie, ")
                .append("demandeur, fkPatientMediline, fkFicheMedicale, datecreate, dateupdate, usercreateid, userupdateid ")
                .append("FROM ventes WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (fkPharmacie != null) {
            sql.append(" AND fkPharmacie = ?");
            params.add(fkPharmacie);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }

        if (fkPatient != null && fkPatient != 0) {
            sql.append(" AND fkPatient = ?");
            params.add(fkPatient);
        }

        // Filtres de période sur datecreate (date de création)
        if (dateFrom != null) {
            sql.append(" AND DATE(datecreate) >= ?");
            params.add(java.sql.Date.valueOf(dateFrom));
        }

        if (dateTo != null) {
            sql.append(" AND DATE(datecreate) <= ?");
            params.add(java.sql.Date.valueOf(dateTo));
        }

        // Recherche par demandeur ou raisonsortie (recherche partielle, insensible à la casse)
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (LOWER(demandeur) LIKE LOWER(?) OR LOWER(raisonsortie) LIKE LOWER(?) OR CAST(id AS CHAR) LIKE ?)");
            String searchPattern = "%" + searchText.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), VENTE_MAPPER, params.toArray());
    }

    @Override
    public long count(Long fkPharmacie, String statut, Long fkPatient,
            java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ventes WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fkPharmacie != null) {
            sql.append(" AND fkPharmacie = ?");
            params.add(fkPharmacie);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }

        if (fkPatient != null && fkPatient != 0) {
            sql.append(" AND fkPatient = ?");
            params.add(fkPatient);
        }

        // Filtres de période sur datecreate (date de création)
        if (dateFrom != null) {
            sql.append(" AND DATE(datecreate) >= ?");
            params.add(java.sql.Date.valueOf(dateFrom));
        }

        if (dateTo != null) {
            sql.append(" AND DATE(datecreate) <= ?");
            params.add(java.sql.Date.valueOf(dateTo));
        }

        // Recherche par demandeur ou raisonsortie (recherche partielle, insensible à la casse)
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (LOWER(demandeur) LIKE LOWER(?) OR LOWER(raisonsortie) LIKE LOWER(?) OR CAST(id AS CHAR) LIKE ?)");
            String searchPattern = "%" + searchText.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public int save(Vente vente) {
        String sql = "INSERT INTO ventes (fkEntreprise, fkPatient, fkPharmacie, statut, taux, typepaiement, raisonsortie, "
                + "demandeur, fkPatientMediline, fkFicheMedicale, datecreate, dateupdate, usercreateid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // S'assurer que le statut est défini
        Vente.StatutVente statut = vente.getStatut() != null ? vente.getStatut() : Vente.StatutVente.EN_ATTENTE;
        String statutDbValue = convertStatutForDatabase(statut);
        log.info("💾 [VenteJdbcRepositoryImpl] Saving vente - statut enum: {}, dbValue: '{}'", statut, statutDbValue);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateCreate = vente.getDateCreate() != null ? vente.getDateCreate() : now;
        LocalDateTime dateUpdate = vente.getDateUpdate() != null ? vente.getDateUpdate() : dateCreate;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, vente.getFkEntreprise() != null ? vente.getFkEntreprise() : 0L);
            ps.setLong(2, vente.getFkPatient() != null ? vente.getFkPatient() : 0L);
            ps.setLong(3, vente.getFkPharmacie());
            ps.setString(4, statutDbValue);
            ps.setShort(5, vente.getTaux() != null ? vente.getTaux() : (short) 0);
            ps.setString(6, vente.getTypepaiement() != null ? vente.getTypepaiement() : "-");
            if (vente.getRaisonsortie() != null) {
                ps.setString(7, vente.getRaisonsortie());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            if (vente.getDemandeur() != null) {
                ps.setString(8, vente.getDemandeur());
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }
            if (vente.getFkPatientMediline() != null) {
                ps.setString(9, vente.getFkPatientMediline());
            } else {
                ps.setNull(9, java.sql.Types.VARCHAR);
            }
            if (vente.getFkFicheMedicale() != null) {
                ps.setString(10, vente.getFkFicheMedicale());
            } else {
                ps.setNull(10, java.sql.Types.VARCHAR);
            }
            ps.setTimestamp(11, Timestamp.valueOf(dateCreate));
            ps.setTimestamp(12, Timestamp.valueOf(dateUpdate));
            if (vente.getUserCreatedId() != null) {
                ps.setLong(13, vente.getUserCreatedId());
            } else {
                ps.setNull(13, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        // Récupérer l'ID généré et l'assigner à l'objet
        if (rowsAffected > 0 && keyHolder.getKey() != null) {
            Long generatedId = keyHolder.getKey().longValue();
            vente.setId(generatedId);
            vente.setDateCreate(dateCreate);
            vente.setDateUpdate(dateUpdate);
            log.debug("ID généré pour la vente: {}", generatedId);
        }

        return rowsAffected;
    }

    @Override
    public int update(Vente vente) {
        String sql = "UPDATE ventes SET fkEntreprise = ?, fkPatient = ?, fkPharmacie = ?, statut = ?, "
                + "taux = ?, typepaiement = ?, raisonsortie = ?, demandeur = ?, fkPatientMediline = ?, "
                + "fkFicheMedicale = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                vente.getFkEntreprise() != null ? vente.getFkEntreprise() : 0L,
                vente.getFkPatient() != null ? vente.getFkPatient() : 0L,
                vente.getFkPharmacie(),
                convertStatutForDatabase(vente.getStatut()),
                vente.getTaux() != null ? vente.getTaux() : (short) 0,
                vente.getTypepaiement() != null ? vente.getTypepaiement() : "-",
                vente.getRaisonsortie(),
                vente.getDemandeur(),
                vente.getFkPatientMediline(),
                vente.getFkFicheMedicale(),
                Timestamp.valueOf(vente.getDateUpdate() != null ? vente.getDateUpdate() : LocalDateTime.now()),
                vente.getUserUpdatedId(),
                vente.getId());
    }

    /**
     * Convertit l'enum StatutVente en valeur de base de données.
     * Utilise la méthode getDbValue() de l'enum qui retourne la valeur correcte pour la base de données.
     */
    private static String convertStatutForDatabase(Vente.StatutVente statut) {
        if (statut == null) {
            return "EN ATTENTE"; // Valeur par défaut
        }
        return statut.getDbValue();
    }

    /**
     * Convertit la valeur de base de données en enum StatutVente.
     * Utilise la méthode fromDbValue() de l'enum qui gère automatiquement la conversion.
     */
    private static Vente.StatutVente convertStatutFromDatabase(String statut) {
        return Vente.StatutVente.fromDbValue(statut);
    }
}

