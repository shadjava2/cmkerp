package cd.shad.erp.cmk.cmkerp.stocks.autorisations.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model.AutorisationOperation;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model.AutorisationOperation.StatutAutorisation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
public class AutorisationOperationJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<AutorisationOperation> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

  public Long insert(AutorisationOperation auth) {
    String sql = """
        INSERT INTO autorisations_operations
        (table_cible, enregistrement_id, type_operation, statut, motif, datecreate, usercreateid)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    LocalDateTime now = auth.getDateCreate() != null ? auth.getDateCreate() : LocalDateTime.now();
    jdbcTemplate.update(con -> {
      var ps = con.prepareStatement(sql, new String[] {"id"});
      ps.setString(1, auth.getTableCible());
      ps.setLong(2, auth.getEnregistrementId());
      ps.setString(3, auth.getTypeOperation());
      ps.setString(4, auth.getStatut().name());
      ps.setString(5, auth.getMotif());
      ps.setTimestamp(6, Timestamp.valueOf(now));
      if (auth.getUserCreateId() != null) {
        ps.setLong(7, auth.getUserCreateId());
      } else {
        ps.setNull(7, java.sql.Types.BIGINT);
      }
      return ps;
    }, keyHolder);
    Number key = keyHolder.getKey();
    return key != null ? key.longValue() : null;
  }

  public int updateDecision(AutorisationOperation auth) {
    String sql = """
        UPDATE autorisations_operations
        SET statut = ?, dateupdate = ?, userdecideid = ?, datedecision = ?, commentaire_decision = ?
        WHERE id = ?
        """;
    return jdbcTemplate.update(sql,
        auth.getStatut().name(),
        Timestamp.valueOf(auth.getDateUpdate()),
        auth.getUserDecideId(),
        Timestamp.valueOf(auth.getDateDecision()),
        auth.getCommentaireDecision(),
        auth.getId());
  }

  public Optional<AutorisationOperation> findById(Long id) {
    String sql = "SELECT * FROM autorisations_operations WHERE id = ?";
    List<AutorisationOperation> rows = jdbcTemplate.query(sql, ROW_MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public Optional<AutorisationOperation> findPending(String tableCible, Long enregistrementId,
      String typeOperation) {
    String sql = """
        SELECT * FROM autorisations_operations
        WHERE table_cible = ? AND enregistrement_id = ? AND type_operation = ?
          AND statut = 'EN_ATTENTE'
        ORDER BY datecreate DESC LIMIT 1
        """;
    List<AutorisationOperation> rows =
        jdbcTemplate.query(sql, ROW_MAPPER, tableCible, enregistrementId, typeOperation);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public Optional<AutorisationOperation> findApproved(String tableCible, Long enregistrementId,
      String typeOperation) {
    String sql = """
        SELECT * FROM autorisations_operations
        WHERE table_cible = ? AND enregistrement_id = ? AND type_operation = ?
          AND statut = 'APPROUVEE'
        ORDER BY datedecision DESC LIMIT 1
        """;
    List<AutorisationOperation> rows =
        jdbcTemplate.query(sql, ROW_MAPPER, tableCible, enregistrementId, typeOperation);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<AutorisationOperation> findAll(String statut, int limit, int offset) {
    StringBuilder sql = new StringBuilder("SELECT * FROM autorisations_operations");
    List<Object> params = new ArrayList<>();
    if (statut != null && !statut.isBlank()) {
      sql.append(" WHERE statut = ?");
      params.add(statut);
    }
    sql.append(" ORDER BY datecreate DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
  }

  public long count(String statut) {
    if (statut != null && !statut.isBlank()) {
      Long c = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM autorisations_operations WHERE statut = ?", Long.class, statut);
      return c != null ? c : 0;
    }
    Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM autorisations_operations", Long.class);
    return c != null ? c : 0;
  }

  private static AutorisationOperation mapRow(ResultSet rs) throws SQLException {
    return AutorisationOperation.builder()
        .id(rs.getLong("id"))
        .tableCible(rs.getString("table_cible"))
        .enregistrementId(rs.getLong("enregistrement_id"))
        .typeOperation(rs.getString("type_operation"))
        .statut(StatutAutorisation.valueOf(rs.getString("statut")))
        .motif(rs.getString("motif"))
        .dateCreate(toLocalDateTime(rs.getTimestamp("datecreate")))
        .dateUpdate(toLocalDateTime(rs.getTimestamp("dateupdate")))
        .userCreateId(rs.getObject("usercreateid") != null ? rs.getLong("usercreateid") : null)
        .userDecideId(rs.getObject("userdecideid") != null ? rs.getLong("userdecideid") : null)
        .dateDecision(toLocalDateTime(rs.getTimestamp("datedecision")))
        .commentaireDecision(rs.getString("commentaire_decision"))
        .build();
  }

  private static LocalDateTime toLocalDateTime(Timestamp ts) {
    return ts != null ? ts.toLocalDateTime() : null;
  }
}
