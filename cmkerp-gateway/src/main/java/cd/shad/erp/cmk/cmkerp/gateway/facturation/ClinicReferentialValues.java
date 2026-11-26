package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Column;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Kind;

/**
 * Normalisation des champs métier. N'accepte que les colonnes du catalogue.
 */
public final class ClinicReferentialValues {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private ClinicReferentialValues() {
  }

  public static Map<String, Object> normalize(
      Definition def, Map<String, Object> raw, boolean rejectUnknown, boolean enforceRequired) {
    Map<String, Object> source = raw == null ? Map.of() : raw;
    if (rejectUnknown) {
      for (String name : source.keySet()) {
        if (name == null) {
          continue;
        }
        String upper = name.trim().toUpperCase(Locale.ROOT);
        if ("MOTDEPASSE".equals(upper) || "ACTIF".equals(upper) || def.column(upper).isEmpty()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ non pris en charge.");
        }
      }
    }
    Map<String, Object> out = new LinkedHashMap<>();
    for (Column column : def.columns()) {
      Object value = first(source, column.name());
      out.put(column.name(), coerce(column, value));
    }
    if (enforceRequired) {
      for (Column column : def.columns()) {
        if (column.required() && out.get(column.name()) == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Information obligatoire manquante.");
        }
      }
    }
    return out;
  }

  public static String naturalKey(Definition def, Map<String, Object> normalized) {
    Object value = normalized.get(def.keyColumn());
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant obligatoire.");
    }
    String key = String.valueOf(value).trim();
    if (key.isEmpty() || key.length() > 64) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant invalide.");
    }
    return key;
  }

  public static String label(Definition def, Map<String, Object> normalized) {
    Object value = normalized.get(def.labelColumn());
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return null;
    }
    return text.length() <= 255 ? text : text.substring(0, 255);
  }

  public static String fingerprint(Definition def, Map<String, Object> normalized) {
    StringBuilder canonical = new StringBuilder();
    def.columns().stream()
        .sorted((a, b) -> a.name().compareTo(b.name()))
        .forEach(column -> canonical
            .append(column.name())
            .append('=')
            .append(canonicalValue(normalized.get(column.name())))
            .append('\n'));
    return sha256(canonical.toString());
  }

  public static Object coerce(Column column, Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof String text && text.isBlank()) {
      return null;
    }
    try {
      return switch (column.kind()) {
        case STRING -> stringValue(column, raw);
        case INT -> intValue(raw);
        case DECIMAL -> decimalValue(raw);
        case BIT -> bitValue(raw);
        case DATETIME -> dateValue(raw);
      };
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur invalide.");
    }
  }

  private static Object first(Map<String, Object> source, String name) {
    if (source.containsKey(name)) {
      return source.get(name);
    }
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(name)) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static String stringValue(Column column, Object raw) {
    String text = String.valueOf(raw).trim();
    if (text.isEmpty()) {
      return null;
    }
    if (column.maxLength() > 0 && text.length() > column.maxLength()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur trop longue.");
    }
    return text;
  }

  private static Integer intValue(Object raw) {
    if (raw instanceof Boolean) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur invalide.");
    }
    if (raw instanceof Number number) {
      double d = number.doubleValue();
      if (d != Math.rint(d) || d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur invalide.");
      }
      return number.intValue();
    }
    String text = String.valueOf(raw).trim();
    if (text.isEmpty()) {
      return null;
    }
    return Integer.valueOf(text);
  }

  private static Double decimalValue(Object raw) {
    if (raw instanceof Boolean) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur invalide.");
    }
    if (raw instanceof Number number) {
      return number.doubleValue();
    }
    String text = String.valueOf(raw).trim();
    if (text.isEmpty()) {
      return null;
    }
    return Double.valueOf(text);
  }

  private static Boolean bitValue(Object raw) {
    if (raw instanceof Boolean bool) {
      return bool;
    }
    if (raw instanceof Number number) {
      return number.intValue() != 0;
    }
    String text = String.valueOf(raw).trim();
    if (text.isEmpty()) {
      return null;
    }
    if ("1".equals(text) || "true".equalsIgnoreCase(text) || "oui".equalsIgnoreCase(text)) {
      return true;
    }
    if ("0".equals(text) || "false".equalsIgnoreCase(text) || "non".equalsIgnoreCase(text)) {
      return false;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur invalide.");
  }

  private static String dateValue(Object raw) {
    if (raw instanceof java.sql.Timestamp timestamp) {
      return timestamp.toLocalDateTime().withNano(0).format(ISO);
    }
    if (raw instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate().atStartOfDay().format(ISO);
    }
    if (raw instanceof LocalDateTime dateTime) {
      return dateTime.withNano(0).format(ISO);
    }
    if (raw instanceof LocalDate localDate) {
      return localDate.atStartOfDay().format(ISO);
    }
    String text = String.valueOf(raw).trim();
    if (text.isEmpty()) {
      return null;
    }
    String normalized = text.replace(' ', 'T');
    // Date seule (DATENAIS) — accepter yyyy-MM-dd sans heure.
    if (normalized.length() == 10) {
      try {
        return LocalDate.parse(normalized).atStartOfDay().format(ISO);
      } catch (DateTimeParseException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date invalide.");
      }
    }
    if (normalized.length() >= 19) {
      normalized = normalized.substring(0, 19);
    }
    try {
      return LocalDateTime.parse(normalized).format(ISO);
    } catch (DateTimeParseException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date invalide.");
    }
  }

  private static String canonicalValue(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Double number) {
      return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }
    if (value instanceof Boolean bool) {
      return bool ? "1" : "0";
    }
    return String.valueOf(value).trim();
  }

  private static String sha256(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Empreinte impossible", ex);
    }
  }
}
