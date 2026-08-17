package cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireLigne;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.InventaireLigne.Resultat;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
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
public class InventaireLigneJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<InventaireLigne> ROW_MAPPER = (rs, n) -> mapRow(rs);

  public Long insert(InventaireLigne l) {
    String sql = """
        INSERT INTO gmao_inventaire_ligne
        (fk_campagne, fk_equipement, resultat, localisation_systeme, localisation_constatee,
         etat_constate, fonctionnement_constate, consommables_ok, pieces_ok, manuel_utilisateur_ok,
         manuel_technique_ok, accessoires_ok, remarque, inventoriste, date_controle, ecart,
         datecreate, usercreateid)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    KeyHolder kh = new GeneratedKeyHolder();
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
      bindWrite(ps, l, true);
      return ps;
    }, kh);
    Number key = kh.getKey();
    return key != null ? key.longValue() : null;
  }

  public int update(InventaireLigne l) {
    return jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement("""
          UPDATE gmao_inventaire_ligne SET
            resultat = ?, localisation_systeme = ?, localisation_constatee = ?,
            etat_constate = ?, fonctionnement_constate = ?, consommables_ok = ?, pieces_ok = ?,
            manuel_utilisateur_ok = ?, manuel_technique_ok = ?, accessoires_ok = ?,
            remarque = ?, inventoriste = ?, date_controle = ?, ecart = ?,
            dateupdate = ?, userupdateid = ?
          WHERE id = ?
          """);
      int i = bindUpdate(ps, l);
      ps.setLong(i, l.getId());
      return ps;
    });
  }

  public Optional<InventaireLigne> findById(Long id) {
    List<InventaireLigne> rows = jdbcTemplate.query("""
        SELECT l.*, e.code_interne AS eq_code, e.designation AS eq_designation,
               e.service AS eq_service, e.statut AS eq_statut
        FROM gmao_inventaire_ligne l
        JOIN gmao_equipement e ON e.id = l.fk_equipement
        WHERE l.id = ?
        """, ROW_MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<InventaireLigne> findByCampagne(Long fkCampagne, String resultat, String search,
      int limit, int offset) {
    StringBuilder sql = new StringBuilder("""
        SELECT l.*, e.code_interne AS eq_code, e.designation AS eq_designation,
               e.service AS eq_service, e.statut AS eq_statut
        FROM gmao_inventaire_ligne l
        JOIN gmao_equipement e ON e.id = l.fk_equipement
        WHERE l.fk_campagne = ?
        """);
    List<Object> params = new ArrayList<>();
    params.add(fkCampagne);
    if (StringUtils.hasText(resultat)) {
      sql.append(" AND l.resultat = ?");
      params.add(resultat);
    }
    if (StringUtils.hasText(search)) {
      sql.append(" AND (e.code_interne LIKE ? OR e.designation LIKE ? OR e.service LIKE ?)");
      String like = "%" + search.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    sql.append(" ORDER BY CASE l.resultat WHEN 'A_VERIFIER' THEN 0 ELSE 1 END, e.designation ASC");
    sql.append(" LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
  }

  public long countByCampagne(Long fkCampagne, String resultat, String search) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*) FROM gmao_inventaire_ligne l
        JOIN gmao_equipement e ON e.id = l.fk_equipement
        WHERE l.fk_campagne = ?
        """);
    List<Object> params = new ArrayList<>();
    params.add(fkCampagne);
    if (StringUtils.hasText(resultat)) {
      sql.append(" AND l.resultat = ?");
      params.add(resultat);
    }
    if (StringUtils.hasText(search)) {
      sql.append(" AND (e.code_interne LIKE ? OR e.designation LIKE ? OR e.service LIKE ?)");
      String like = "%" + search.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0;
  }

  public long countByResultat(Long fkCampagne, String resultat) {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_inventaire_ligne WHERE fk_campagne = ? AND resultat = ?",
        Long.class, fkCampagne, resultat);
    return c != null ? c : 0;
  }

  public long countEcarts(Long fkCampagne) {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_inventaire_ligne WHERE fk_campagne = ? AND ecart = 1",
        Long.class, fkCampagne);
    return c != null ? c : 0;
  }

  public boolean exists(Long fkCampagne, Long fkEquipement) {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_inventaire_ligne WHERE fk_campagne = ? AND fk_equipement = ?",
        Long.class, fkCampagne, fkEquipement);
    return c != null && c > 0;
  }

  private void bindWrite(PreparedStatement ps, InventaireLigne l, boolean insert) throws SQLException {
    int i = 1;
    ps.setLong(i++, l.getFkCampagne());
    ps.setLong(i++, l.getFkEquipement());
    ps.setString(i++, l.getResultat().name());
    ps.setString(i++, l.getLocalisationSysteme());
    ps.setString(i++, l.getLocalisationConstatee());
    ps.setString(i++, l.getEtatConstate());
    ps.setString(i++, l.getFonctionnementConstate());
    setBool(ps, i++, l.getConsommablesOk());
    setBool(ps, i++, l.getPiecesOk());
    setBool(ps, i++, l.getManuelUtilisateurOk());
    setBool(ps, i++, l.getManuelTechniqueOk());
    setBool(ps, i++, l.getAccessoiresOk());
    ps.setString(i++, l.getRemarque());
    ps.setString(i++, l.getInventoriste());
    if (l.getDateControle() != null) {
      ps.setTimestamp(i++, Timestamp.valueOf(l.getDateControle()));
    } else {
      ps.setNull(i++, Types.TIMESTAMP);
    }
    ps.setInt(i++, l.isEcart() ? 1 : 0);
    ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
    if (l.getUserCreateId() != null) {
      ps.setLong(i, l.getUserCreateId());
    } else {
      ps.setNull(i, Types.BIGINT);
    }
  }

  private int bindUpdate(PreparedStatement ps, InventaireLigne l) throws SQLException {
    int i = 1;
    ps.setString(i++, l.getResultat().name());
    ps.setString(i++, l.getLocalisationSysteme());
    ps.setString(i++, l.getLocalisationConstatee());
    ps.setString(i++, l.getEtatConstate());
    ps.setString(i++, l.getFonctionnementConstate());
    setBool(ps, i++, l.getConsommablesOk());
    setBool(ps, i++, l.getPiecesOk());
    setBool(ps, i++, l.getManuelUtilisateurOk());
    setBool(ps, i++, l.getManuelTechniqueOk());
    setBool(ps, i++, l.getAccessoiresOk());
    ps.setString(i++, l.getRemarque());
    ps.setString(i++, l.getInventoriste());
    if (l.getDateControle() != null) {
      ps.setTimestamp(i++, Timestamp.valueOf(l.getDateControle()));
    } else {
      ps.setNull(i++, Types.TIMESTAMP);
    }
    ps.setInt(i++, l.isEcart() ? 1 : 0);
    ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
    if (l.getUserUpdateId() != null) {
      ps.setLong(i++, l.getUserUpdateId());
    } else {
      ps.setNull(i++, Types.BIGINT);
    }
    return i;
  }

  private static InventaireLigne mapRow(ResultSet rs) throws SQLException {
    return InventaireLigne.builder()
        .id(rs.getLong("id"))
        .fkCampagne(rs.getLong("fk_campagne"))
        .fkEquipement(rs.getLong("fk_equipement"))
        .resultat(Resultat.valueOf(rs.getString("resultat")))
        .localisationSysteme(rs.getString("localisation_systeme"))
        .localisationConstatee(rs.getString("localisation_constatee"))
        .etatConstate(rs.getString("etat_constate"))
        .fonctionnementConstate(rs.getString("fonctionnement_constate"))
        .consommablesOk(getBool(rs, "consommables_ok"))
        .piecesOk(getBool(rs, "pieces_ok"))
        .manuelUtilisateurOk(getBool(rs, "manuel_utilisateur_ok"))
        .manuelTechniqueOk(getBool(rs, "manuel_technique_ok"))
        .accessoiresOk(getBool(rs, "accessoires_ok"))
        .remarque(rs.getString("remarque"))
        .inventoriste(rs.getString("inventoriste"))
        .dateControle(rs.getTimestamp("date_controle") != null
            ? rs.getTimestamp("date_controle").toLocalDateTime() : null)
        .ecart(rs.getBoolean("ecart"))
        .dateCreate(rs.getTimestamp("datecreate") != null
            ? rs.getTimestamp("datecreate").toLocalDateTime() : null)
        .dateUpdate(rs.getTimestamp("dateupdate") != null
            ? rs.getTimestamp("dateupdate").toLocalDateTime() : null)
        .userCreateId(rs.getObject("usercreateid") != null ? rs.getLong("usercreateid") : null)
        .userUpdateId(rs.getObject("userupdateid") != null ? rs.getLong("userupdateid") : null)
        .equipementCode(getOpt(rs, "eq_code"))
        .equipementDesignation(getOpt(rs, "eq_designation"))
        .equipementService(getOpt(rs, "eq_service"))
        .equipementStatut(getOpt(rs, "eq_statut"))
        .build();
  }

  private static void setBool(PreparedStatement ps, int i, Boolean v) throws SQLException {
    if (v == null) ps.setNull(i, Types.TINYINT);
    else ps.setInt(i, v ? 1 : 0);
  }

  private static Boolean getBool(ResultSet rs, String col) throws SQLException {
    Object o = rs.getObject(col);
    if (o == null) return null;
    return rs.getBoolean(col);
  }

  private static String getOpt(ResultSet rs, String col) {
    try {
      return rs.getString(col);
    } catch (SQLException e) {
      return null;
    }
  }
}
