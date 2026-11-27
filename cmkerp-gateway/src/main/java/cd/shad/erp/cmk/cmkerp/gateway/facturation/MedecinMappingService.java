package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Identité médicale unifiée : médecin ERP ↔ agent Mediline et médecin ERP ↔ utilisateur CMK ERP.
 *
 * <p>Le moteur propose, une personne valide. Aucune écriture dans Mediline ni dans CLINIQUE.
 */
@Service
public class MedecinMappingService {

  public static final String MEDILINE = "MEDILINE";
  public static final String USER = "UTILISATEUR";
  private static final Pattern TITLES = Pattern.compile("(?i)(?<![\\p{L}])(dr|docteur|prof|professeur|pr)(?![\\p{L}])\\.?");
  private static final int MAX_CANDIDATES = 5;
  /** En dessous, aucune proposition n'est présentée. */
  private static final int MIN_PROPOSAL = 40;

  private final NamedParameterJdbcTemplate primary;
  private final ObjectProvider<NamedParameterJdbcTemplate> mediline;

  public MedecinMappingService(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate primary,
      @Qualifier("medilineNamedParameterJdbcTemplate") ObjectProvider<NamedParameterJdbcTemplate> mediline) {
    this.primary = primary;
    this.mediline = mediline;
  }

  /** Compte rapprochable : agent Mediline (code t_login) ou utilisateur ERP (id). */
  public record Account(String id, String libelle, String identifiant, boolean actif, String specialite) {}
  public record Link(String externalId, String libelle, Integer score, String origine, LocalDateTime valideeLe) {}
  public record Candidate(String id, String libelle, int score) {}
  public record Row(long medecinId, String code, String nom, Link mediline, Link utilisateur,
      List<Candidate> candidats, Integer score, String niveau, boolean ambigue) {}
  public record Stats(long medecins, long mediline, long utilisateurs, long complets, long ambigues, long sansAssociation) {}
  public record Listing(List<Row> lignes, Stats indicateurs, List<Account> comptes) {}
  public record Item(Long medecinId, String externalId) {}
  public record Doctor(long id, String code, String nom, String specialite, String agentMediline) {}

  private record Medecin(long id, String code, String nom, boolean actif) {}

  /** Médecin ERP associé à l'utilisateur authentifié, résolu côté serveur. */
  public Optional<Doctor> doctorOfUser(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }
    var rows = primary.query("""
        SELECT m.id, m.code, m.nom, u.specialite, a.external_id AS agent
        FROM erp_medecin_mappings x
        JOIN erp_medecins m ON m.id = x.fk_medecin AND m.actif = 1
        JOIN utilisateurs u ON u.id = :user
        LEFT JOIN erp_medecin_mappings a
          ON a.type_mapping = 'MEDILINE' AND a.active_medecin = m.id
        WHERE x.type_mapping = 'UTILISATEUR' AND x.active_external = :external
        """, new MapSqlParameterSource("user", userId).addValue("external", String.valueOf(userId)),
        (rs, row) -> new Doctor(rs.getLong("id"), rs.getString("code"), rs.getString("nom"),
            rs.getString("specialite"), rs.getString("agent")));
    return rows.stream().findFirst();
  }

  /** Liens Mediline actifs : identifiant médecin ERP → code agent. */
  public Map<Long, String> medilineAgents() {
    Map<Long, String> agents = new HashMap<>();
    links(MEDILINE).forEach((doctor, link) -> agents.put(doctor, link.externalId()));
    return agents;
  }

  /** Agent Mediline validé et toujours actif pour ce médecin. */
  public Optional<Account> medilineAccountOf(long medecinId) {
    String agent = medilineAgents().get(medecinId);
    return agent == null ? Optional.empty()
        : accounts(MEDILINE).stream().filter(account -> account.id().equals(agent)).findFirst();
  }

  /** Correspondances enregistrées, sans calcul de proposition. */
  public Listing current(String type) {
    return listing(checkedType(type), false);
  }

  /** Correspondances et propositions calculées. */
  public Listing proposals(String type) {
    return listing(checkedType(type), true);
  }

  private Listing listing(String type, boolean analyze) {
    var doctors = doctors();
    var medilineLinks = links(MEDILINE);
    var userLinks = links(USER);
    var mine = MEDILINE.equals(type) ? medilineLinks : userLinks;
    List<Account> accounts = analyze ? accounts(type) : List.of();
    var analysis = analyze ? analyze(doctors, accounts, mine) : Map.<Long, List<Candidate>>of();
    var ambiguous = ambiguous(analysis);
    var rows = new ArrayList<Row>();
    for (var doctor : doctors) {
      var candidates = analysis.getOrDefault(doctor.id(), List.of());
      var link = mine.get(doctor.id());
      Integer score = link != null ? link.score() : candidates.isEmpty() ? null : candidates.getFirst().score();
      String level = link != null ? "VALIDEE" : candidates.isEmpty() ? "NONE"
          : ReferentialLabelMatcher.levelOf(candidates.getFirst().score());
      rows.add(new Row(doctor.id(), doctor.code(), doctor.nom(), medilineLinks.get(doctor.id()),
          userLinks.get(doctor.id()), candidates, score, level, ambiguous.contains(doctor.id())));
    }
    var stats = new Stats(rows.size(),
        rows.stream().filter(row -> row.mediline() != null).count(),
        rows.stream().filter(row -> row.utilisateur() != null).count(),
        rows.stream().filter(row -> row.mediline() != null && row.utilisateur() != null).count(),
        ambiguous.size(),
        rows.stream().filter(row -> row.mediline() == null && row.utilisateur() == null).count());
    return new Listing(rows, stats, accounts);
  }

  private static Map<Long, List<Candidate>> analyze(List<Medecin> doctors, List<Account> accounts,
      Map<Long, Link> linked) {
    var taken = linked.values().stream().map(Link::externalId).collect(Collectors.toSet());
    Map<Long, List<Candidate>> result = new LinkedHashMap<>();
    for (var doctor : doctors) {
      if (linked.containsKey(doctor.id())) {
        continue;
      }
      var candidates = accounts.stream()
          .filter(account -> account.actif() && !taken.contains(account.id()))
          .map(account -> new Candidate(account.id(), account.libelle(), score(doctor.nom(), account.libelle())))
          .filter(candidate -> candidate.score() >= MIN_PROPOSAL)
          .sorted(Comparator.comparingInt(Candidate::score).reversed().thenComparing(Candidate::libelle))
          .limit(MAX_CANDIDATES)
          .toList();
      if (!candidates.isEmpty()) {
        result.put(doctor.id(), candidates);
      }
    }
    return result;
  }

  /** Ambigu : plusieurs comptes au meilleur score, ou même compte meilleur choix de plusieurs médecins. */
  private static java.util.Set<Long> ambiguous(Map<Long, List<Candidate>> analysis) {
    var bestCount = analysis.values().stream()
        .collect(Collectors.groupingBy(list -> list.getFirst().id(), Collectors.counting()));
    return analysis.entrySet().stream()
        .filter(entry -> {
          var list = entry.getValue();
          return (list.size() > 1 && list.get(1).score() == list.getFirst().score())
              || bestCount.get(list.getFirst().id()) > 1;
        })
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  /** Titres professionnels ignorés ; un seul nom commun ne suffit jamais pour « très probable ». */
  static int score(String left, String right) {
    String a = TITLES.matcher(left == null ? "" : left).replaceAll(" ");
    String b = TITLES.matcher(right == null ? "" : right).replaceAll(" ");
    int score = ReferentialLabelMatcher.scorePercent(a, b);
    int shortest = Math.min(ReferentialLabelMatcher.tokens(ReferentialLabelMatcher.normalizeSpaced(a)).size(),
        ReferentialLabelMatcher.tokens(ReferentialLabelMatcher.normalizeSpaced(b)).size());
    return score < 100 && shortest <= 1 ? Math.min(score, 89) : score;
  }

  /** Validation individuelle d'une correspondance pour un médecin qui n'en a pas encore. */
  @Transactional
  public Listing validate(String type, List<Item> items, Long userId) {
    return save(checkedType(type), items, userId, false, false);
  }

  /** Validation groupée : les ambiguïtés, recalculées par le serveur, sont refusées. */
  @Transactional
  public Listing validateBulk(String type, List<Item> items, Long userId) {
    return save(checkedType(type), items, userId, true, false);
  }

  /** Remplace une correspondance existante en conservant l'ancienne dans l'historique. */
  @Transactional
  public Listing replace(String type, List<Item> items, Long userId) {
    return save(checkedType(type), items, userId, false, true);
  }

  private Listing save(String type, List<Item> items, Long userId, boolean bulk, boolean replace) {
    if (items == null || items.isEmpty() || items.size() > 500) {
      throw bad("Sélectionnez les correspondances à enregistrer.");
    }
    var doctors = doctors().stream().collect(Collectors.toMap(Medecin::id, doctor -> doctor));
    var accountList = accounts(type);
    var accounts = accountList.stream().collect(Collectors.toMap(Account::id, account -> account, (a, b) -> a));
    var existing = links(type);
    var analysis = analyze(new ArrayList<>(doctors.values()), accountList, existing);
    var ambiguous = ambiguous(analysis);
    for (var item : items) {
      var doctor = item.medecinId() == null ? null : doctors.get(item.medecinId());
      var account = item.externalId() == null ? null : accounts.get(item.externalId().trim());
      if (doctor == null || !doctor.actif() || account == null || !account.actif()) {
        throw bad("Médecin ou compte indisponible. Actualisez la liste.");
      }
      boolean linked = existing.containsKey(doctor.id());
      if (linked && !replace) {
        throw conflict(doctor.nom() + " possède déjà une correspondance. Utilisez la modification.");
      }
      if (!linked && replace) {
        throw conflict(doctor.nom() + " n’a pas de correspondance à modifier.");
      }
      var best = analysis.getOrDefault(doctor.id(), List.of()).stream().findFirst().orElse(null);
      boolean proposed = best != null && best.id().equals(account.id());
      if (bulk && (!proposed || ambiguous.contains(doctor.id()))) {
        throw conflict(doctor.nom() + " : correspondance ambiguë ou différente de la proposition, à valider individuellement.");
      }
      if (replace) {
        end(type, doctor.id(), userId);
      }
      try {
        primary.update("""
            INSERT INTO erp_medecin_mappings
              (fk_medecin, type_mapping, external_id, external_label, score, origine, active_medecin,
               active_external, validated_at, validated_by)
            VALUES (:doctor, :type, :external, :label, :score, :origin, :doctor, :external, :now, :user)
            """, new MapSqlParameterSource("doctor", doctor.id()).addValue("type", type)
            .addValue("external", account.id()).addValue("label", account.libelle())
            .addValue("score", score(doctor.nom(), account.libelle()))
            .addValue("origin", proposed ? "PROPOSITION" : "MANUEL")
            .addValue("now", now()).addValue("user", userId));
      } catch (DuplicateKeyException duplicate) {
        throw conflict("« " + account.libelle() + " » est déjà associé à un autre médecin.");
      }
    }
    // Relecture après écriture : l'écran affiche ce qui est réellement enregistré.
    return listing(type, true);
  }

  @Transactional
  public Listing dissociate(String type, Long medecinId, Long userId) {
    String checked = checkedType(type);
    if (medecinId == null || end(checked, medecinId, userId) == 0) {
      throw conflict("Aucune correspondance active à dissocier.");
    }
    return listing(checked, false);
  }

  private int end(String type, long doctor, Long userId) {
    return primary.update("""
        UPDATE erp_medecin_mappings
        SET active_medecin = NULL, active_external = NULL, ended_at = :now, ended_by = :user
        WHERE type_mapping = :type AND active_medecin = :doctor
        """, new MapSqlParameterSource("type", type).addValue("doctor", doctor)
        .addValue("now", now()).addValue("user", userId));
  }

  private Map<Long, Link> links(String type) {
    Map<Long, Link> links = new LinkedHashMap<>();
    primary.query("""
        SELECT active_medecin, external_id, external_label, score, origine, validated_at
        FROM erp_medecin_mappings
        WHERE type_mapping = :type AND active_medecin IS NOT NULL
        """, new MapSqlParameterSource("type", type), rs -> {
      int score = rs.getInt("score");
      links.put(rs.getLong("active_medecin"), new Link(rs.getString("external_id"), rs.getString("external_label"),
          rs.wasNull() ? null : score, rs.getString("origine"),
          rs.getObject("validated_at", LocalDateTime.class)));
    });
    return links;
  }

  private List<Medecin> doctors() {
    return primary.query("SELECT id, code, nom, actif FROM erp_medecins ORDER BY nom, code",
        new MapSqlParameterSource(),
        (rs, row) -> new Medecin(rs.getLong("id"), rs.getString("code"), rs.getString("nom"), rs.getBoolean("actif")));
  }

  private List<Account> accounts(String type) {
    if (USER.equals(type)) {
      return primary.query("SELECT id, username, nom, postnom, prenom, locked, specialite FROM utilisateurs"
          + " ORDER BY nom, postnom, id", new MapSqlParameterSource(), (rs, row) -> new Account(
              String.valueOf(rs.getLong("id")),
              name(rs.getString("nom"), rs.getString("postnom"), rs.getString("prenom"), rs.getString("username")),
              rs.getString("username"), !rs.getBoolean("locked"), rs.getString("specialite")));
    }
    var jdbc = mediline.getIfAvailable();
    if (jdbc == null) {
      throw unavailable();
    }
    try {
      return jdbc.query("""
          SELECT l.CODE AS code, l.USERNAME AS username, p.NOM AS nom, p.POSTNOM AS postnom, p.PRENOM AS prenom
          FROM t_login l
          LEFT JOIN t_personne p ON p.CODE = l.FK_PERSONNE
          WHERE l.ETAT = 1 AND UPPER(l.TYPE_USER) IN ('DOCTEUR', 'MEDECIN')
          ORDER BY p.NOM, p.POSTNOM, l.CODE
          """, new MapSqlParameterSource(), (rs, row) -> new Account(rs.getString("code"),
              name(rs.getString("nom"), rs.getString("postnom"), rs.getString("prenom"), rs.getString("username")),
              rs.getString("code"), true, null));
    } catch (DataAccessException exception) {
      throw unavailable();
    }
  }

  private static String name(String last, String middle, String first, String fallback) {
    String full = Stream.of(last, middle, first).filter(part -> part != null && !part.isBlank())
        .map(String::trim).collect(Collectors.joining(" "));
    return full.isEmpty() ? fallback : full;
  }

  private static String checkedType(String type) {
    if (!MEDILINE.equals(type) && !USER.equals(type)) {
      throw bad("Type de correspondance inconnu.");
    }
    return type;
  }

  private static LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of("Africa/Kinshasa")).withNano(0);
  }

  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  private static ResponseStatusException unavailable() {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La source Mediline est indisponible.");
  }
}
