package cd.shad.erp.cmk.cmkerp.gateway.restcontroller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import cd.shad.erp.cmk.cmkerp.gateway.restcontroller.CardexCreationRequests.Series;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionAuthorizationPort;

@RestController
@RequestMapping("/api/v1/cardex")
public class MedilinePatientController {
  private static final int PATIENT_LIMIT = 100;
  private static final int COMPANY_LIMIT = 50;
  private static final int ALLOCATION_ATTEMPTS = 8;
  private static final Set<Integer> FILE_PAGE_SIZES = Set.of(10, 25, 50, 100);
  private static final Logger log = LoggerFactory.getLogger(MedilinePatientController.class);
  private static final Set<String> FILE_TYPES = Set.of("AMBULATOIRE", "URGENCE", "HOSPITALISATION",
      "DIAGNOSTICS", "SOINS A DOMICILE", "THERAPIE AMBULATOIRE");
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
  private static final String PATIENT_SELECT = """
      SELECT p.CODE, p.NOM, p.POSTNOM, p.PRENOM, p.SEXE, p.DATE_NAISSANCE,
             p.LIEU_NAISSANCE, p.TELEPHONE, p.EMAIL, p.MAIL AS pec, p.ADRESSE, p.FONCTION,
             p.ANCIEN_DOSSIER, CAST(p.DATE_CREAT AS DATE) AS date_creation,
             p.FK_COMPTE_A_FACTURER AS compte_code, c.INTITULE AS compte_intitule,
             p.FK_CATEGORIE AS categorie_code,
             COALESCE(NULLIF(TRIM(cat.INTITULE), ''), NULLIF(TRIM(p.FK_CATEGORIE), '-1')) AS categorie
      FROM t_personne p LEFT JOIN t_compte c ON c.CODE = p.FK_COMPTE_A_FACTURER
      LEFT JOIN t_categorie_patient cat ON cat.CODE = p.FK_CATEGORIE
      """;
  /** Sous-requêtes : évite les doublons si t_categorie_patient / t_compte ont des CODE non uniques. */
  private static final String FILE_SELECT = """
      SELECT f.CODE, NULLIF(DATE_FORMAT(f.DATE_CREAT, '%Y-%m-%d'), '0000-00-00') AS date_creation,
             NULLIF(TRIM(f.typehospi), '') AS typehospi, f.statut, f.ETAT, f.URGENCE, f.FK_HOSPITALISATION,
             f.FK_CATEGORIE_PATIENT AS categorie_code,
             (SELECT cat.INTITULE FROM t_categorie_patient cat
              WHERE cat.CODE = f.FK_CATEGORIE_PATIENT LIMIT 1) AS categorie,
             f.FK_COMPTE_A_FACTURER AS compte_code,
             (SELECT c.INTITULE FROM t_compte c
              WHERE c.CODE = f.FK_COMPTE_A_FACTURER LIMIT 1) AS compte
      FROM t_fiche_medicale f
      """;
  private static final String EMERGENCY_FICHE = """
      (f.typehospi = :type OR (f.URGENCE = 1 AND (f.typehospi IS NULL OR f.typehospi = 'INDETERMINE')))
      AND COALESCE(f.FK_HOSPITALISATION, 0) <= 0
      """;
  private static final String INPATIENT_FICHE = "(f.typehospi = :type OR f.FK_HOSPITALISATION > 0)";
  private final ObjectProvider<NamedParameterJdbcTemplate> mediline;
  private final ObjectProvider<TransactionTemplate> medilineReadTransaction;
  private final ObjectProvider<TransactionTemplate> medilineWriteTransaction;
  private final UtilisateurRepository users;
  private final CardexCreationRequests creationRequests;
  private final PermissionAuthorizationPort permissionAuthorization;

  public MedilinePatientController(
      @Qualifier("medilineNamedParameterJdbcTemplate") ObjectProvider<NamedParameterJdbcTemplate> mediline,
      @Qualifier("medilineReadTransaction") ObjectProvider<TransactionTemplate> medilineReadTransaction,
      @Qualifier("medilineWriteTransaction") ObjectProvider<TransactionTemplate> medilineWriteTransaction,
      UtilisateurRepository users,
      CardexCreationRequests creationRequests,
      PermissionAuthorizationPort permissionAuthorization) {
    this.mediline = mediline;
    this.medilineReadTransaction = medilineReadTransaction;
    this.medilineWriteTransaction = medilineWriteTransaction;
    this.users = users;
    this.creationRequests = creationRequests;
    this.permissionAuthorization = permissionAuthorization;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> cardexError(ResponseStatusException error) {
    String message = error.getReason() == null ? "Accès au Cardex refusé." : error.getReason();
    return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", message));
  }

  @GetMapping("/societes")
  public Results<Company> companies(Principal principal, @RequestParam(name = "q", defaultValue = "") String q,
      @RequestParam(name = "page", defaultValue = "0") int page) {
    var jdbc = searchDatabase(principal);
    if (page < 0 || page > 1000 || q.length() > 150) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtre ou page invalide.");
    }
    var params = new MapSqlParameterSource().addValue("limit", COMPANY_LIMIT + 1)
        .addValue("offset", page * COMPANY_LIMIT);
    String sql = "SELECT CODE, INTITULE FROM t_compte WHERE CODE <> ''";
    if (!q.isBlank()) {
      params.addValue("query", contains(q)).addValue("code", q.trim());
      sql += " AND (CODE = :code OR INTITULE LIKE :query ESCAPE '!')";
    }
    try {
      var rows = jdbc.query(sql + " ORDER BY INTITULE, CODE LIMIT :limit OFFSET :offset", params,
          (rs, row) -> new Company(rs.getString("CODE"), rs.getString("INTITULE")));
      return limited(rows, COMPANY_LIMIT);
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  @PostMapping("/patients/recherche")
  public Results<Patient> patients(Principal principal, @RequestBody Criteria criteria) {
    var jdbc = searchDatabase(principal);
    var params = new MapSqlParameterSource();
    var where = new StringBuilder(" WHERE p.PERSONNE_PHYSIQUE = 1");
    applyIdentite(where, params, criteria.identite());
    addContains(where, params, "p.NOM", "nom", criteria.nom(), 100);
    addContains(where, params, "p.POSTNOM", "postnom", criteria.postnom(), 75);
    addContains(where, params, "p.PRENOM", "prenom", criteria.prenom(), 150);
    addContains(where, params, "p.LIEU_NAISSANCE", "lieu", criteria.lieuNaissance(), 50);
    if (criteria.dateNaissance() != null) {
      where.append(" AND p.DATE_NAISSANCE = :birth");
      params.addValue("birth", criteria.dateNaissance());
    }
    if (hasValue(criteria.nIpp())) {
      where.append(" AND p.CODE = :ipp");
      params.addValue("ipp", checked(criteria.nIpp(), 25));
    }
    if (hasValue(criteria.nFiche())) {
      where.append("""
           AND EXISTS (SELECT 1 FROM t_fiche_medicale f
                       WHERE f.CODE = :fiche AND f.FK_PERSONNE_PATIENT = p.CODE)
          """);
      params.addValue("fiche", checked(criteria.nFiche(), 50));
    }
    if (params.getValues().isEmpty() && !hasValue(criteria.compteCode())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseignez au moins un critère.");
    }

    try {
      if (hasValue(criteria.compteCode())) {
        where.append(" AND p.FK_COMPTE_A_FACTURER = :account");
        params.addValue("account", checked(criteria.compteCode(), 20));
      }
      params.addValue("limit", PATIENT_LIMIT + 1);
      var rows = jdbc.query(PATIENT_SELECT + where + " ORDER BY p.NOM, p.POSTNOM, p.PRENOM, p.CODE LIMIT :limit",
          params, (rs, row) -> readPatient(rs));
      return limited(rows, PATIENT_LIMIT);
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  @GetMapping("/patients/recents")
  public Results<RecentPatient> recentPatients(Principal principal,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    var jdbc = database(principal);
    if (limit != 50 && limit != 100 && limit != 200) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez 50, 100 ou 200 dossiers.");
    }
    try {
      var rows = jdbc.query("""
          SELECT p.CODE, p.NOM, p.POSTNOM, p.PRENOM, p.SEXE, p.DATE_NAISSANCE,
                 p.ANCIEN_DOSSIER, p.MAIL AS pec, CAST(p.DATE_CREAT AS DATE) AS date_creation
          FROM t_personne p
          WHERE p.PERSONNE_PHYSIQUE = 1 AND p.DATE_CREAT >= '1000-01-01'
          ORDER BY p.DATE_CREAT DESC, p.CODE DESC
          LIMIT :limit
          """, new MapSqlParameterSource("limit", limit + 1), (rs, row) -> new RecentPatient(
              rs.getString("CODE"), rs.getString("NOM"), rs.getString("POSTNOM"), rs.getString("PRENOM"),
              rs.getString("SEXE"), rs.getObject("DATE_NAISSANCE", LocalDate.class),
              rs.getString("ANCIEN_DOSSIER"), rs.getString("pec"), rs.getObject("date_creation", LocalDate.class)));
      return limited(rows, limit);
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  @GetMapping("/patients/{ipp}")
  public Patient patient(Principal principal, @PathVariable("ipp") String ipp) {
    var jdbc = database(principal);
    try {
      return findPatient(jdbc, checked(ipp, 25));
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  @PostMapping("/patients")
  public Patient createPatient(Principal principal, @RequestBody PatientCreate request) {
    var jdbc = database(principal);
    if (request.idRequete() == null || request.fields() == null
        || !hasValue(request.fields().get("nom"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
    }
    var fields = new LinkedHashMap<>(request.fields());
    fields.putIfAbsent("prenom", "");
    var transaction = medilineWriteTransaction.getIfAvailable();
    if (transaction == null) throw unavailable();
    var params = new MapSqlParameterSource();
    try {
      var columns = bindPatientFields(jdbc, fields, params);
      var id = request.idRequete();
      String confirmed = creationRequests.reserve(Series.PATIENT, id, principal.getName(), params);
      if (confirmed != null) return findPatient(jdbc, confirmed);

      var createdAt = LocalDateTime.now(ZoneId.of("Africa/Kinshasa"));
      params.addValue("createdAt", createdAt);
      String insert = "INSERT INTO t_personne (CODE, PERSONNE_PHYSIQUE, FK_FORME_JURIDIQUE, DATE_CREAT, ETAT, "
          + String.join(", ", columns) + ") VALUES (:ipp, 1, 'FJ01', :createdAt, 1, "
          + String.join(", ", fields.keySet().stream().map(key -> ":field_" + key).toList()) + ")";
      for (int attempt = 0; attempt < ALLOCATION_ATTEMPTS; attempt++) {
        String code = nextMedilineCode(jdbc, Series.PATIENT, createdAt.getYear());
        if (code == null) {
          creationRequests.release(Series.PATIENT, id);
          throw new ResponseStatusException(HttpStatus.CONFLICT, "La série IPP de l’année a atteint 99999.");
        }
        creationRequests.recordCandidate(Series.PATIENT, id, code);
        params.addValue("ipp", code);
        Patient created;
        try {
          created = transaction.execute(status -> {
            Integer occupied = jdbc.queryForObject("SELECT COUNT(*) FROM t_personne WHERE CODE = :ipp", params, Integer.class);
            if (occupied != null && occupied > 0) {
              throw new DuplicateKeyException("IPP indisponible.");
            }
            jdbc.update(insert, params);
            try {
              return findPatient(jdbc, code);
            } catch (ResponseStatusException missing) {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Mediline n’a pas repris l’IPP attribué.");
            }
          });
        } catch (DuplicateKeyException duplicate) {
          // La clé primaire Mediline arbitre aussi les créations provenant d'autres clients.
          continue;
        } catch (ResponseStatusException rejected) {
          creationRequests.release(Series.PATIENT, id);
          throw rejected;
        }
        creationRequests.confirm(Series.PATIENT, id, code);
        log.info("Création du dossier Cardex {} par l’utilisateur ERP {}", code, principal.getName());
        return created;
      }
      creationRequests.release(Series.PATIENT, id);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Attribution de l’IPP concurrente. Réessayez.");
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  private static String nextMedilineCode(NamedParameterJdbcTemplate jdbc, Series series, int year) {
    String prefix = String.format(Locale.ROOT, "%02d%s", year % 100, series.marker);
    String maxSuffix = "9".repeat(series.digits);
    var params = new MapSqlParameterSource("first", prefix + "0".repeat(series.digits))
        .addValue("last", prefix + maxSuffix)
        .addValue("pattern", "^" + prefix + "[0-9]{" + series.digits + "}$");
    var codes = jdbc.queryForList("""
        SELECT CODE FROM %s
        WHERE CODE BETWEEN :first AND :last AND CODE REGEXP :pattern
        ORDER BY CODE DESC LIMIT 1
        """.formatted(series.table), params, String.class);
    int last = codes.isEmpty() ? 0 : Integer.parseInt(codes.getFirst().substring(prefix.length()));
    return last == Integer.parseInt(maxSuffix) ? null
        : prefix + String.format(Locale.ROOT, "%0" + series.digits + "d", last + 1);
  }

  @PatchMapping("/patients/{ipp}")
  public Patient updatePatient(Principal principal, @PathVariable("ipp") String ipp,
      @RequestBody Map<String, String> fields) {
    var jdbc = database(principal);
    String code = checked(ipp, 25);
    var params = new MapSqlParameterSource("ipp", code);
    try {
      var columns = bindPatientFields(jdbc, fields, params);
      if (!columns.isEmpty()) {
        var setters = new ArrayList<String>();
        int index = 0;
        for (String key : fields.keySet()) setters.add(columns.get(index++) + " = :field_" + key);
        if (jdbc.update("UPDATE t_personne SET " + String.join(", ", setters)
            + " WHERE CODE = :ipp AND PERSONNE_PHYSIQUE = 1", params) > 0) {
          log.info("Modification du dossier Cardex {} par l’utilisateur ERP {}", code, principal.getName());
        }
      }
      return findPatient(jdbc, code);
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  private static Patient findPatient(NamedParameterJdbcTemplate jdbc, String ipp) {
    return jdbc.query(PATIENT_SELECT + " WHERE p.CODE = :ipp AND p.PERSONNE_PHYSIQUE = 1 LIMIT 1",
        new MapSqlParameterSource("ipp", ipp), (rs, row) -> readPatient(rs))
        .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));
  }

  private static List<String> bindPatientFields(NamedParameterJdbcTemplate jdbc, Map<String, String> fields,
      MapSqlParameterSource params) {
    var columns = new ArrayList<String>();
    for (var entry : fields.entrySet()) {
      String key = entry.getKey();
      var field = PATIENT_FIELDS.get(key);
      if (field == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ patient non pris en charge.");
      String value = hasValue(entry.getValue()) ? checked(entry.getValue(), field.max()) : null;
      Object stored = value;
      switch (key) {
        case "nom" -> {
          if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        case "prenom" -> { if (value == null) stored = ""; }
        case "email" -> {
          if (value != null && !value.matches("[^\\s@]+@[^\\s@]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail invalide.");
          }
        }
        case "sexe" -> {
          if (value != null) stored = switch (value) {
            case "M" -> "MASCULIN";
            case "F" -> "FEMININ";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sexe invalide.");
          };
        }
        case "dateNaissance" -> {
          if (value != null) {
            try {
              var date = LocalDate.parse(value);
              if (date.isAfter(LocalDate.now(ZoneId.of("Africa/Kinshasa")))) throw new IllegalArgumentException();
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
    if (code == null) return;
    var ids = jdbc.queryForList("SELECT CODE FROM " + table + " WHERE CODE = :code",
        new MapSqlParameterSource("code", code), String.class);
    if (ids.isEmpty() || "-1".equals(code) && "t_categorie_patient".equals(table)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie ou compte inconnu.");
    }
  }

  private static Patient readPatient(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Patient(rs.getString("CODE"), rs.getString("NOM"), rs.getString("POSTNOM"),
        rs.getString("PRENOM"), rs.getString("SEXE"),
        rs.getObject("DATE_NAISSANCE", LocalDate.class), rs.getString("LIEU_NAISSANCE"),
        rs.getString("TELEPHONE"), rs.getString("ADRESSE"), rs.getString("FONCTION"),
        rs.getString("ANCIEN_DOSSIER"), rs.getString("compte_code"), rs.getString("compte_intitule"),
        rs.getString("pec"), rs.getString("EMAIL"), rs.getString("categorie"),
        rs.getString("categorie_code"), rs.getObject("date_creation", LocalDate.class));
  }

  @GetMapping("/categories-patient")
  public List<Company> categories(Principal principal) {
    var jdbc = database(principal);
    try {
      return jdbc.query("""
          SELECT CODE, INTITULE FROM t_categorie_patient
          WHERE CODE <> '' AND CODE <> '-1' AND CHAR_LENGTH(CODE) <= 5
          ORDER BY INTITULE, CODE LIMIT 200
          """, new MapSqlParameterSource(), (rs, row) -> new Company(rs.getString(1), rs.getString(2)));
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  @GetMapping("/prises-en-charge")
  public CareQueuePage careQueue(Principal principal,
      @RequestParam("type") String type,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "pageSize", defaultValue = "25") int pageSize) {
    var jdbc = database(principal);
    String careType = normalizeCareType(type);
    if (careType == null || !FILE_TYPES.contains(careType) || page < 0 || !FILE_PAGE_SIZES.contains(pageSize)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètres de la file de soins invalides.");
    }
    String source = """
        FROM t_fiche_medicale f
        JOIN t_personne p ON p.CODE = f.FK_PERSONNE_PATIENT
        WHERE f.statut = 'EN COURS'
          AND (f.ETAT = 1 OR f.ETAT IS NULL)
          AND f.CODE <> ''
          AND """ + " " + switch (careType) {
      case "HOSPITALISATION" -> INPATIENT_FICHE;
      case "URGENCE" -> EMERGENCY_FICHE;
      default -> "f.typehospi = :type AND COALESCE(f.FK_HOSPITALISATION, 0) <= 0";
    };
    var params = new MapSqlParameterSource().addValue("type", careType).addValue("limit", pageSize);
    var transaction = medilineReadTransaction.getIfAvailable();
    if (transaction == null) throw unavailable();
    try {
      return transaction.execute(tx -> {
        long total = jdbc.queryForObject("SELECT COUNT(*) " + source, params, Long.class);
        long totalPages = (total + pageSize - 1) / pageSize;
        int currentPage = (int) Math.min(page, Math.max(0, totalPages - 1));
        params.addValue("offset", (long) currentPage * pageSize);
        var items = total == 0 ? List.<CareQueueRow>of() : jdbc.query("""
            SELECT f.CODE, NULLIF(DATE_FORMAT(f.DATE_CREAT, '%Y-%m-%dT%H:%i:%s'), '0000-00-00T00:00:00') AS opened_at,
                   p.CODE AS ipp,
                   CONCAT_WS(' ', NULLIF(TRIM(p.NOM), ''), NULLIF(TRIM(p.POSTNOM), ''),
                                  NULLIF(TRIM(p.PRENOM), '')) AS patient
            """ + source + " ORDER BY f.DATE_CREAT DESC, f.CODE DESC LIMIT :limit OFFSET :offset",
            params, (rs, row) -> new CareQueueRow(rs.getString("ipp"), rs.getString("patient"),
                rs.getString("CODE"), rs.getString("opened_at")));
        return new CareQueuePage(items, currentPage + 1 < totalPages, currentPage, pageSize, totalPages, total);
      });
    } catch (DataAccessException | TransactionException exception) {
      throw unavailable();
    }
  }

  @GetMapping("/patients/{ipp}/fiches")
  public FilePage files(Principal principal, @PathVariable("ipp") String ipp,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "pageSize", defaultValue = "25") int pageSize,
      @RequestParam(name = "hospitalisations", defaultValue = "false") boolean hospitalisations,
      @RequestParam(name = "period", defaultValue = "today") String period,
      @RequestParam(name = "type", required = false) String type,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "dateFrom", required = false) LocalDate dateFrom,
      @RequestParam(name = "dateTo", required = false) LocalDate dateTo,
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "fiche", required = false) String fiche) {
    var jdbc = database(principal);
    if (page < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page invalide.");
    }
    if (!FILE_PAGE_SIZES.contains(pageSize)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de fiches par page invalide.");
    }
    String periodKey = period == null || period.isBlank() ? "today" : period.trim().toLowerCase(Locale.ROOT);
    if (!Set.of("today", "history", "all", "range").contains(periodKey)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période invalide.");
    }
    LocalDate today = LocalDate.now(ZoneId.of("Africa/Kinshasa"));
    var params = new MapSqlParameterSource().addValue("ipp", checked(ipp, 25))
        .addValue("limit", pageSize);
    StringBuilder filter = new StringBuilder();
    if (hospitalisations) {
      filter.append(" AND (UPPER(TRIM(IFNULL(f.typehospi,''))) = 'HOSPITALISATION' OR f.FK_HOSPITALISATION > 0)");
    }
    switch (periodKey) {
      case "today" -> {
        params.addValue("day", today);
        filter.append(" AND DATE(f.DATE_CREAT) = :day");
      }
      case "history" -> {
        params.addValue("day", today);
        filter.append(" AND DATE(f.DATE_CREAT) < :day");
      }
      case "range" -> {
        if (dateFrom == null && dateTo == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez une plage de dates.");
        }
        if (dateFrom != null) {
          params.addValue("dateFrom", dateFrom);
          filter.append(" AND DATE(f.DATE_CREAT) >= :dateFrom");
        }
        if (dateTo != null) {
          params.addValue("dateTo", dateTo);
          filter.append(" AND DATE(f.DATE_CREAT) <= :dateTo");
        }
      }
      default -> {
        /* all */
      }
    }
    if (hasValue(type)) {
      String normalized = normalizeCareType(type);
      if (normalized == null || !FILE_TYPES.contains(normalized)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de prise en charge invalide.");
      }
      params.addValue("type", normalized);
      if ("URGENCE".equals(normalized)) {
        filter.append(" AND ").append(EMERGENCY_FICHE);
      } else if ("HOSPITALISATION".equals(normalized)) {
        filter.append(" AND ").append(INPATIENT_FICHE);
      } else {
        filter.append(" AND UPPER(TRIM(IFNULL(f.typehospi,''))) = :type");
      }
    }
    if (hasValue(status)) {
      String normalizedStatus = normalizeFileStatus(status);
      if (normalizedStatus == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de fiche invalide.");
      }
      params.addValue("status", normalizedStatus);
      if ("INDETERMINE".equals(normalizedStatus)) {
        filter.append(" AND (f.statut IS NULL OR TRIM(f.statut) = '' OR UPPER(TRIM(f.statut)) NOT IN ('EN COURS','TERMINE'))");
      } else {
        filter.append(" AND UPPER(TRIM(f.statut)) = :status");
      }
    }
    if (hasValue(fiche)) {
      params.addValue("fiche", checked(fiche, 40));
      filter.append(" AND f.CODE = :fiche");
    } else if (hasValue(q)) {
      params.addValue("q", contains(checked(q, 40).toUpperCase(Locale.ROOT)));
      filter.append(" AND UPPER(f.CODE) LIKE :q ESCAPE '!'");
    }
    var transaction = medilineReadTransaction.getIfAvailable();
    if (transaction == null) throw unavailable();
    try {
      return transaction.execute(tx -> {
        String where = " WHERE f.FK_PERSONNE_PATIENT = :ipp" + filter;
        Long totalObj = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT f.CODE) FROM t_fiche_medicale f" + where, params, Long.class);
        long total = totalObj == null ? 0L : totalObj;
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        int currentPage = (int) Math.min(page, Math.max(0, totalPages - 1));
        params.addValue("offset", (long) currentPage * pageSize);
        var rows = total == 0 ? List.<MedicalFile>of() : jdbc.query(FILE_SELECT + where
                + " ORDER BY f.DATE_CREAT DESC, f.CODE DESC LIMIT :limit OFFSET :offset",
            params, (rs, row) -> readFile(rs));
        // Ceinture : dédoublonnage par CODE si une jointure résiduelle réapparaît
        rows = dedupeFiles(rows);
        return new FilePage(rows, currentPage + 1 < totalPages, currentPage, pageSize, totalPages, total);
      });
    } catch (DataAccessException | TransactionException exception) {
      throw unavailable();
    }
  }

  @PostMapping("/patients/{ipp}/fiches")
  public ResponseEntity<?> createFile(Principal principal, @PathVariable("ipp") String ipp,
      @RequestBody CreateFile request,
      @RequestParam(name = "force", defaultValue = "false") boolean force) {
    var jdbc = database(principal);
    String careType = normalizeCareType(request.type());
    if (request.idRequete() == null || careType == null || !FILE_TYPES.contains(careType)
        || !hasValue(request.categorieCode()) || !hasValue(request.compteCode())
        || "-1".equals(request.categorieCode().trim())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseignez le type, la catégorie et le compte.");
    }
    var transaction = medilineWriteTransaction.getIfAvailable();
    if (transaction == null) throw unavailable();
    var params = new MapSqlParameterSource("ipp", checked(ipp, 25))
        .addValue("category", checked(request.categorieCode(), 5))
        .addValue("account", checked(request.compteCode(), 20))
        .addValue("type", careType);
    UUID id = request.idRequete();
    try {
      String confirmed = creationRequests.reserve(Series.FICHE, id, principal.getName(), params);
      if (confirmed != null) {
        return ResponseEntity.ok(findMedicalFile(jdbc, params.addValue("code", confirmed)));
      }

      var now = LocalDateTime.now(ZoneId.of("Africa/Kinshasa"));
      params.addValue("createdAt", now).addValue("day", now.toLocalDate())
          .addValue("urgent", "URGENCE".equals(careType) ? 1 : 0);
      for (int attempt = 0; attempt < ALLOCATION_ATTEMPTS; attempt++) {
        String code = nextMedilineCode(jdbc, Series.FICHE, now.getYear());
        if (code == null) {
          creationRequests.release(Series.FICHE, id);
          throw new ResponseStatusException(HttpStatus.CONFLICT, "La série de fiches de l’année a atteint 9999999.");
        }
        creationRequests.recordCandidate(Series.FICHE, id, code);
        params.addValue("code", code);
        MedicalFile created;
        try {
          created = transaction.execute(status -> insertMedicalFile(jdbc, params, force));
        } catch (DuplicateKeyException duplicate) {
          continue;
        } catch (OpenMedicalFile duplicate) {
          creationRequests.release(Series.FICHE, id);
          return ResponseEntity.status(HttpStatus.CONFLICT).body(new DuplicateFileResponse(
              "Une fiche « " + careType + " » du jour est déjà ouverte pour ce patient.", duplicate.file));
        } catch (ResponseStatusException rejected) {
          creationRequests.release(Series.FICHE, id);
          throw rejected;
        }
        creationRequests.confirm(Series.FICHE, id, code);
        log.info("Création de la fiche Mediline {} par l’utilisateur ERP {}", code, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
      }
      creationRequests.release(Series.FICHE, id);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Attribution du numéro de fiche concurrente. Réessayez.");
    } catch (DataAccessException | TransactionException exception) {
      String sqlState = exception instanceof DataAccessException dataAccess
          && dataAccess.getMostSpecificCause() instanceof java.sql.SQLException cause
          ? cause.getSQLState() : "inconnu";
      log.warn("Échec de l’écriture ou de la relecture Mediline (SQLState={})", sqlState);
      throw unavailable();
    }
  }

  private static MedicalFile insertMedicalFile(NamedParameterJdbcTemplate jdbc, MapSqlParameterSource params,
      boolean force) {
    // Sérialise le contrôle des fiches ouvertes pour un même patient dans Cardex.
    var patients = jdbc.queryForList("""
        SELECT CODE FROM t_personne WHERE CODE = :ipp AND PERSONNE_PHYSIQUE = 1 FOR UPDATE
        """, params, String.class);
    if (patients.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vérifiez le patient, la catégorie et le compte.");
    }
    if (!force) {
      var openToday = jdbc.query(FILE_SELECT + """
          WHERE f.FK_PERSONNE_PATIENT = :ipp
            AND UPPER(TRIM(IFNULL(f.typehospi,''))) = :type
            AND DATE(f.DATE_CREAT) = :day
            AND (f.ETAT IS NULL OR f.ETAT = 1)
            AND (f.statut IS NULL OR TRIM(f.statut) = '' OR UPPER(TRIM(f.statut)) = 'EN COURS')
          ORDER BY f.DATE_CREAT DESC, f.CODE DESC
          LIMIT 1
          """, params, (rs, row) -> readFile(rs));
      if (!openToday.isEmpty()) throw new OpenMedicalFile(openToday.getFirst());
    }
    Integer occupied = jdbc.queryForObject("SELECT COUNT(*) FROM t_fiche_medicale WHERE CODE = :code", params, Integer.class);
    if (occupied != null && occupied > 0) throw new DuplicateKeyException("Numéro de fiche indisponible.");
    int inserted = jdbc.update("""
        INSERT INTO t_fiche_medicale
          (CODE, FK_PERSONNE_PATIENT, FK_CATEGORIE_PATIENT, FK_COMPTE_A_FACTURER,
           DATE_CREAT, ETAT, URGENCE, typehospi, statut)
        SELECT :code, CODE, :category, :account, :createdAt, 1, :urgent, :type, 'EN COURS'
        FROM t_personne
        WHERE CODE = :ipp AND PERSONNE_PHYSIQUE = 1
          AND EXISTS (SELECT 1 FROM t_categorie_patient WHERE CODE = :category)
          AND EXISTS (SELECT 1 FROM t_compte WHERE CODE = :account)
        """, params);
    if (inserted != 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vérifiez le patient, la catégorie et le compte.");
    }
    return findMedicalFile(jdbc, params);
  }

  private static MedicalFile findMedicalFile(NamedParameterJdbcTemplate jdbc, MapSqlParameterSource params) {
    var files = jdbc.query(FILE_SELECT + " WHERE f.CODE = :code AND f.FK_PERSONNE_PATIENT = :ipp",
        params, (rs, row) -> readFile(rs));
    if (files.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Le numéro de fiche attribué est introuvable dans Mediline.");
    }
    return files.getFirst();
  }

  private static final class OpenMedicalFile extends RuntimeException {
    private final MedicalFile file;

    private OpenMedicalFile(MedicalFile file) {
      this.file = file;
    }
  }

  private static MedicalFile readFile(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new MedicalFile(rs.getString("CODE"), rs.getString("date_creation"),
        normalizeCareType(rs.getString("typehospi")),
        normalizeFileStatus(rs.getString("statut")),
        rs.getBoolean("URGENCE"), rs.getObject("ETAT", Boolean.class),
        rs.getString("categorie_code"), rs.getString("categorie"),
        rs.getString("compte_code"), rs.getString("compte"));
  }

  private static List<MedicalFile> dedupeFiles(List<MedicalFile> rows) {
    if (rows == null || rows.isEmpty()) return List.of();
    Map<String, MedicalFile> unique = new LinkedHashMap<>();
    for (MedicalFile file : rows) {
      if (file == null || !hasValue(file.number())) continue;
      unique.putIfAbsent(file.number().trim().toUpperCase(Locale.ROOT), file);
    }
    return List.copyOf(unique.values());
  }

  private static String normalizeCareType(String type) {
    if (!hasValue(type)) return null;
    String raw = type.trim().toUpperCase(Locale.ROOT).replace('_', ' ');
    if (FILE_TYPES.contains(raw)) return raw;
    if (raw.contains("THERAPIE") || raw.contains("THÉRAPIE")) return "THERAPIE AMBULATOIRE";
    if (raw.contains("DOMICILE")) return "SOINS A DOMICILE";
    if (raw.contains("DIAGNOST")) return "DIAGNOSTICS";
    if (raw.contains("HOSPITAL")) return "HOSPITALISATION";
    if (raw.contains("URGENCE")) return "URGENCE";
    if (raw.contains("AMBULATOIRE") || raw.equals("OUT AMBULATOIRE") || raw.equals("CONSULTATION")) {
      return "AMBULATOIRE";
    }
    return raw;
  }

  private static String normalizeFileStatus(String status) {
    if (!hasValue(status)) return "INDETERMINE";
    String raw = status.trim().toUpperCase(Locale.ROOT);
    if (raw.equals("EN COURS") || raw.equals("EN_COURS") || raw.equals("OUVERT") || raw.equals("OPEN")) {
      return "EN COURS";
    }
    if (raw.equals("TERMINE") || raw.equals("TERMINÉ") || raw.equals("TERMINEE") || raw.equals("CLOTURE")
        || raw.equals("CLOTURÉ") || raw.equals("CLOSED")) {
      return "TERMINE";
    }
    if (raw.equals("INDETERMINE") || raw.equals("INDÉTERMINÉ")) return "INDETERMINE";
    return "INDETERMINE";
  }

  /** Accès Cardex complet (fiches, création, détail médical). */
  private static final List<String> CARDEX_ACCESS_PERMS = List.of(
      "MODULE_DMI", "SIDEBAR_CARDEX", "DMI_CARDEX_VIEW",
      "SIDEBAR_RECEPTION", "DMI_RECEPTION_PATIENT_VIEW");

  /**
   * Recherche d'identité patient — moteur unifié DMI / Facturation.
   * N'accorde pas l'accès au dossier médical complet.
   */
  private static final List<String> PATIENT_IDENTITY_SEARCH_PERMS = List.of(
      "MODULE_DMI", "SIDEBAR_CARDEX", "DMI_CARDEX_VIEW", "DMI_CARDEX_RECHERCHE",
      "DMI_ACCUEIL_RECHERCHE_PATIENT", "SIDEBAR_RECEPTION", "DMI_RECEPTION_PATIENT_VIEW",
      "FACTURATION_AYANTSDROIT_VIEW", "FACTURATION_AYANTSDROIT_CREATE");

  private NamedParameterJdbcTemplate database(Principal principal) {
    return requireMediline(principal, CARDEX_ACCESS_PERMS,
        "Vous ne disposez pas des autorisations nécessaires pour accéder au cardex.");
  }

  private NamedParameterJdbcTemplate searchDatabase(Principal principal) {
    return requireMediline(principal, PATIENT_IDENTITY_SEARCH_PERMS,
        "Vous ne disposez pas des autorisations nécessaires pour rechercher un patient.");
  }

  private NamedParameterJdbcTemplate requireMediline(Principal principal, List<String> anyOf,
      String forbiddenMessage) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    Utilisateur user = users.findByUsername(principal.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    boolean allowed = permissionAuthorization.hasAnyPermission(user.getId(), anyOf);
    if (!allowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }
    var jdbc = mediline.getIfAvailable();
    if (jdbc == null) throw unavailable();
    return jdbc;
  }

  private static boolean hasValue(String value) {
    return value != null && !value.isBlank();
  }

  private static String checked(String value, int max) {
    if (value.trim().length() > max) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Critère de recherche trop long.");
    }
    return value.trim();
  }

  private static String contains(String value) {
    return "%" + value.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }

  /**
   * Recherche rapide multiterme sur l'identité concaténée (AND entre termes).
   * Un token unique contenant un chiffre est traité comme IPP exact.
   */
  private static void applyIdentite(StringBuilder where, MapSqlParameterSource params, String value) {
    if (!hasValue(value)) return;
    String raw = checked(value, 150);
    String[] terms = raw.split("[\\p{Z}\\s\\p{Pd}'’]+");
    List<String> nonEmpty = new ArrayList<>();
    for (String term : terms) {
      if (!term.isEmpty()) nonEmpty.add(term);
    }
    if (nonEmpty.size() == 1 && nonEmpty.get(0).chars().anyMatch(Character::isDigit)) {
      where.append(" AND p.CODE = :ippFromIdentite");
      params.addValue("ippFromIdentite", nonEmpty.get(0));
      return;
    }
    addContains(where, params, "CONCAT_WS(' ', p.NOM, p.POSTNOM, p.PRENOM)", "identite", raw, 150);
  }

  private static void addContains(StringBuilder where, MapSqlParameterSource params,
      String expression, String param, String value, int max) {
    if (!hasValue(value)) return;
    String[] terms = checked(value, max).split("[\\p{Z}\\s\\p{Pd}'’]+");
    for (int index = 0; index < terms.length; index++) {
      if (terms[index].isEmpty()) continue;
      String key = param + index;
      where.append(" AND CONVERT(").append(expression)
          .append(" USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE :").append(key).append(" ESCAPE '!'");
      params.addValue(key, contains(terms[index]));
    }
  }

  private static ResponseStatusException unavailable() {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La source Mediline est indisponible.");
  }

  private static <T> Results<T> limited(List<T> rows, int limit) {
    return new Results<>(rows.subList(0, Math.min(rows.size(), limit)), rows.size() > limit);
  }

  public record Criteria(String nom, String postnom, String prenom, String lieuNaissance,
      LocalDate dateNaissance, String nIpp, String nFiche, String compteCode, String identite) {}
  public record Results<T>(List<T> items, boolean hasMore) {}

  public record RecentPatient(String code, String nom, String postnom, String prenom, String sexe,
      LocalDate dateNaissance, String ancienDossier, String pec, LocalDate dateCreation) {}
  public record Company(String code, String intitule) {}
  public record Patient(String code, String nom, String postnom, String prenom, String sexe,
      LocalDate dateNaissance, String lieuNaissance, String telephone, String adresse,
      String fonction, String ancienDossier, String compteCode, String compteIntitule,
      String pec, String email, String categorie, String categorieCode, LocalDate dateCreation) {}
  public record MedicalFile(String number, String date, String type, String status, boolean urgent,
      Boolean active, String categorieCode, String categorie, String compteCode, String compte) {}
  public record CreateFile(UUID idRequete, String type, String categorieCode, String compteCode) {}
  public record DuplicateFileResponse(String message, MedicalFile existing) {}
  public record FilePage(List<MedicalFile> items, boolean hasMore, int page, int pageSize, long totalPages, long total) {}
  public record CareQueueRow(String ipp, String patient, String fiche, String openedAt) {}
  public record CareQueuePage(List<CareQueueRow> items, boolean hasMore, int page, int pageSize, long totalPages, long total) {}
  public record PatientCreate(UUID idRequete, Map<String, String> fields) {}
}
