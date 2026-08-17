package cd.shad.erp.cmk.cmkerp.stocks.inventaires.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.LigneInventaire;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.repository.LigneInventaireRepository;
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

/**
 * Implémentation JDBC du repository LigneInventaire.
 * Note: Les lignes sont créées automatiquement par une procédure stockée,
 * on ne peut que les mettre à jour.
 */
@Repository
@Slf4j
public class LigneInventaireJdbcRepositoryImpl extends AbstractJdbcRepository implements LigneInventaireRepository {

    public LigneInventaireJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    private static final RowMapper<LigneInventaire> LIGNE_INVENTAIRE_MAPPER = (rs, rowNum) -> {
        Timestamp dateCreateTs = rs.getTimestamp("datecreate");
        Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

        return LigneInventaire.builder()
                .id(rs.getLong("id"))
                .fkInventaire(rs.getLong("fkInventaire"))
                .fkStock(rs.getLong("fkStock"))
                .quantite_theorique(rs.getFloat("quantite_theorique"))
                .quantite_physique(rs.getFloat("quantite_physique"))
                // ecart est une colonne virtuelle (GENERATED ALWAYS AS) - pas besoin de la mapper
                .commentaire(rs.getString("commentaire"))
                .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
                .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
                .userCreatedId(rs.getObject("usercreateid", Long.class))
                .userUpdatedId(rs.getObject("userupdateid", Long.class))
                .build();
    };

    @Override
    public Optional<LigneInventaire> findById(Long id) {
        String sql = "SELECT id, fkInventaire, fkStock, quantite_theorique, quantite_physique, commentaire, "
                + "datecreate, dateupdate, usercreateid, userupdateid "
                + "FROM lignes_inventaire WHERE id = ?";
        return queryForOptional(sql, LIGNE_INVENTAIRE_MAPPER, id);
    }

    @Override
    public List<LigneInventaire> findByFkInventaire(Long fkInventaire) {
        return findByFkInventaire(fkInventaire, null);
    }

    @Override
    public List<LigneInventaire> findByFkInventaire(Long fkInventaire, Boolean operationnel) {
        // Toujours joindre stock_produits pour pouvoir filtrer les actifs (operationnel = 1).
        StringBuilder sql = new StringBuilder("""
            SELECT li.id, li.fkInventaire, li.fkStock, li.quantite_theorique, li.quantite_physique,
                   li.commentaire, li.datecreate, li.dateupdate, li.usercreateid, li.userupdateid
            FROM lignes_inventaire li
            INNER JOIN stock_produits sp ON sp.id = li.fkStock
            WHERE li.fkInventaire = ?
            """);
        if (Boolean.TRUE.equals(operationnel)) {
            sql.append(" AND COALESCE(sp.operationnel, 0) = 1 ");
        } else if (Boolean.FALSE.equals(operationnel)) {
            sql.append(" AND COALESCE(sp.operationnel, 0) = 0 ");
        }
        sql.append(" ORDER BY li.datecreate");

        return jdbcTemplate.query(sql.toString(), LIGNE_INVENTAIRE_MAPPER, fkInventaire);
    }

    @Override
    public int update(LigneInventaire ligneInventaire) {
        String sql = "UPDATE lignes_inventaire SET quantite_physique = ?, commentaire = ?, "
                + "dateupdate = ?, userupdateid = ? WHERE id = ?";
        return update(sql,
                ligneInventaire.getQuantite_physique(),
                ligneInventaire.getCommentaire(),
                Timestamp.valueOf(ligneInventaire.getDateUpdate() != null ? ligneInventaire.getDateUpdate() : LocalDateTime.now()),
                ligneInventaire.getUserUpdatedId(),
                ligneInventaire.getId());
    }
}

