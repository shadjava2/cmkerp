package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Column;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Kind;

/**
 * Lecture paramétrée CLINIQUE Beach. Colonnes explicites du catalogue. Jamais de SELECT *.
 */
@Component
public class ClinicReferentialLegacyReader {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final JdbcTemplate cliniqueJdbc;

  public ClinicReferentialLegacyReader(
      @Autowired(required = false) @Qualifier("cliniqueJdbcTemplate") JdbcTemplate cliniqueJdbc) {
    this.cliniqueJdbc = cliniqueJdbc;
  }

  public boolean available() {
    return cliniqueJdbc != null;
  }

  public int count(Definition def, String q) {
    StringBuilder sql = new StringBuilder("SELECT COUNT_BIG(*) FROM ").append(def.qualifiedTable());
    List<Object> args = new ArrayList<>();
    appendSearch(sql, args, def, q);
    Long n = cliniqueJdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    return n == null ? 0 : n.intValue();
  }

  public List<Map<String, Object>> page(Definition def, String q, int offset, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, ClinicReferentialCatalog.FULL_COMPARE_LIMIT));
    int safeOffset = Math.max(0, offset);
    StringBuilder sql = new StringBuilder("SELECT ").append(selectList(def))
        .append(" FROM ").append(def.qualifiedTable());
    List<Object> args = new ArrayList<>();
    appendSearch(sql, args, def, q);
    sql.append(" ORDER BY ").append(Definition.bracket(def.keyColumn()))
        .append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
    args.add(safeOffset);
    args.add(safeLimit);
    return cliniqueJdbc.query(sql.toString(), (rs, i) -> mapRow(def, rs), args.toArray());
  }

  public List<Map<String, Object>> findByKeys(Definition def, List<String> keys) {
    if (cliniqueJdbc == null || keys == null || keys.isEmpty()) {
      return List.of();
    }
    List<Map<String, Object>> out = new ArrayList<>();
    boolean numeric = def.keyColumnDef().kind() == Kind.INT;
    for (int i = 0; i < keys.size(); i += 80) {
      List<String> chunk = keys.subList(i, Math.min(i + 80, keys.size()));
      String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
      String column = Definition.bracket(def.keyColumn());
      String where = numeric
          ? column + " IN (" + placeholders + ")"
          : "RTRIM(" + column + ") IN (" + placeholders + ")";
      String sql = "SELECT " + selectList(def) + " FROM " + def.qualifiedTable() + " WHERE " + where;
      Object[] args = numeric
          ? chunk.stream().map(Integer::valueOf).toArray()
          : chunk.toArray();
      out.addAll(cliniqueJdbc.query(sql, (rs, row) -> mapRow(def, rs), args));
    }
    return out;
  }

  static String selectList(Definition def) {
    StringBuilder sql = new StringBuilder();
    for (Column column : def.columns()) {
      if (!sql.isEmpty()) {
        sql.append(", ");
      }
      String quoted = Definition.bracket(column.name());
      if (column.kind() == Kind.STRING) {
        sql.append("RTRIM(").append(quoted).append(") AS ").append(quoted);
      } else {
        sql.append(quoted);
      }
    }
    return sql.toString();
  }

  private static void appendSearch(StringBuilder sql, List<Object> args, Definition def, String q) {
    if (q == null || q.isBlank()) {
      return;
    }
    String needle = "%" + q.trim().replace("%", "").replace("_", "") + "%";
    sql.append(" WHERE (");
    String key = Definition.bracket(def.keyColumn());
    if (def.keyColumnDef().kind() == Kind.STRING) {
      sql.append("RTRIM(").append(key).append(") LIKE ?");
    } else {
      sql.append("CAST(").append(key).append(" AS varchar(20)) LIKE ?");
    }
    args.add(needle);
    if (!def.labelColumn().equals(def.keyColumn())) {
      sql.append(" OR RTRIM(").append(Definition.bracket(def.labelColumn())).append(") LIKE ?");
      args.add(needle);
    }
    sql.append(')');
  }

  private static Map<String, Object> mapRow(Definition def, ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    for (Column column : def.columns()) {
      row.put(column.name(), read(column, rs));
    }
    return row;
  }

  private static Object read(Column column, ResultSet rs) throws SQLException {
    return switch (column.kind()) {
      case STRING -> {
        String value = rs.getString(column.name());
        yield value == null ? null : value.trim();
      }
      case INT -> {
        int value = rs.getInt(column.name());
        yield rs.wasNull() ? null : value;
      }
      case DECIMAL -> {
        double value = rs.getDouble(column.name());
        yield rs.wasNull() ? null : value;
      }
      case BIT -> {
        boolean value = rs.getBoolean(column.name());
        yield rs.wasNull() ? null : value;
      }
      case DATETIME -> {
        Timestamp timestamp = rs.getTimestamp(column.name());
        yield timestamp == null ? null : timestamp.toLocalDateTime().withNano(0).format(ISO);
      }
    };
  }
}
