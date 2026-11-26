package cd.shad.erp.cmk.cmkerp.gateway.mediline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ErpFacturationRelationalStore;
import cd.shad.erp.cmk.cmkerp.gateway.mediline.PatientIdentityMatcher.IdentityParts;
import cd.shad.erp.cmk.cmkerp.gateway.mediline.PatientIdentityMatcher.MatchScore;

/**
 * Détection intelligente des doublons patients (Mediline + inscriptions ERP en cours).
 * Phase 1 : candidats sélectifs. Phase 2 : scoring multicritère.
 */
@Service
public class PatientDuplicateDetectionService {

  private static final Logger log = LoggerFactory.getLogger(PatientDuplicateDetectionService.class);
  private static final int CANDIDATE_POOL_LIMIT = 120;
  private static final int DISPLAY_LIMIT = 12;
  private static final int MIN_DISPLAY_SCORE = PatientIdentityMatcher.THRESHOLD_POSSIBLE;

  private final NamedParameterJdbcTemplate mediline;
  private final JdbcTemplate medilineJdbc;
  private final ErpFacturationRelationalStore store;

  public PatientDuplicateDetectionService(
      @Qualifier("medilineNamedParameterJdbcTemplate") ObjectProvider<NamedParameterJdbcTemplate> mediline,
      @Qualifier("medilineJdbcTemplate") ObjectProvider<JdbcTemplate> medilineJdbc,
      ErpFacturationRelationalStore store) {
    this.mediline = mediline.getIfAvailable();
    this.medilineJdbc = medilineJdbc.getIfAvailable();
    this.store = store;
  }

  public Map<String, Object> analyze(Map<String, Object> identity, String excludeRequestId) {
    if (mediline == null || medilineJdbc == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "La vérification des doublons est indisponible : Mediline ne répond pas. Réessayez.");
    }
    IdentityParts probe = partsFrom(identity);
    String composed = PatientIdentityMatcher.composeIdentity(probe.nom(), probe.postnom(), probe.prenom());
    if (composed.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire pour analyser les doublons.");
    }

    long started = System.currentTimeMillis();
    List<Map<String, Object>> pool = fetchCandidatePool(probe);
    List<Map<String, Object>> scored = new ArrayList<>();
    for (Map<String, Object> row : pool) {
      IdentityParts cand = partsFrom(row);
      MatchScore match = PatientIdentityMatcher.score(probe, cand);
      if (match.scorePercent() < MIN_DISPLAY_SCORE && match.identityScore() < 80) {
        continue;
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("ipp", str(row.get("ipp")));
      item.put("nom", str(row.get("nom")));
      item.put("postnom", str(row.get("postnom")));
      item.put("prenom", str(row.get("prenom")));
      item.put("nomComplet", composeDisplay(row));
      item.put("sexe", str(row.get("sexe")));
      item.put("sexeLabel", sexLabel(str(row.get("sexe"))));
      item.put("dateNaissance", str(row.get("dateNaissance")));
      item.put("lieuNaissance", str(row.get("lieuNaissance")));
      item.put("telephone", str(row.get("telephone")));
      item.put("email", str(row.get("email")));
      item.put("pec", str(row.get("pec")));
      item.put("score", match.scorePercent());
      item.put("completeness", match.completenessPercent());
      item.put("riskBand", match.riskBand());
      item.put("riskLabel", PatientIdentityMatcher.riskLabel(match.riskBand()));
      item.put("identityScore", match.identityScore());
      item.put("reasons", match.reasons());
      item.put("source", "MEDILINE");
      scored.add(item);
    }

    // Inscriptions ERP concurrentes / interrompues.
    for (Map<String, Object> pending : store.findPendingPatientRegistrations(80)) {
      String requestId = str(pending.get("requestId"));
      if (excludeRequestId != null && excludeRequestId.equalsIgnoreCase(requestId)) {
        continue;
      }
      IdentityParts cand = partsFromPayload(str(pending.get("payloadJson")));
      if (cand == null) {
        continue;
      }
      MatchScore match = PatientIdentityMatcher.score(probe, cand);
      if (match.scorePercent() < MIN_DISPLAY_SCORE) {
        continue;
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("ipp", str(pending.get("medilineIpp")));
      item.put("requestId", requestId);
      item.put("nom", cand.nom());
      item.put("postnom", cand.postnom());
      item.put("prenom", cand.prenom());
      item.put(
          "nomComplet",
          PatientIdentityMatcher.composeIdentity(cand.nom(), cand.postnom(), cand.prenom()));
      item.put("sexe", cand.sexe());
      item.put("sexeLabel", sexLabel(cand.sexe()));
      item.put("dateNaissance", cand.dateNaissance());
      item.put("telephone", cand.telephone());
      item.put("score", match.scorePercent());
      item.put("completeness", match.completenessPercent());
      item.put("riskBand", match.riskBand());
      item.put("riskLabel", PatientIdentityMatcher.riskLabel(match.riskBand()));
      item.put("identityScore", match.identityScore());
      item.put("reasons", match.reasons());
      item.put("source", "ERP_PENDING");
      item.put("medilineStatus", pending.get("medilineStatus"));
      scored.add(item);
    }

    scored.sort(Comparator
        .comparing((Map<String, Object> m) -> ((Number) m.get("score")).intValue())
        .reversed()
        .thenComparing(m -> String.valueOf(m.getOrDefault("ipp", ""))));

    // Ne jamais masquer une correspondance forte hors fenêtre d'affichage.
    List<Map<String, Object>> strong = scored.stream()
        .filter(m -> ((Number) m.get("score")).intValue() >= PatientIdentityMatcher.THRESHOLD_VERY_PROBABLE)
        .toList();
    List<Map<String, Object>> displayed = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (Map<String, Object> item : strong) {
      String key = keyOf(item);
      if (seen.add(key)) {
        displayed.add(item);
      }
    }
    for (Map<String, Object> item : scored) {
      if (displayed.size() >= DISPLAY_LIMIT) {
        break;
      }
      String key = keyOf(item);
      if (seen.add(key)) {
        displayed.add(item);
      }
    }

    int maxScore = scored.stream()
        .mapToInt(m -> ((Number) m.get("score")).intValue())
        .max()
        .orElse(0);
    String status;
    boolean blockingRequired;
    if (scored.isEmpty()) {
      status = "NONE";
      blockingRequired = false;
    } else if (maxScore >= PatientIdentityMatcher.THRESHOLD_IDENTICAL) {
      status = "VERIFICATION_REQUIRED";
      blockingRequired = true;
    } else if (maxScore >= PatientIdentityMatcher.THRESHOLD_VERY_PROBABLE) {
      status = "VERIFICATION_REQUIRED";
      blockingRequired = true;
    } else if (maxScore >= PatientIdentityMatcher.THRESHOLD_POSSIBLE) {
      status = "CANDIDATES";
      blockingRequired = false;
    } else {
      status = "WEAK_ONLY";
      blockingRequired = false;
    }

    String fingerprint = fingerprint(probe, displayed);
    long elapsed = System.currentTimeMillis() - started;

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", status);
    result.put("blockingRequired", blockingRequired);
    result.put("fingerprint", fingerprint);
    result.put("candidateCount", scored.size());
    result.put("displayedCount", displayed.size());
    result.put("maxScore", maxScore);
    result.put("thresholds", Map.of(
        "identical", PatientIdentityMatcher.THRESHOLD_IDENTICAL,
        "veryProbable", PatientIdentityMatcher.THRESHOLD_VERY_PROBABLE,
        "possible", PatientIdentityMatcher.THRESHOLD_POSSIBLE));
    result.put("weights", Map.of(
        "identity", PatientIdentityMatcher.WEIGHT_IDENTITY,
        "dateNaissance", PatientIdentityMatcher.WEIGHT_DOB,
        "telephone", PatientIdentityMatcher.WEIGHT_PHONE,
        "sexe", PatientIdentityMatcher.WEIGHT_SEX,
        "lieuNaissance", PatientIdentityMatcher.WEIGHT_BIRTHPLACE,
        "email", PatientIdentityMatcher.WEIGHT_EMAIL));
    result.put("candidates", displayed);
    result.put("elapsedMs", elapsed);
    result.put(
        "message",
        switch (status) {
          case "NONE" -> "Aucun doublon significatif détecté.";
          case "VERIFICATION_REQUIRED" -> "Vérification d'identité obligatoire avant création.";
          case "CANDIDATES" -> "Correspondances possibles à examiner.";
          default -> "Correspondances faibles uniquement.";
        });
    log.info(
        "Duplicate check: pool={} scored={} displayed={} max={} {}ms",
        pool.size(),
        scored.size(),
        displayed.size(),
        maxScore,
        elapsed);
    return result;
  }

  /**
   * Vérifie que la confirmation client correspond encore aux candidats actuels.
   * @return analyse courante
   */
  public Map<String, Object> assertCreationAllowed(
      Map<String, Object> identity,
      String excludeRequestId,
      Map<String, Object> duplicateDecision,
      boolean canOverride) {
    Map<String, Object> analysis = analyze(identity, excludeRequestId);
    boolean blocking = Boolean.TRUE.equals(analysis.get("blockingRequired"));
    if (!blocking) {
      return analysis;
    }
    if (duplicateDecision == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Des dossiers potentiellement identiques ont été détectés. Vérifiez les doublons avant de créer.");
    }
    String decision = str(duplicateDecision.get("decision"));
    String clientFp = str(duplicateDecision.get("fingerprint"));
    String currentFp = str(analysis.get("fingerprint"));
    if (clientFp == null || !clientFp.equalsIgnoreCase(currentFp)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "De nouveaux candidats ont été détectés depuis votre dernière vérification. Relancez l'analyse des doublons.");
    }
    if ("USE_EXISTING".equalsIgnoreCase(decision)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Utilisez le dossier existant sélectionné plutôt que d'en créer un nouveau.");
    }
    if (!"ACKNOWLEDGE_DISTINCT".equalsIgnoreCase(decision)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Décision de doublon invalide. Confirmez qu'il s'agit d'un patient différent ou utilisez un dossier existant.");
    }
    if (!canOverride) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Vous n'êtes pas autorisé à créer un dossier malgré une correspondance forte.");
    }
    String justification = str(duplicateDecision.get("justification"));
    if (justification == null || justification.length() < 8) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Une justification est obligatoire pour continuer malgré une correspondance forte.");
    }
    return analysis;
  }

  private List<Map<String, Object>> fetchCandidatePool(IdentityParts probe) {
    LinkedHashMap<String, Map<String, Object>> byIpp = new LinkedHashMap<>();
    Set<String> tokens = significantTokens(probe);
    for (String token : tokens) {
      if (byIpp.size() >= CANDIDATE_POOL_LIMIT) {
        break;
      }
      queryIdentityLike(token, byIpp);
    }
    // Inversions : chercher aussi avec postnom puis nom.
    if (probe.postnom() != null && !probe.postnom().isBlank()) {
      queryIdentityLike(PatientIdentityMatcher.normalizeSpaced(probe.postnom()), byIpp);
    }
    if (probe.prenom() != null && !probe.prenom().isBlank()) {
      queryIdentityLike(PatientIdentityMatcher.normalizeSpaced(probe.prenom()), byIpp);
    }
    String phone = PatientIdentityMatcher.normalizePhone(probe.telephone());
    if (!phone.isEmpty()) {
      queryByPhone(phone, byIpp);
    }
    String dob = normalizeDob(probe.dateNaissance());
    if (!dob.isEmpty()) {
      queryByDob(dob, byIpp);
    }
    return new ArrayList<>(byIpp.values());
  }

  private void queryIdentityLike(String token, Map<String, Map<String, Object>> byIpp) {
    if (token == null || token.length() < 2) {
      return;
    }
    String pattern = "%" + token.replace("%", "").replace("_", "") + "%";
    List<Map<String, Object>> rows = mediline.query(
        """
            SELECT p.CODE AS ipp, p.NOM AS nom, p.POSTNOM AS postnom, p.PRENOM AS prenom,
                   p.SEXE AS sexe, p.DATE_NAISSANCE AS dateNaissance, p.LIEU_NAISSANCE AS lieuNaissance,
                   p.TELEPHONE AS telephone, p.EMAIL AS email, p.MAIL AS pec
            FROM t_personne p
            WHERE p.PERSONNE_PHYSIQUE = 1
              AND CONCAT_WS(' ', p.NOM, p.POSTNOM, p.PRENOM) LIKE :pattern
            ORDER BY p.CODE DESC
            LIMIT 40
            """,
        new MapSqlParameterSource("pattern", pattern),
        (rs, i) -> rowFromRs(rs));
    for (Map<String, Object> row : rows) {
      byIpp.putIfAbsent(str(row.get("ipp")), row);
    }
  }

  private void queryByPhone(String phoneTail, Map<String, Map<String, Object>> byIpp) {
    List<Map<String, Object>> rows = mediline.query(
        """
            SELECT p.CODE AS ipp, p.NOM AS nom, p.POSTNOM AS postnom, p.PRENOM AS prenom,
                   p.SEXE AS sexe, p.DATE_NAISSANCE AS dateNaissance, p.LIEU_NAISSANCE AS lieuNaissance,
                   p.TELEPHONE AS telephone, p.EMAIL AS email, p.MAIL AS pec
            FROM t_personne p
            WHERE p.PERSONNE_PHYSIQUE = 1
              AND REPLACE(REPLACE(REPLACE(IFNULL(p.TELEPHONE,''),' ',''),'-',''),'+','') LIKE :phone
            ORDER BY p.CODE DESC
            LIMIT 30
            """,
        new MapSqlParameterSource("phone", "%" + phoneTail),
        (rs, i) -> rowFromRs(rs));
    for (Map<String, Object> row : rows) {
      byIpp.putIfAbsent(str(row.get("ipp")), row);
    }
  }

  private void queryByDob(String dob, Map<String, Map<String, Object>> byIpp) {
    List<Map<String, Object>> rows = mediline.query(
        """
            SELECT p.CODE AS ipp, p.NOM AS nom, p.POSTNOM AS postnom, p.PRENOM AS prenom,
                   p.SEXE AS sexe, p.DATE_NAISSANCE AS dateNaissance, p.LIEU_NAISSANCE AS lieuNaissance,
                   p.TELEPHONE AS telephone, p.EMAIL AS email, p.MAIL AS pec
            FROM t_personne p
            WHERE p.PERSONNE_PHYSIQUE = 1
              AND p.DATE_NAISSANCE = :dob
            ORDER BY p.CODE DESC
            LIMIT 40
            """,
        new MapSqlParameterSource("dob", java.sql.Date.valueOf(dob)),
        (rs, i) -> rowFromRs(rs));
    for (Map<String, Object> row : rows) {
      byIpp.putIfAbsent(str(row.get("ipp")), row);
    }
  }

  private static Map<String, Object> rowFromRs(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("ipp", rs.getString("ipp"));
    row.put("nom", rs.getString("nom"));
    row.put("postnom", rs.getString("postnom"));
    row.put("prenom", rs.getString("prenom"));
    row.put("sexe", rs.getString("sexe"));
    var dob = rs.getDate("dateNaissance");
    row.put("dateNaissance", dob == null ? null : dob.toLocalDate().toString());
    row.put("lieuNaissance", rs.getString("lieuNaissance"));
    row.put("telephone", rs.getString("telephone"));
    row.put("email", rs.getString("email"));
    row.put("pec", rs.getString("pec"));
    return row;
  }

  private static Set<String> significantTokens(IdentityParts probe) {
    LinkedHashSet<String> tokens = new LinkedHashSet<>();
    for (String part : new String[] {probe.nom(), probe.postnom(), probe.prenom()}) {
      String spaced = PatientIdentityMatcher.normalizeSpaced(part);
      if (spaced.length() >= 3) {
        tokens.add(spaced);
      }
      for (String t : spaced.split(" ")) {
        if (t.length() >= 3) {
          tokens.add(t);
        }
      }
    }
    return tokens;
  }

  private static IdentityParts partsFrom(Map<String, Object> map) {
    if (map == null) {
      return new IdentityParts(null, null, null, null, null, null, null, null);
    }
    return new IdentityParts(
        str(map.get("nom")),
        str(map.get("postnom")),
        str(map.get("prenom")),
        str(map.get("sexe")),
        str(map.get("dateNaissance")),
        str(map.get("telephone")),
        str(map.get("lieuNaissance")),
        str(map.get("email")));
  }

  @SuppressWarnings("unchecked")
  private IdentityParts partsFromPayload(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      Map<String, Object> root = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
      Object identity = root.get("identity");
      if (identity instanceof Map<?, ?> m) {
        return partsFrom((Map<String, Object>) m);
      }
    } catch (Exception ignored) {
      // ignore malformed payloads
    }
    return null;
  }

  private static String fingerprint(IdentityParts probe, List<Map<String, Object>> candidates) {
    StringBuilder sb = new StringBuilder();
    sb.append(PatientIdentityMatcher.composeIdentity(probe.nom(), probe.postnom(), probe.prenom()))
        .append('|')
        .append(normalizeDob(probe.dateNaissance()))
        .append('|')
        .append(PatientIdentityMatcher.normalizePhone(probe.telephone()))
        .append('|')
        .append(normalizeSex(probe.sexe()));
    for (Map<String, Object> c : candidates) {
      sb.append(';')
          .append(str(c.get("ipp")))
          .append(':')
          .append(c.get("score"))
          .append(':')
          .append(str(c.get("source")));
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(dig);
    } catch (NoSuchAlgorithmException ex) {
      return Integer.toHexString(sb.toString().hashCode());
    }
  }

  private static String keyOf(Map<String, Object> item) {
    String ipp = str(item.get("ipp"));
    if (ipp != null) {
      return "IPP:" + ipp;
    }
    return "REQ:" + str(item.get("requestId"));
  }

  private static String composeDisplay(Map<String, Object> row) {
    return PatientIdentityMatcher.composeIdentity(str(row.get("nom")), str(row.get("postnom")), str(row.get("prenom")));
  }

  private static String sexLabel(String sexe) {
    String n = normalizeSex(sexe);
    if ("M".equals(n)) {
      return "Masculin";
    }
    if ("F".equals(n)) {
      return "Féminin";
    }
    return sexe == null || sexe.isBlank() ? "Non renseigné" : sexe;
  }

  private static String normalizeDob(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String t = raw.trim();
    return t.length() >= 10 ? t.substring(0, 10) : t;
  }

  private static String normalizeSex(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String t = raw.trim().toUpperCase(Locale.ROOT);
    if (t.startsWith("M")) {
      return "M";
    }
    if (t.startsWith("F")) {
      return "F";
    }
    return t;
  }

  private static String str(Object raw) {
    if (raw == null) {
      return null;
    }
    String text = String.valueOf(raw).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }
}
