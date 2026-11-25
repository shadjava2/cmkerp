package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Column;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Kind;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * INSERT / UPDATE paramétrés vers Beach, colonnes du catalogue uniquement.
 * Aucun DELETE, aucune colonne secrète, aucun DDL.
 */
@Component
public class ClinicReferentialLegacyWriter {

  private final ClinicLegacyWriteGuard writeGuard;
  private final JdbcTemplate cliniqueJdbc;

  public ClinicReferentialLegacyWriter(
      ClinicLegacyWriteGuard writeGuard,
      @Autowired(required = false) @Qualifier("cliniqueJdbcTemplate") JdbcTemplate cliniqueJdbc) {
    this.writeGuard = writeGuard;
    this.cliniqueJdbc = cliniqueJdbc;
  }

  public void apply(Definition def, String operation, String naturalKey, Map<String, Object> fields) {
    if (!def.outbound()) {
      throw new IllegalStateException("Réplication sortante interdite pour " + def.label());
    }
    if (cliniqueJdbc == null) {
      throw new IllegalStateException("Source historique indisponible");
    }
    String op = operation == null ? "" : operation.trim().toUpperCase();
    if ("DELETE".equals(op)) {
      writeGuard.assertBeachDeleteAllowed(def.entityType());
      throw new IllegalStateException("Suppression historique non autorisée");
    }
    writeGuard.assertBeachWriteAllowed(def.entityType() + "." + op);
    writeGuard.assertChWriteForbidden();
    Map<String, Object> normalized = ClinicReferentialValues.normalize(def, fields, true, true);
    Object keyParam = keyParam(def, naturalKey);
    boolean exists = exists(def, keyParam);
    if ("INSERT".equals(op)) {
      if (!def.outboundInsert()) {
        throw new IllegalStateException("Création historique refusée pour " + def.label());
      }
      if (exists) {
        update(def, keyParam, normalized);
      } else {
        insert(def, normalized);
      }
      return;
    }
    if ("UPDATE".equals(op)) {
      int updated = update(def, keyParam, normalized);
      if (updated == 0) {
        if (!def.outboundInsert()) {
          throw new IllegalStateException("Ligne historique absente — création refusée");
        }
        insert(def, normalized);
      }
      return;
    }
    throw new IllegalStateException("Opération non prise en charge");
  }

  static String insertSql(Definition def) {
    StringBuilder columns = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (Column column : def.columns()) {
      if (!columns.isEmpty()) {
        columns.append(", ");
        values.append(", ");
      }
      columns.append(Definition.bracket(column.name()));
      values.append('?');
    }
    return "INSERT INTO " + def.qualifiedTable() + " (" + columns + ") VALUES (" + values + ")";
  }

  static String updateSql(Definition def) {
    StringBuilder set = new StringBuilder();
    for (Column column : def.columns()) {
      if (column.name().equals(def.keyColumn())) {
        continue;
      }
      if (!set.isEmpty()) {
        set.append(", ");
      }
      set.append(Definition.bracket(column.name())).append(" = ?");
    }
    String key = Definition.bracket(def.keyColumn());
    String where = def.keyColumnDef().kind() == Kind.STRING
        ? "RTRIM(" + key + ") = ?"
        : key + " = ?";
    return "UPDATE " + def.qualifiedTable() + " SET " + set + " WHERE " + where;
  }

  private boolean exists(Definition def, Object keyParam) {
    String key = Definition.bracket(def.keyColumn());
    String where = def.keyColumnDef().kind() == Kind.STRING
        ? "RTRIM(" + key + ") = ?"
        : key + " = ?";
    Integer n = cliniqueJdbc.query(
        "SELECT COUNT(*) FROM " + def.qualifiedTable() + " WHERE " + where,
        rs -> rs.next() ? rs.getInt(1) : 0,
        keyParam);
    return n != null && n > 0;
  }

  private void insert(Definition def, Map<String, Object> fields) {
    List<Object> args = new ArrayList<>();
    for (Column column : def.columns()) {
      args.add(sqlValue(column, fields.get(column.name())));
    }
    cliniqueJdbc.update(insertSql(def), args.toArray());
  }

  private int update(Definition def, Object keyParam, Map<String, Object> fields) {
    List<Object> args = new ArrayList<>();
    for (Column column : def.columns()) {
      if (column.name().equals(def.keyColumn())) {
        continue;
      }
      args.add(sqlValue(column, fields.get(column.name())));
    }
    args.add(keyParam);
    return cliniqueJdbc.update(updateSql(def), args.toArray());
  }

  private static Object keyParam(Definition def, String naturalKey) {
    if (def.keyColumnDef().kind() == Kind.INT) {
      return Integer.valueOf(naturalKey.trim());
    }
    return naturalKey.trim();
  }

  private static Object sqlValue(Column column, Object value) {
    if (value == null) {
      return null;
    }
    return switch (column.kind()) {
      case STRING -> String.valueOf(value);
      case INT -> value instanceof Number n ? n.intValue() : Integer.valueOf(String.valueOf(value));
      case DECIMAL -> value instanceof Number n ? n.doubleValue() : Double.valueOf(String.valueOf(value));
      case BIT -> value instanceof Boolean b ? b : Boolean.valueOf(String.valueOf(value));
      case DATETIME -> Timestamp.valueOf(LocalDateTime.parse(String.valueOf(value).replace(' ', 'T').substring(0, 19)));
    };
  }
}
