package cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Intervention;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Intervention.Priorite;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Intervention.Statut;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.Intervention.Type;
import java.math.BigDecimal;
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
public class InterventionJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final String SELECT_JOIN = """
      SELECT i.*, e.code_interne AS equipement_code, e.designation AS equipement_designation
      FROM gmao_intervention i
      INNER JOIN gmao_equipement e ON e.id = i.fk_equipement
      """;

  private static final RowMapper<Intervention> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

  public Long insert(Intervention it) {
    String sql = """
        INSERT INTO gmao_intervention
        (numero, fk_equipement, type_intervention, priorite, statut, titre, description, diagnostic,
         travaux_realises, technicien_nom, technicien_user_id, fk_pharmacie, date_demande,
         date_planifiee, date_debut, date_cloture, cout_estime, cout_reel, datecreate, usercreateid)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
      int i = 1;
      ps.setString(i++, it.getNumero());
      ps.setLong(i++, it.getFkEquipement());
      ps.setString(i++, it.getTypeIntervention().name());
      ps.setString(i++, it.getPriorite().name());
      ps.setString(i++, it.getStatut().name());
      ps.setString(i++, it.getTitre());
      ps.setString(i++, it.getDescription());
      ps.setString(i++, it.getDiagnostic());
      ps.setString(i++, it.getTravauxRealises());
      ps.setString(i++, it.getTechnicienNom());
      setNullableLong(ps, i++, it.getTechnicienUserId());
      setNullableLong(ps, i++, it.getFkPharmacie());
      ps.setTimestamp(i++, Timestamp.valueOf(it.getDateDemande() != null ? it.getDateDemande() : now));
      setNullableTs(ps, i++, it.getDatePlanifiee());
      setNullableTs(ps, i++, it.getDateDebut());
      setNullableTs(ps, i++, it.getDateCloture());
      setNullableDecimal(ps, i++, it.getCoutEstime());
      setNullableDecimal(ps, i++, it.getCoutReel());
      ps.setTimestamp(i++, Timestamp.valueOf(now));
      setNullableLong(ps, i, it.getUserCreateId());
      return ps;
    }, keyHolder);
    Number key = keyHolder.getKey();
    return key != null ? key.longValue() : null;
  }

  public int update(Intervention it) {
    String sql = """
        UPDATE gmao_intervention SET
          fk_equipement = ?, type_intervention = ?, priorite = ?, statut = ?, titre = ?,
          description = ?, diagnostic = ?, travaux_realises = ?, technicien_nom = ?,
          technicien_user_id = ?, fk_pharmacie = ?, date_planifiee = ?, date_debut = ?,
          date_cloture = ?, cout_estime = ?, cout_reel = ?, dateupdate = ?, userupdateid = ?
        WHERE id = ?
        """;
    return jdbcTemplate.update(sql,
        it.getFkEquipement(),
        it.getTypeIntervention().name(),
        it.getPriorite().name(),
        it.getStatut().name(),
        it.getTitre(),
        it.getDescription(),
        it.getDiagnostic(),
        it.getTravauxRealises(),
        it.getTechnicienNom(),
        it.getTechnicienUserId(),
        it.getFkPharmacie(),
        it.getDatePlanifiee() != null ? Timestamp.valueOf(it.getDatePlanifiee()) : null,
        it.getDateDebut() != null ? Timestamp.valueOf(it.getDateDebut()) : null,
        it.getDateCloture() != null ? Timestamp.valueOf(it.getDateCloture()) : null,
        it.getCoutEstime(),
        it.getCoutReel(),
        Timestamp.valueOf(LocalDateTime.now()),
        it.getUserUpdateId(),
        it.getId());
  }

  public Optional<Intervention> findById(Long id) {
    List<Intervention> rows =
        jdbcTemplate.query(SELECT_JOIN + " WHERE i.id = ?", ROW_MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<Intervention> findAll(Long fkPharmacie, Long fkEquipement, String statut,
      String type, String search, int limit, int offset) {
    StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE 1=1");
    List<Object> params = new ArrayList<>();
    appendFilters(sql, params, fkPharmacie, fkEquipement, statut, type, search);
    sql.append(" ORDER BY i.date_demande DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
  }

  public long count(Long fkPharmacie, Long fkEquipement, String statut, String type, String search) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*) FROM gmao_intervention i
        INNER JOIN gmao_equipement e ON e.id = i.fk_equipement
        WHERE 1=1
        """);
    List<Object> params = new ArrayList<>();
    appendFilters(sql, params, fkPharmacie, fkEquipement, statut, type, search);
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0;
  }

  public long countByStatut(String statut) {
    Long c = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM gmao_intervention WHERE statut = ?", Long.class, statut);
    return c != null ? c : 0;
  }

  public long countOpen() {
    Long c = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*) FROM gmao_intervention
            WHERE statut IN ('BROUILLON','PLANIFIEE','EN_COURS')
            """,
        Long.class);
    return c != null ? c : 0;
  }

  public String nextNumero(LocalDateTime now) {
    String prefix = "OT-" + now.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-";
    String sql = """
        SELECT COALESCE(MAX(CAST(SUBSTRING(numero, ?) AS UNSIGNED)), 0)
        FROM gmao_intervention WHERE numero LIKE ?
        """;
    Integer seqStart = prefix.length() + 1;
    Integer max = jdbcTemplate.queryForObject(sql, Integer.class, seqStart, prefix + "%");
    int next = (max != null ? max : 0) + 1;
    return prefix + String.format("%04d", next);
  }

  private void appendFilters(StringBuilder sql, List<Object> params, Long fkPharmacie,
      Long fkEquipement, String statut, String type, String search) {
    if (fkPharmacie != null) {
      sql.append(" AND i.fk_pharmacie = ?");
      params.add(fkPharmacie);
    }
    if (fkEquipement != null) {
      sql.append(" AND i.fk_equipement = ?");
      params.add(fkEquipement);
    }
    if (StringUtils.hasText(statut)) {
      sql.append(" AND i.statut = ?");
      params.add(statut);
    }
    if (StringUtils.hasText(type)) {
      sql.append(" AND i.type_intervention = ?");
      params.add(type);
    }
    if (StringUtils.hasText(search)) {
      sql.append("""
           AND (i.numero LIKE ? OR i.titre LIKE ? OR i.technicien_nom LIKE ?
                OR e.code_interne LIKE ? OR e.designation LIKE ?)
          """);
      String like = "%" + search.trim() + "%";
      for (int i = 0; i < 5; i++) {
        params.add(like);
      }
    }
  }

  private static Intervention mapRow(ResultSet rs) throws SQLException {
    return Intervention.builder()
        .id(rs.getLong("id"))
        .numero(rs.getString("numero"))
        .fkEquipement(rs.getLong("fk_equipement"))
        .equipementCode(rs.getString("equipement_code"))
        .equipementDesignation(rs.getString("equipement_designation"))
        .typeIntervention(Type.valueOf(rs.getString("type_intervention")))
        .priorite(Priorite.valueOf(rs.getString("priorite")))
        .statut(Statut.valueOf(rs.getString("statut")))
        .titre(rs.getString("titre"))
        .description(rs.getString("description"))
        .diagnostic(rs.getString("diagnostic"))
        .travauxRealises(rs.getString("travaux_realises"))
        .technicienNom(rs.getString("technicien_nom"))
        .technicienUserId(getLong(rs, "technicien_user_id"))
        .fkPharmacie(getLong(rs, "fk_pharmacie"))
        .dateDemande(toLocalDateTime(rs.getTimestamp("date_demande")))
        .datePlanifiee(toLocalDateTime(rs.getTimestamp("date_planifiee")))
        .dateDebut(toLocalDateTime(rs.getTimestamp("date_debut")))
        .dateCloture(toLocalDateTime(rs.getTimestamp("date_cloture")))
        .coutEstime(rs.getBigDecimal("cout_estime"))
        .coutReel(rs.getBigDecimal("cout_reel"))
        .dateCreate(toLocalDateTime(rs.getTimestamp("datecreate")))
        .dateUpdate(toLocalDateTime(rs.getTimestamp("dateupdate")))
        .userCreateId(getLong(rs, "usercreateid"))
        .userUpdateId(getLong(rs, "userupdateid"))
        .build();
  }

  private static void setNullableLong(PreparedStatement ps, int index, Long value)
      throws SQLException {
    if (value != null) {
      ps.setLong(index, value);
    } else {
      ps.setNull(index, Types.BIGINT);
    }
  }

  private static void setNullableTs(PreparedStatement ps, int index, LocalDateTime value)
      throws SQLException {
    if (value != null) {
      ps.setTimestamp(index, Timestamp.valueOf(value));
    } else {
      ps.setNull(index, Types.TIMESTAMP);
    }
  }

  private static void setNullableDecimal(PreparedStatement ps, int index, BigDecimal value)
      throws SQLException {
    if (value != null) {
      ps.setBigDecimal(index, value);
    } else {
      ps.setNull(index, Types.DECIMAL);
    }
  }

  private static Long getLong(ResultSet rs, String col) throws SQLException {
    long v = rs.getLong(col);
    return rs.wasNull() ? null : v;
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime() : null;
  }
}
