package cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.EquipementMedia;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.EquipementMedia.TypeMedia;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EquipementMediaJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<EquipementMedia> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

  public Long insert(EquipementMedia m) {
    String sql = """
        INSERT INTO gmao_equipement_media
        (fk_equipement, type_media, nom_fichier, nom_original, content_type, taille_octets,
         storage_key, legende, est_principal, datecreate, usercreateid)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
      int i = 1;
      ps.setLong(i++, m.getFkEquipement());
      ps.setString(i++, m.getTypeMedia().name());
      ps.setString(i++, m.getNomFichier());
      ps.setString(i++, m.getNomOriginal());
      ps.setString(i++, m.getContentType());
      ps.setLong(i++, m.getTailleOctets());
      ps.setString(i++, m.getStorageKey());
      ps.setString(i++, m.getLegende());
      ps.setInt(i++, m.isEstPrincipal() ? 1 : 0);
      ps.setTimestamp(i++, Timestamp.valueOf(now));
      if (m.getUserCreateId() != null) {
        ps.setLong(i, m.getUserCreateId());
      } else {
        ps.setNull(i, Types.BIGINT);
      }
      return ps;
    }, keyHolder);
    Number key = keyHolder.getKey();
    return key != null ? key.longValue() : null;
  }

  public Optional<EquipementMedia> findById(Long id) {
    List<EquipementMedia> rows = jdbcTemplate.query(
        "SELECT * FROM gmao_equipement_media WHERE id = ?", ROW_MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<EquipementMedia> findByEquipement(Long fkEquipement) {
    return jdbcTemplate.query(
        """
            SELECT * FROM gmao_equipement_media
            WHERE fk_equipement = ?
            ORDER BY est_principal DESC, datecreate DESC
            """,
        ROW_MAPPER, fkEquipement);
  }

  public Optional<EquipementMedia> findPrincipal(Long fkEquipement) {
    List<EquipementMedia> rows = jdbcTemplate.query(
        """
            SELECT * FROM gmao_equipement_media
            WHERE fk_equipement = ? AND est_principal = 1
            ORDER BY id DESC LIMIT 1
            """,
        ROW_MAPPER, fkEquipement);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public Long findPrincipalId(Long fkEquipement) {
    List<Long> ids = jdbcTemplate.query(
        """
            SELECT id FROM gmao_equipement_media
            WHERE fk_equipement = ? AND est_principal = 1
            ORDER BY id DESC LIMIT 1
            """,
        (rs, rowNum) -> rs.getLong("id"), fkEquipement);
    return ids.isEmpty() ? null : ids.get(0);
  }

  public int countByEquipement(Long fkEquipement) {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_equipement_media WHERE fk_equipement = ?",
        Long.class, fkEquipement);
    return c != null ? c.intValue() : 0;
  }

  public void clearPrincipal(Long fkEquipement) {
    jdbcTemplate.update(
        "UPDATE gmao_equipement_media SET est_principal = 0 WHERE fk_equipement = ?",
        fkEquipement);
  }

  public void setPrincipal(Long id, Long fkEquipement) {
    clearPrincipal(fkEquipement);
    jdbcTemplate.update(
        "UPDATE gmao_equipement_media SET est_principal = 1 WHERE id = ? AND fk_equipement = ?",
        id, fkEquipement);
  }

  public int delete(Long id) {
    return jdbcTemplate.update("DELETE FROM gmao_equipement_media WHERE id = ?", id);
  }

  private static EquipementMedia mapRow(ResultSet rs) throws SQLException {
    return EquipementMedia.builder()
        .id(rs.getLong("id"))
        .fkEquipement(rs.getLong("fk_equipement"))
        .typeMedia(TypeMedia.valueOf(rs.getString("type_media")))
        .nomFichier(rs.getString("nom_fichier"))
        .nomOriginal(rs.getString("nom_original"))
        .contentType(rs.getString("content_type"))
        .tailleOctets(rs.getLong("taille_octets"))
        .storageKey(rs.getString("storage_key"))
        .legende(rs.getString("legende"))
        .estPrincipal(rs.getBoolean("est_principal"))
        .dateCreate(rs.getTimestamp("datecreate") != null
            ? rs.getTimestamp("datecreate").toLocalDateTime() : null)
        .userCreateId(rs.getObject("usercreateid") != null ? rs.getLong("usercreateid") : null)
        .build();
  }
}
