package cd.shad.erp.cmk.cmkerp.stocks.inventaires.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.Inventaire;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository.InventaireRepository;
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
 * Implémentation JDBC du repository Inventaire.
 */
@Repository
@Slf4j
public class InventaireJdbcRepositoryImpl extends AbstractJdbcRepository implements InventaireRepository {

    public InventaireJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Inventaire> INVENTAIRE_MAPPER = (rs, rowNum) -> {
        Timestamp dateDebutTs = rs.getTimestamp("date_debut");
        Timestamp dateFinTs = rs.getTimestamp("date_fin");
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return Inventaire.builder()
                .id(rs.getLong("id"))
                .fkPharmacie(rs.getLong("fkPharmacie"))
                .date_debut(dateDebutTs != null ? dateDebutTs.toLocalDateTime() : null)
                .date_fin(dateFinTs != null ? dateFinTs.toLocalDateTime() : null)
                .statut(convertStatutFromDatabase(rs.getString("statut")))
                .commentaire(rs.getString("commentaire"))
                .typeinventaire(convertTypeInventaireFromDatabase(rs.getString("typeinventaire")))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<Inventaire> findById(Long id) {
        String sql = "SELECT id, fkPharmacie, date_debut, date_fin, statut, commentaire, typeinventaire, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM inventaires WHERE id = ?";
        return queryForOptional(sql, INVENTAIRE_MAPPER, id);
    }

    @Override
    public List<Inventaire> findAll(int offset, int limit, Long fkPharmacie, String statut, String typeinventaire,
            java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText) {
        StringBuilder sql = new StringBuilder("SELECT id, fkPharmacie, date_debut, date_fin, statut, commentaire, typeinventaire, ")
                .append("datecreate, dateupdate, usercreateid, userupdateid ")
                .append("FROM inventaires WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (fkPharmacie != null) {
            sql.append(" AND fkPharmacie = ?");
            params.add(fkPharmacie);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }

        if (typeinventaire != null && !typeinventaire.trim().isEmpty()) {
            sql.append(" AND typeinventaire = ?");
            params.add(typeinventaire);
        }

        // Filtres de période sur date_debut
        if (dateFrom != null) {
            sql.append(" AND DATE(date_debut) >= ?");
            params.add(java.sql.Date.valueOf(dateFrom));
        }

        if (dateTo != null) {
            sql.append(" AND DATE(date_debut) <= ?");
            params.add(java.sql.Date.valueOf(dateTo));
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

        return jdbcTemplate.query(sql.toString(), INVENTAIRE_MAPPER, params.toArray());
    }

    @Override
    public long count(Long fkPharmacie, String statut, String typeinventaire,
            java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM inventaires WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fkPharmacie != null) {
            sql.append(" AND fkPharmacie = ?");
            params.add(fkPharmacie);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }

        if (typeinventaire != null && !typeinventaire.trim().isEmpty()) {
            sql.append(" AND typeinventaire = ?");
            params.add(typeinventaire);
        }

        // Filtres de période sur date_debut
        if (dateFrom != null) {
            sql.append(" AND DATE(date_debut) >= ?");
            params.add(java.sql.Date.valueOf(dateFrom));
        }

        if (dateTo != null) {
            sql.append(" AND DATE(date_debut) <= ?");
            params.add(java.sql.Date.valueOf(dateTo));
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
    public int save(Inventaire inventaire) {
        String sql = "INSERT INTO inventaires (fkPharmacie, date_debut, statut, commentaire, typeinventaire, "
                + "datecreate, usercreateid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        // S'assurer que le statut est défini
        Inventaire.StatutInventaire statut = inventaire.getStatut() != null ? inventaire.getStatut() : Inventaire.StatutInventaire.EN_COURS;
        String statutDbValue = convertStatutForDatabase(statut);

        // S'assurer que le type est défini
        Inventaire.TypeInventaire type = inventaire.getTypeinventaire() != null ? inventaire.getTypeinventaire() : Inventaire.TypeInventaire.PHYSIQUE;
        String typeDbValue = convertTypeInventaireForDatabase(type);

        log.debug("💾 [InventaireJdbcRepositoryImpl] Saving inventaire - statut: {}, type: {}", statutDbValue, typeDbValue);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, inventaire.getFkPharmacie());
            ps.setTimestamp(2, Timestamp.valueOf(inventaire.getDate_debut() != null ? inventaire.getDate_debut() : LocalDateTime.now()));
            ps.setString(3, statutDbValue);
            if (inventaire.getCommentaire() != null) {
                ps.setString(4, inventaire.getCommentaire());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            ps.setString(5, typeDbValue);
            ps.setTimestamp(6, Timestamp.valueOf(inventaire.getDateCreate() != null ? inventaire.getDateCreate() : LocalDateTime.now()));
            if (inventaire.getUserCreatedId() != null) {
                ps.setLong(7, inventaire.getUserCreatedId());
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        // Récupérer l'ID généré et l'assigner à l'objet
        if (rowsAffected > 0 && keyHolder.getKey() != null) {
            Long generatedId = keyHolder.getKey().longValue();
            inventaire.setId(generatedId);
            log.debug("ID généré pour l'inventaire: {}", generatedId);
        }

        return rowsAffected;
    }

    @Override
    public int update(Inventaire inventaire) {
        String sql = "UPDATE inventaires SET fkPharmacie = ?, date_debut = ?, date_fin = ?, statut = ?, "
                + "commentaire = ?, typeinventaire = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                inventaire.getFkPharmacie(),
                Timestamp.valueOf(inventaire.getDate_debut() != null ? inventaire.getDate_debut() : LocalDateTime.now()),
                inventaire.getDate_fin() != null ? Timestamp.valueOf(inventaire.getDate_fin()) : null,
                convertStatutForDatabase(inventaire.getStatut()),
                inventaire.getCommentaire(),
                convertTypeInventaireForDatabase(inventaire.getTypeinventaire()),
                Timestamp.valueOf(inventaire.getDateUpdate() != null ? inventaire.getDateUpdate() : LocalDateTime.now()),
                inventaire.getUserUpdatedId(),
                inventaire.getId());
    }

    /**
     * Convertit l'enum StatutInventaire en valeur de base de données.
     */
    private static String convertStatutForDatabase(Inventaire.StatutInventaire statut) {
        if (statut == null) {
            return "EN COURS"; // Valeur par défaut
        }
        return statut.getDbValue();
    }

    /**
     * Convertit la valeur de base de données en enum StatutInventaire.
     */
    private static Inventaire.StatutInventaire convertStatutFromDatabase(String statut) {
        return Inventaire.StatutInventaire.fromDbValue(statut);
    }

    /**
     * Convertit l'enum TypeInventaire en valeur de base de données.
     */
    private static String convertTypeInventaireForDatabase(Inventaire.TypeInventaire type) {
        if (type == null) {
            return "PHYSIQUE"; // Valeur par défaut
        }
        return type.getDbValue();
    }

    /**
     * Convertit la valeur de base de données en enum TypeInventaire.
     */
    private static Inventaire.TypeInventaire convertTypeInventaireFromDatabase(String type) {
        return Inventaire.TypeInventaire.fromDbValue(type);
    }
}

