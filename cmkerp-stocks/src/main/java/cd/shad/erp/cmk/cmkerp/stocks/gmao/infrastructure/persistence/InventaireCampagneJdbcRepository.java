package cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireCampagne;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireCampagne.Statut;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class InventaireCampagneJdbcRepository {

  private final JdbcTemplate jdbcTemplate;
  private static final RowMapper<InventaireCampagne> ROW_MAPPER = (rs, n) -> mapRow(rs);

  public Long insert(InventaireCampagne c) {
    String sql = """
        INSERT INTO gmao_inventaire_campagne
        (numero, libelle, date_debut, date_fin_prevue, statut, perimetre_service, perimetre_categorie,
         responsable, notes, datecreate, usercreateid)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    KeyHolder kh = new GeneratedKeyHolder();
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
      int i = 1;
      ps.setString(i++, c.getNumero());
      ps.setString(i++, c.getLibelle());
      ps.setDate(i++, Date.valueOf(c.getDateDebut()));
      setDate(ps, i++, c.getDateFinPrevue());
      ps.setString(i++, c.getStatut().name());
      ps.setString(i++, c.getPerimetreService());
      ps.setString(i++, c.getPerimetreCategorie());
      ps.setString(i++, c.getResponsable());
      ps.setString(i++, c.getNotes());
      ps.setTimestamp(i++, Timestamp.valueOf(now));
      setLong(ps, i, c.getUserCreateId());
      return ps;
    }, kh);
    Number key = kh.getKey();
    return key != null ? key.longValue() : null;
  }

  public int update(InventaireCampagne c) {
    return jdbcTemplate.update("""
        UPDATE gmao_inventaire_campagne SET
          libelle = ?, date_debut = ?, date_fin_prevue = ?, date_cloture = ?, statut = ?,
          perimetre_service = ?, perimetre_categorie = ?, responsable = ?, notes = ?,
          dateupdate = ?, userupdateid = ?
        WHERE id = ?
        """,
        c.getLibelle(),
        Date.valueOf(c.getDateDebut()),
        c.getDateFinPrevue() != null ? Date.valueOf(c.getDateFinPrevue()) : null,
        c.getDateCloture() != null ? Timestamp.valueOf(c.getDateCloture()) : null,
        c.getStatut().name(),
        c.getPerimetreService(),
        c.getPerimetreCategorie(),
        c.getResponsable(),
        c.getNotes(),
        Timestamp.valueOf(LocalDateTime.now()),
        c.getUserUpdateId(),
        c.getId());
  }

  public Optional<InventaireCampagne> findById(Long id) {
    List<InventaireCampagne> rows =
        jdbcTemplate.query("SELECT * FROM gmao_inventaire_campagne WHERE id = ?", ROW_MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<InventaireCampagne> findAll(String statut, String search, int limit, int offset) {
    StringBuilder sql = new StringBuilder("SELECT * FROM gmao_inventaire_campagne WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (StringUtils.hasText(statut)) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    if (StringUtils.hasText(search)) {
      sql.append(" AND (numero LIKE ? OR libelle LIKE ? OR responsable LIKE ?)");
      String like = "%" + search.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
  }

  public long count(String statut, String search) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM gmao_inventaire_campagne WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (StringUtils.hasText(statut)) {
      sql.append(" AND statut = ?");
      params.add(statut);
    }
    if (StringUtils.hasText(search)) {
      sql.append(" AND (numero LIKE ? OR libelle LIKE ? OR responsable LIKE ?)");
      String like = "%" + search.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0;
  }

  public long countByPrefix(String prefix) {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_inventaire_campagne WHERE numero LIKE ?",
        Long.class, prefix + "%");
    return c != null ? c : 0;
  }

  private static InventaireCampagne mapRow(ResultSet rs) throws SQLException {
    return InventaireCampagne.builder()
        .id(rs.getLong("id"))
        .numero(rs.getString("numero"))
        .libelle(rs.getString("libelle"))
        .dateDebut(rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null)
        .dateFinPrevue(rs.getDate("date_fin_prevue") != null
            ? rs.getDate("date_fin_prevue").toLocalDate() : null)
        .dateCloture(rs.getTimestamp("date_cloture") != null
            ? rs.getTimestamp("date_cloture").toLocalDateTime() : null)
        .statut(Statut.valueOf(rs.getString("statut")))
        .perimetreService(rs.getString("perimetre_service"))
        .perimetreCategorie(rs.getString("perimetre_categorie"))
        .responsable(rs.getString("responsable"))
        .notes(rs.getString("notes"))
        .dateCreate(rs.getTimestamp("datecreate") != null
            ? rs.getTimestamp("datecreate").toLocalDateTime() : null)
        .dateUpdate(rs.getTimestamp("dateupdate") != null
            ? rs.getTimestamp("dateupdate").toLocalDateTime() : null)
        .userCreateId(rs.getObject("usercreateid") != null ? rs.getLong("usercreateid") : null)
        .userUpdateId(rs.getObject("userupdateid") != null ? rs.getLong("userupdateid") : null)
        .build();
  }

  private static void setDate(PreparedStatement ps, int i, LocalDate d) throws SQLException {
    if (d != null) ps.setDate(i, Date.valueOf(d));
    else ps.setNull(i, Types.DATE);
  }

  private static void setLong(PreparedStatement ps, int i, Long v) throws SQLException {
    if (v != null) ps.setLong(i, v);
    else ps.setNull(i, Types.BIGINT);
  }
}
