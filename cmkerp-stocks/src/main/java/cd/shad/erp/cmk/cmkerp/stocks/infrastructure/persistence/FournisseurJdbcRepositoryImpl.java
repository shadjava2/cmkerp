package cd.shad.erp.cmk.cmkerp.stocks.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Fournisseur;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.FournisseurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC du repository Fournisseur.
 */
@Repository
@Slf4j
public class FournisseurJdbcRepositoryImpl extends AbstractJdbcRepository implements FournisseurRepository {

    public FournisseurJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<Fournisseur> FOURNISSEUR_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return Fournisseur.builder()
                .id(rs.getLong("id"))
                .nom(rs.getString("nom"))
                .adresse(rs.getString("adresse"))
                .telephone(rs.getString("telephone"))
                .email(rs.getString("email"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<Fournisseur> findById(Long id) {
        String sql = "SELECT id, nom, adresse, telephone, email, datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM fournisseurs WHERE id = ?";
        return queryForOptional(sql, FOURNISSEUR_MAPPER, id);
    }

    @Override
    public List<Fournisseur> findAll(int offset, int limit, String nom) {
        StringBuilder sql = new StringBuilder("SELECT id, nom, adresse, telephone, email, datecreate, dateupdate, usercreateid, userupdateid ")
                .append("FROM fournisseurs WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (nom != null && !nom.trim().isEmpty()) {
            sql.append(" AND LOWER(nom) LIKE LOWER(?)");
            params.add("%" + nom.trim() + "%");
        }

        sql.append(" ORDER BY nom ASC");

        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), FOURNISSEUR_MAPPER, params.toArray());
    }

    @Override
    public long count(String nom) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM fournisseurs WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (nom != null && !nom.trim().isEmpty()) {
            sql.append(" AND LOWER(nom) LIKE LOWER(?)");
            params.add("%" + nom.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public Long save(Fournisseur fournisseur) {
        String sql = "INSERT INTO fournisseurs (nom, adresse, telephone, email, datecreate, usercreateid) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fournisseur.getNom());
            ps.setString(2, fournisseur.getAdresse());
            ps.setString(3, fournisseur.getTelephone());
            ps.setString(4, fournisseur.getEmail());
            ps.setTimestamp(5, fournisseur.getDateCreate() != null ? Timestamp.valueOf(fournisseur.getDateCreate()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setObject(6, fournisseur.getUserCreatedId());
            return ps;
        }, keyHolder);

        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    @Override
    public void update(Fournisseur fournisseur) {
        String sql = "UPDATE fournisseurs SET nom = ?, adresse = ?, telephone = ?, email = ?, dateupdate = ?, userupdateid = ? "
                + "WHERE id = ?";

        jdbcTemplate.update(sql,
                fournisseur.getNom(),
                fournisseur.getAdresse(),
                fournisseur.getTelephone(),
                fournisseur.getEmail(),
                fournisseur.getDateUpdate() != null ? Timestamp.valueOf(fournisseur.getDateUpdate()) : Timestamp.valueOf(java.time.LocalDateTime.now()),
                fournisseur.getUserUpdatedId(),
                fournisseur.getId());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM fournisseurs WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}

