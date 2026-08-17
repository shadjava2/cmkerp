package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.Approvisionnement;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.ApprovisionnementRepository;
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
 * Implémentation JDBC du repository Approvisionnement.
 */
@Repository
@Slf4j
public class ApprovisionnementJdbcRepositoryImpl extends AbstractJdbcRepository implements ApprovisionnementRepository {

    public ApprovisionnementJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Approvisionnement> APPROVISIONNEMENT_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");
        java.sql.Date datebonlivSql = rs.getDate("datebonliv");

        return Approvisionnement.builder()
                .id(rs.getLong("id"))
                .fkFournisseur(rs.getLong("fkFournisseur"))
                .fkPharmacie(rs.getLong("fkPharmacie"))
                .fkEchangeDevise(rs.getObject("fkEchangeDevise", Long.class))
                .statut(convertStatutFromDatabase(rs.getString("statut")))
                .numbonliv(rs.getString("numbonliv"))
                .taux(rs.getObject("taux", Short.class))
                .datebonliv(datebonlivSql != null ? datebonlivSql.toLocalDate() : null)
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .fkBonCommande(getLongQuiet(rs, "fk_bon_commande"))
                .fkReceptionCommande(getLongQuiet(rs, "fk_reception_commande"))
                .build();
    };

    private static Long getLongQuiet(java.sql.ResultSet rs, String col) {
        try {
            return rs.getObject(col, Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Optional<Approvisionnement> findById(Long id) {
        String sql = "SELECT id, fkFournisseur, fkPharmacie, fkEchangeDevise, statut, numbonliv, taux, datebonliv, "
                + "datecreate, dateupdate, usercreateid, userupdateid, fk_bon_commande, fk_reception_commande "
                + "FROM approvsionnements WHERE id = ?";
        return queryForOptional(sql, APPROVISIONNEMENT_MAPPER, id);
    }

    @Override
    public List<Approvisionnement> findAll(int offset, int limit, Long fkPharmacie, String statut, Long fkFournisseur, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText, String produitQ, Long produitId) {
        StringBuilder sql = new StringBuilder("SELECT id, fkFournisseur, fkPharmacie, fkEchangeDevise, statut, numbonliv, taux, datebonliv, ")
                .append("datecreate, dateupdate, usercreateid, userupdateid, fk_bon_commande, fk_reception_commande ")
                .append("FROM approvsionnements WHERE 1=1");

        List<Object> params = new ArrayList<>();
        appendListFilters(sql, params, fkPharmacie, statut, fkFournisseur, dateFrom, dateTo, searchText, produitQ, produitId);

        sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), APPROVISIONNEMENT_MAPPER, params.toArray());
    }

    @Override
    public long count(Long fkPharmacie, String statut, Long fkFournisseur, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, String searchText, String produitQ, Long produitId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM approvsionnements WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendListFilters(sql, params, fkPharmacie, statut, fkFournisseur, dateFrom, dateTo, searchText, produitQ, produitId);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    private static void appendListFilters(
            StringBuilder sql,
            List<Object> params,
            Long fkPharmacie,
            String statut,
            Long fkFournisseur,
            java.time.LocalDate dateFrom,
            java.time.LocalDate dateTo,
            String searchText,
            String produitQ,
            Long produitId) {
        if (fkPharmacie != null) {
            sql.append(" AND fkPharmacie = ?");
            params.add(fkPharmacie);
        }

        if (statut != null && !statut.trim().isEmpty()) {
            sql.append(" AND statut = ?");
            params.add(statut.trim().replace("_", " "));
        }

        if (fkFournisseur != null) {
            sql.append(" AND fkFournisseur = ?");
            params.add(fkFournisseur);
        }

        if (dateFrom != null) {
            sql.append(" AND DATE(datecreate) >= ?");
            params.add(java.sql.Date.valueOf(dateFrom));
        }

        if (dateTo != null) {
            sql.append(" AND DATE(datecreate) <= ?");
            params.add(java.sql.Date.valueOf(dateTo));
        }

        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (LOWER(numbonliv) LIKE LOWER(?) OR CAST(id AS CHAR) LIKE ?")
                    .append(" OR fkFournisseur IN (SELECT id FROM fournisseurs WHERE LOWER(nom) LIKE LOWER(?)))");
            String searchPattern = "%" + searchText.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (produitId != null) {
            sql.append("""
                 AND EXISTS (
                   SELECT 1 FROM lignes_approv la
                   INNER JOIN stock_produits sp ON sp.id = la.fkStock
                   WHERE la.fkApprov = approvsionnements.id
                     AND sp.fkProduits = ?
                 )
                """);
            params.add(produitId);
        } else if (produitQ != null && !produitQ.trim().isEmpty()) {
            sql.append("""
                 AND EXISTS (
                   SELECT 1 FROM lignes_approv la
                   INNER JOIN stock_produits sp ON sp.id = la.fkStock
                   INNER JOIN produits p ON p.id = sp.fkProduits
                   WHERE la.fkApprov = approvsionnements.id
                     AND (
                       LOWER(COALESCE(p.nomcommercial, '')) LIKE LOWER(?)
                       OR LOWER(COALESCE(p.nomscientifique, '')) LIKE LOWER(?)
                     )
                 )
                """);
            String produitPattern = "%" + produitQ.trim() + "%";
            params.add(produitPattern);
            params.add(produitPattern);
        }
    }

    @Override
    public int save(Approvisionnement approvisionnement) {
        String sql = "INSERT INTO approvsionnements (fkFournisseur, fkPharmacie, fkEchangeDevise, statut, numbonliv, taux, datebonliv, "
                + "datecreate, usercreateid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, approvisionnement.getFkFournisseur());
            ps.setLong(2, approvisionnement.getFkPharmacie());
            if (approvisionnement.getFkEchangeDevise() != null) {
                ps.setLong(3, approvisionnement.getFkEchangeDevise());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            ps.setString(4, convertStatutForDatabase(approvisionnement.getStatut()));
            if (approvisionnement.getNumbonliv() != null) {
                ps.setString(5, approvisionnement.getNumbonliv());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            if (approvisionnement.getTaux() != null) {
                ps.setShort(6, approvisionnement.getTaux());
            } else {
                ps.setNull(6, java.sql.Types.SMALLINT);
            }
            if (approvisionnement.getDatebonliv() != null) {
                ps.setDate(7, java.sql.Date.valueOf(approvisionnement.getDatebonliv()));
            } else {
                ps.setNull(7, java.sql.Types.DATE);
            }
            ps.setTimestamp(8, Timestamp.valueOf(approvisionnement.getDateCreate() != null ? approvisionnement.getDateCreate() : LocalDateTime.now()));
            if (approvisionnement.getUserCreatedId() != null) {
                ps.setLong(9, approvisionnement.getUserCreatedId());
            } else {
                ps.setNull(9, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        // Récupérer l'ID généré et l'assigner à l'objet
        if (rowsAffected > 0) {
            if (keyHolder.getKey() != null) {
                approvisionnement.setId(keyHolder.getKey().longValue());
            } else {
                Long lastId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                if (lastId != null && lastId > 0) {
                    approvisionnement.setId(lastId);
                }
            }
            log.debug("ID généré pour l'approvisionnement: {}", approvisionnement.getId());
        }

        return rowsAffected;
    }

    @Override
    public Optional<Approvisionnement> findByPharmacieAndNumeroBon(
            Long fkPharmacie, String numeroSaisi, int year) {
        if (numeroSaisi == null || numeroSaisi.isBlank()) {
            return Optional.empty();
        }
        String numero = numeroSaisi.trim();
        String sql = "SELECT id, fkFournisseur, fkPharmacie, fkEchangeDevise, statut, numbonliv, taux, datebonliv, "
                + "datecreate, dateupdate, usercreateid, userupdateid, fk_bon_commande, fk_reception_commande "
                + "FROM approvsionnements WHERE fkPharmacie = ? AND ("
                + "numbonliv LIKE CONCAT(?, '-', ?, '-%') "
                + "OR LOWER(numbonliv) = LOWER(?) "
                + "OR LOWER(numbonliv) LIKE LOWER(CONCAT('%-', ?, '-%')) "
                + "OR LOWER(numbonliv) LIKE LOWER(CONCAT('%', ?, '%'))"
                + ") ORDER BY id DESC LIMIT 1";
        return queryForOptional(sql, APPROVISIONNEMENT_MAPPER, fkPharmacie, year, numero, numero, numero,
                numero);
    }

    @Override
    public int update(Approvisionnement approvisionnement) {
        String sql = "UPDATE approvsionnements SET fkFournisseur = ?, fkPharmacie = ?, fkEchangeDevise = ?, statut = ?, "
                + "numbonliv = ?, taux = ?, datebonliv = ?, dateupdate = ?, userupdateid = ?, "
                + "fk_bon_commande = ?, fk_reception_commande = ? WHERE id = ?";
        return update(sql,
                approvisionnement.getFkFournisseur(),
                approvisionnement.getFkPharmacie(),
                approvisionnement.getFkEchangeDevise(),
                convertStatutForDatabase(approvisionnement.getStatut()),
                approvisionnement.getNumbonliv(),
                approvisionnement.getTaux(),
                approvisionnement.getDatebonliv() != null ? java.sql.Date.valueOf(approvisionnement.getDatebonliv()) : null,
                Timestamp.valueOf(approvisionnement.getDateUpdate() != null ? approvisionnement.getDateUpdate() : LocalDateTime.now()),
                approvisionnement.getUserUpdatedId(),
                approvisionnement.getFkBonCommande(),
                approvisionnement.getFkReceptionCommande(),
                approvisionnement.getId());
    }

    /**
     * Convertit l'enum StatutApprovisionnement (avec underscore) en valeur de base de données (avec espace).
     * La base de données utilise un ENUM avec 'EN ATTENTE' (espace), mais l'enum Java utilise EN_ATTENTE (underscore).
     */
    private static String convertStatutForDatabase(Approvisionnement.StatutApprovisionnement statut) {
        if (statut == null) {
            return "EN ATTENTE"; // Valeur par défaut
        }
        // Convertir EN_ATTENTE en EN ATTENTE pour correspondre à l'ENUM MySQL
        return statut.name().replace("_", " ");
    }

    /**
     * Convertit la valeur de base de données (avec espace) en enum StatutApprovisionnement (avec underscore).
     * La base de données utilise un ENUM avec 'EN ATTENTE' (espace), mais l'enum Java utilise EN_ATTENTE (underscore).
     */
    private static Approvisionnement.StatutApprovisionnement convertStatutFromDatabase(String statut) {
        if (statut == null || statut.trim().isEmpty()) {
            return Approvisionnement.StatutApprovisionnement.EN_ATTENTE; // Valeur par défaut
        }
        // Convertir "EN ATTENTE" en EN_ATTENTE pour correspondre à l'enum Java
        String enumValue = statut.trim().replace(" ", "_");
        return Approvisionnement.StatutApprovisionnement.valueOf(enumValue);
    }
}

