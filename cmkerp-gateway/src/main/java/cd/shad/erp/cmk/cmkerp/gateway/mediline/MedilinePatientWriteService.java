package cd.shad.erp.cmk.cmkerp.gateway.mediline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import cd.shad.erp.cmk.cmkerp.gateway.restcontroller.CardexCreationRequests;
import cd.shad.erp.cmk.cmkerp.gateway.restcontroller.CardexCreationRequests.Series;

/**
 * Création idempotente patient Mediline (série IPP) — logique alignée sur {@code MedilinePatientController}.
 */
@Service
public class MedilinePatientWriteService {

  private static final Logger log = LoggerFactory.getLogger(MedilinePatientWriteService.class);
  private static final int ALLOCATION_ATTEMPTS = 8;

  private record PatientField(String column, int max) {}

  private static final Map<String, PatientField> PATIENT_FIELDS = Map.ofEntries(
      Map.entry("nom", new PatientField("NOM", 100)),
      Map.entry("prenom", new PatientField("PRENOM", 150)),
      Map.entry("postnom", new PatientField("POSTNOM", 75)),
      Map.entry("sexe", new PatientField("SEXE", 20)),
      Map.entry("dateNaissance", new PatientField("DATE_NAISSANCE", 10)),
      Map.entry("lieuNaissance", new PatientField("LIEU_NAISSANCE", 50)),
      Map.entry("fonction", new PatientField("FONCTION", 75)),
      Map.entry("email", new PatientField("EMAIL", 255)),
      Map.entry("pec", new PatientField("MAIL", 75)),
      Map.entry("telephone", new PatientField("TELEPHONE", 150)),
      Map.entry("adresse", new PatientField("ADRESSE", 150)),
      Map.entry("categorieCode", new PatientField("FK_CATEGORIE", 5)),
      Map.entry("compteCode", new PatientField("FK_COMPTE_A_FACTURER", 20)));

  private final ObjectProvider<NamedParameterJdbcTemplate> mediline;
  private final ObjectProvider<TransactionTemplate> medilineWriteTransaction;
  private final CardexCreationRequests creationRequests;

  public MedilinePatientWriteService(
      @Qualifier("medilineNamedParameterJdbcTemplate") ObjectProvider<NamedParameterJdbcTemplate> mediline,
      @Qualifier("medilineWriteTransaction") ObjectProvider<TransactionTemplate> medilineWriteTransaction,
      CardexCreationRequests creationRequests) {
    this.mediline = mediline;
    this.medilineWriteTransaction = medilineWriteTransaction;
    this.creationRequests = creationRequests;
  }

  public String createPatient(String actor, UUID requestId, Map<String, String> fields) {
    var jdbc = requireMediline();
    if (requestId == null || fields == null || !hasValue(fields.get("nom"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
    }
    var normalized = new LinkedHashMap<>(fields);
    normalized.putIfAbsent("prenom", "");
    var transaction = medilineWriteTransaction.getIfAvailable();
    if (transaction == null) {
      throw unavailable();
    }
    var params = new MapSqlParameterSource();
    try {
      var columns = bindPatientFields(jdbc, normalized, params);
      String confirmed = creationRequests.reserve(Series.PATIENT, requestId, actor, params);
      if (confirmed != null) {
        return confirmed;
      }
      var createdAt = LocalDateTime.now(ZoneId.of("Africa/Kinshasa"));
      params.addValue("createdAt", createdAt);
      String insert = "INSERT INTO t_personne (CODE, PERSONNE_PHYSIQUE, FK_FORME_JURIDIQUE, DATE_CREAT, ETAT, "
          + String.join(", ", columns) + ") VALUES (:ipp, 1, 'FJ01', :createdAt, 1, "
          + String.join(", ", normalized.keySet().stream().map(key -> ":field_" + key).toList()) + ")";
      for (int attempt = 0; attempt < ALLOCATION_ATTEMPTS; attempt++) {
        String code = nextMedilineCode(jdbc, Series.PATIENT, createdAt.getYear());
        if (code == null) {
          creationRequests.release(Series.PATIENT, requestId);
          throw new ResponseStatusException(HttpStatus.CONFLICT, "La série IPP de l’année a atteint 99999.");
        }
        creationRequests.recordCandidate(Series.PATIENT, requestId, code);
        params.addValue("ipp", code);
        try {
          transaction.execute(status -> {
            Integer occupied =
                jdbc.queryForObject("SELECT COUNT(*) FROM t_personne WHERE CODE = :ipp", params, Integer.class);
            if (occupied != null && occupied > 0) {
              throw new DuplicateKeyException("IPP indisponible.");
            }
            if (jdbc.update(insert, params) < 1) {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Insertion Mediline impossible.");
            }
            return code;
          });
        } catch (DuplicateKeyException duplicate) {
          continue;
        } catch (ResponseStatusException rejected) {
          creationRequests.release(Series.PATIENT, requestId);
          throw rejected;
        }
        creationRequests.confirm(Series.PATIENT, requestId, code);
        log.info("Création dossier Mediline {} par {}", code, actor);
        return code;
      }
      creationRequests.release(Series.PATIENT, requestId);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Attribution IPP concurrente. Réessayez.");
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  /**
   * Met à jour PEC / catégorie / compte Mediline après changement d'affiliation.
   * Ne touche pas aux autres champs d'identité.
   */
  public void updateCoverage(String ipp, String pec, String categorieCode, String compteCode) {
    if (ipp == null || ipp.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IPP obligatoire.");
    }
    var jdbc = requireMediline();
    var transaction = medilineWriteTransaction.getIfAvailable();
    if (transaction == null) {
      throw unavailable();
    }
    Map<String, String> fields = new LinkedHashMap<>();
    if (hasValue(pec)) {
      fields.put("pec", pec);
    }
    if (hasValue(categorieCode)) {
      fields.put("categorieCode", categorieCode);
    }
    if (hasValue(compteCode)) {
      fields.put("compteCode", compteCode);
    }
    if (fields.isEmpty()) {
      return;
    }
    var params = new MapSqlParameterSource("ipp", ipp.trim());
    bindPatientFields(jdbc, fields, params);
    StringBuilder set = new StringBuilder();
    int idx = 0;
    for (String key : fields.keySet()) {
      var field = PATIENT_FIELDS.get(key);
      if (idx++ > 0) {
        set.append(", ");
      }
      set.append(field.column()).append(" = :field_").append(key);
    }
    String sql = "UPDATE t_personne SET " + set + " WHERE CODE = :ipp AND PERSONNE_PHYSIQUE = 1";
    Integer updated = transaction.execute(status -> jdbc.update(sql, params));
    if (updated == null || updated < 1) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient Mediline introuvable.");
    }
    log.info("Couverture Mediline actualisée pour {}", ipp);
  }

  private NamedParameterJdbcTemplate requireMediline() {
    var jdbc = mediline.getIfAvailable();
    if (jdbc == null) {
      throw unavailable();
    }
    return jdbc;
  }

  private static ResponseStatusException unavailable() {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mediline indisponible.");
  }

  private static String nextMedilineCode(NamedParameterJdbcTemplate jdbc, Series series, int year) {
    String prefix = String.format(Locale.ROOT, "%02d%s", year % 100, series.marker);
    String maxSuffix = "9".repeat(series.digits);
    var params = new MapSqlParameterSource("first", prefix + "0".repeat(series.digits))
        .addValue("last", prefix + maxSuffix)
        .addValue("pattern", "^" + prefix + "[0-9]{" + series.digits + "}$");
    var codes = jdbc.queryForList(
        """
        SELECT CODE FROM %s
        WHERE CODE BETWEEN :first AND :last AND CODE REGEXP :pattern
        ORDER BY CODE DESC LIMIT 1
        """.formatted(series.table),
        params,
        String.class);
    int last = codes.isEmpty() ? 0 : Integer.parseInt(codes.getFirst().substring(prefix.length()));
    return last == Integer.parseInt(maxSuffix)
        ? null
        : prefix + String.format(Locale.ROOT, "%0" + series.digits + "d", last + 1);
  }

  private static List<String> bindPatientFields(
      NamedParameterJdbcTemplate jdbc, Map<String, String> fields, MapSqlParameterSource params) {
    var columns = new ArrayList<String>();
    for (var entry : fields.entrySet()) {
      String key = entry.getKey();
      var field = PATIENT_FIELDS.get(key);
      if (field == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ patient non pris en charge.");
      }
      String value = hasValue(entry.getValue()) ? checked(entry.getValue(), field.max()) : null;
      Object stored = value;
      switch (key) {
        case "nom" -> {
          if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
          }
        }
        case "prenom" -> {
          if (value == null) {
            stored = "";
          }
        }
        case "email" -> {
          if (value != null && !value.matches("[^\\s@]+@[^\\s@]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail invalide.");
          }
        }
        case "sexe" -> {
          if (value != null) {
            stored = switch (value) {
              case "M" -> "MASCULIN";
              case "F" -> "FEMININ";
              default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sexe invalide.");
            };
          }
        }
        case "dateNaissance" -> {
          if (value != null) {
            try {
              var date = LocalDate.parse(value);
              if (date.isAfter(LocalDate.now(ZoneId.of("Africa/Kinshasa")))) {
                throw new IllegalArgumentException();
              }
              stored = date;
            } catch (java.time.DateTimeException | IllegalArgumentException invalid) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date de naissance invalide.");
            }
          }
        }
        case "categorieCode" -> validateReference(jdbc, "t_categorie_patient", value);
        case "compteCode" -> validateReference(jdbc, "t_compte", value);
        default -> { }
      }
      columns.add(field.column());
      params.addValue("field_" + key, stored);
    }
    return columns;
  }

  private static void validateReference(NamedParameterJdbcTemplate jdbc, String table, String code) {
    if (code == null) {
      return;
    }
    if ("t_categorie_patient".equals(table) && "-1".equals(code)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Code catégorie Mediline invalide (« -1 »). Configurez la catégorie bénéficiaire du contrat.");
    }
    var ids = jdbc.queryForList(
        "SELECT CODE FROM " + table + " WHERE CODE = :code",
        new MapSqlParameterSource("code", code),
        String.class);
    if (ids.isEmpty()) {
      if ("t_categorie_patient".equals(table)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La catégorie Mediline « " + code + " » est introuvable. Vérifiez la configuration du contrat.");
      }
      if ("t_compte".equals(table)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Le compte Mediline « " + code + " » est introuvable. Vérifiez l’association société → compte.");
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Référentiel Mediline introuvable : " + code);
    }
  }

  private static boolean hasValue(String value) {
    return value != null && !value.isBlank();
  }

  private static String checked(String value, int max) {
    String v = value == null ? "" : value.trim();
    if (v.length() > max) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur trop longue.");
    }
    return v.isEmpty() ? null : v;
  }
}