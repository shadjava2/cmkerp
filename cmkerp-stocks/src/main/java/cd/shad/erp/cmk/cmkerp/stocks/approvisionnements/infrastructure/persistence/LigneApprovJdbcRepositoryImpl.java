package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.LigneApprov;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.LigneApprovRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Implémentation JDBC du repository LigneApprov.
 */
@Repository
@Slf4j
public class LigneApprovJdbcRepositoryImpl extends AbstractJdbcRepository
    implements LigneApprovRepository {

  public LigneApprovJdbcRepositoryImpl(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
    super(jdbcTemplate, namedJdbcTemplate);
  }

  private static final RowMapper<LigneApprov> LIGNE_APPROV_MAPPER = (rs, rowNum) -> {
    Timestamp dateCreateTs = rs.getTimestamp("datecreate");
    Timestamp dateUpdateTs = rs.getTimestamp("dateupdate");

    return LigneApprov.builder().id(rs.getLong("id")).fkApprov(rs.getLong("fkApprov"))
        .fkStock(rs.getObject("fkStock", Long.class)).qt(rs.getObject("qt", Float.class))
        .prixachat(rs.getBigDecimal("prixachat")).prixachattotal(rs.getBigDecimal("prixachattotal"))
        .totalfournisseur(rs.getBigDecimal("totalfournisseur"))
        .dateCreate(dateCreateTs != null ? dateCreateTs.toLocalDateTime() : null)
        .dateUpdate(dateUpdateTs != null ? dateUpdateTs.toLocalDateTime() : null)
        .userCreatedId(rs.getObject("usercreateid", Long.class))
        .userUpdatedId(rs.getObject("userupdateid", Long.class)).build();
  };

  @Override
  public Optional<LigneApprov> findById(Long id) {
    String sql = "SELECT id, fkApprov, fkStock, qt, prixachat, prixachattotal, totalfournisseur, "
        + "datecreate, dateupdate, usercreateid, userupdateid " + "FROM lignes_approv WHERE id = ?";
    return queryForOptional(sql, LIGNE_APPROV_MAPPER, id);
  }

  @Override
  public List<LigneApprov> findByFkApprov(Long fkApprov) {
    String sql = "SELECT id, fkApprov, fkStock, qt, prixachat, prixachattotal, totalfournisseur, "
        + "datecreate, dateupdate, usercreateid, userupdateid "
        + "FROM lignes_approv WHERE fkApprov = ? ORDER BY datecreate";
    return jdbcTemplate.query(sql, LIGNE_APPROV_MAPPER, fkApprov);
  }

  @Override
  public int save(LigneApprov ligneApprov) {
    String sql =
        "INSERT INTO lignes_approv (fkApprov, fkStock, qt, prixachat, prixachattotal, totalfournisseur, "
            + "datecreate, usercreateid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    KeyHolder keyHolder = new GeneratedKeyHolder();
    int rowsAffected = jdbcTemplate.update(connection -> {
      java.sql.PreparedStatement ps =
          connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, ligneApprov.getFkApprov());
      if (ligneApprov.getFkStock() != null) {
        ps.setLong(2, ligneApprov.getFkStock());
      } else {
        ps.setNull(2, java.sql.Types.BIGINT);
      }
      if (ligneApprov.getQt() != null) {
        ps.setFloat(3, ligneApprov.getQt());
      } else {
        ps.setNull(3, java.sql.Types.FLOAT);
      }
      if (ligneApprov.getPrixachat() != null) {
        ps.setBigDecimal(4, ligneApprov.getPrixachat());
      } else {
        ps.setNull(4, java.sql.Types.DECIMAL);
      }
      if (ligneApprov.getPrixachattotal() != null) {
        ps.setBigDecimal(5, ligneApprov.getPrixachattotal());
      } else {
        ps.setNull(5, java.sql.Types.DECIMAL);
      }
      if (ligneApprov.getTotalfournisseur() != null) {
        ps.setBigDecimal(6, ligneApprov.getTotalfournisseur());
      } else {
        ps.setNull(6, java.sql.Types.DECIMAL);
      }
      ps.setTimestamp(7, Timestamp.valueOf(
          ligneApprov.getDateCreate() != null ? ligneApprov.getDateCreate() : LocalDateTime.now()));
      if (ligneApprov.getUserCreatedId() != null) {
        ps.setLong(8, ligneApprov.getUserCreatedId());
      } else {
        ps.setNull(8, java.sql.Types.BIGINT);
      }
      return ps;
    }, keyHolder);

    // Récupérer l'ID généré et l'assigner à l'objet
    if (rowsAffected > 0 && keyHolder.getKey() != null) {
      Long generatedId = keyHolder.getKey().longValue();
      ligneApprov.setId(generatedId);
      log.debug("ID généré pour la ligne d'approvisionnement: {}", generatedId);
    }

    return rowsAffected;
  }

  @Override
  public int update(LigneApprov ligneApprov) {
    String sql =
        "UPDATE lignes_approv SET fkApprov = ?, fkStock = ?, qt = ?, prixachat = ?, prixachattotal = ?, "
            + "totalfournisseur = ?, dateupdate = ?, userupdateid = ? WHERE id = ?";
    return update(sql, ligneApprov.getFkApprov(), ligneApprov.getFkStock(), ligneApprov.getQt(),
        ligneApprov.getPrixachat(), ligneApprov.getPrixachattotal(),
        ligneApprov.getTotalfournisseur(),
        Timestamp.valueOf(ligneApprov.getDateUpdate() != null ? ligneApprov.getDateUpdate()
            : LocalDateTime.now()),
        ligneApprov.getUserUpdatedId(), ligneApprov.getId());
  }

  @Override
  public int deleteById(Long id) {
    String sql = "DELETE FROM lignes_approv WHERE id = ?";
    return update(sql, id);
  }
}

