package cd.shad.erp.cmk.cmkerp.stocks.gmao.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model.PlanPreventif;
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

@Repository
@RequiredArgsConstructor
public class PlanPreventifJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final String SELECT_JOIN = """
      SELECT p.*, e.code_interne AS equipement_code, e.designation AS equipement_designation
      FROM gmao_plan_preventif p
      INNER JOIN gmao_equipement e ON e.id = p.fk_equipement
      """;

  private static final RowMapper<PlanPreventif> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

  public Long insert(PlanPreventif plan) {
    String sql = """
        INSERT INTO gmao_plan_preventif
        (fk_equipement, libelle, frequence_jours, prochaine_echeance, derniere_execution,
         actif, notes, datecreate, usercreateid)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, new String[] {"id"});
      int i = 1;
      ps.setLong(i++, plan.getFkEquipement());
      ps.setString(i++, plan.getLibelle());
      ps.setInt(i++, plan.getFrequenceJours());
      ps.setDate(i++, Date.valueOf(plan.getProchaineEcheance()));
      if (plan.getDerniereExecution() != null) {
        ps.setDate(i++, Date.valueOf(plan.getDerniereExecution()));
      } else {
        ps.setNull(i++, Types.DATE);
      }
      ps.setInt(i++, plan.isActif() ? 1 : 0);
      ps.setString(i++, plan.getNotes());
      ps.setTimestamp(i++, Timestamp.valueOf(now));
      if (plan.getUserCreateId() != null) {
        ps.setLong(i, plan.getUserCreateId());
      } else {
        ps.setNull(i, Types.BIGINT);
      }
      return ps;
    }, keyHolder);
    Number key = keyHolder.getKey();
    return key != null ? key.longValue() : null;
  }

  public int update(PlanPreventif plan) {
    String sql = """
        UPDATE gmao_plan_preventif SET
          fk_equipement = ?, libelle = ?, frequence_jours = ?, prochaine_echeance = ?,
          derniere_execution = ?, actif = ?, notes = ?, dateupdate = ?, userupdateid = ?
        WHERE id = ?
        """;
    return jdbcTemplate.update(sql,
        plan.getFkEquipement(),
        plan.getLibelle(),
        plan.getFrequenceJours(),
        Date.valueOf(plan.getProchaineEcheance()),
        plan.getDerniereExecution() != null ? Date.valueOf(plan.getDerniereExecution()) : null,
        plan.isActif() ? 1 : 0,
        plan.getNotes(),
        Timestamp.valueOf(LocalDateTime.now()),
        plan.getUserUpdateId(),
        plan.getId());
  }

  public Optional<PlanPreventif> findById(Long id) {
    List<PlanPreventif> rows =
        jdbcTemplate.query(SELECT_JOIN + " WHERE p.id = ?", ROW_MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<PlanPreventif> findAll(Long fkEquipement, Boolean actif, Boolean enRetardOnly,
      int limit, int offset) {
    StringBuilder sql = new StringBuilder(SELECT_JOIN + " WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (fkEquipement != null) {
      sql.append(" AND p.fk_equipement = ?");
      params.add(fkEquipement);
    }
    if (actif != null) {
      sql.append(" AND p.actif = ?");
      params.add(actif ? 1 : 0);
    }
    if (Boolean.TRUE.equals(enRetardOnly)) {
      sql.append(" AND p.actif = 1 AND p.prochaine_echeance < CURDATE()");
    }
    sql.append(" ORDER BY p.prochaine_echeance ASC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
  }

  public long count(Long fkEquipement, Boolean actif, Boolean enRetardOnly) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM gmao_plan_preventif p WHERE 1=1");
    List<Object> params = new ArrayList<>();
    if (fkEquipement != null) {
      sql.append(" AND p.fk_equipement = ?");
      params.add(fkEquipement);
    }
    if (actif != null) {
      sql.append(" AND p.actif = ?");
      params.add(actif ? 1 : 0);
    }
    if (Boolean.TRUE.equals(enRetardOnly)) {
      sql.append(" AND p.actif = 1 AND p.prochaine_echeance < CURDATE()");
    }
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return c != null ? c : 0;
  }

  public long countEnRetard() {
    Long c = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*) FROM gmao_plan_preventif
            WHERE actif = 1 AND prochaine_echeance < CURDATE()
            """,
        Long.class);
    return c != null ? c : 0;
  }

  private static PlanPreventif mapRow(ResultSet rs) throws SQLException {
    return PlanPreventif.builder()
        .id(rs.getLong("id"))
        .fkEquipement(rs.getLong("fk_equipement"))
        .equipementCode(rs.getString("equipement_code"))
        .equipementDesignation(rs.getString("equipement_designation"))
        .libelle(rs.getString("libelle"))
        .frequenceJours(rs.getInt("frequence_jours"))
        .prochaineEcheance(toLocalDate(rs.getDate("prochaine_echeance")))
        .derniereExecution(toLocalDate(rs.getDate("derniere_execution")))
        .actif(rs.getBoolean("actif"))
        .notes(rs.getString("notes"))
        .dateCreate(toLocalDateTime(rs.getTimestamp("datecreate")))
        .dateUpdate(toLocalDateTime(rs.getTimestamp("dateupdate")))
        .userCreateId(getLong(rs, "usercreateid"))
        .userUpdateId(getLong(rs, "userupdateid"))
        .build();
  }

  private static Long getLong(ResultSet rs, String col) throws SQLException {
    long v = rs.getLong(col);
    return rs.wasNull() ? null : v;
  }

  private static LocalDate toLocalDate(Date d) {
    return d != null ? d.toLocalDate() : null;
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime() : null;
  }
}
