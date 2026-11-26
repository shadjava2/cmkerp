package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rapprochement intelligent sociétés ERP ↔ comptes Mediline.
 * L'algorithme propose ; l'utilisateur valide ; {@link SocieteMedilineMappingService} persiste.
 */
@Service
public class SocieteMedilineMatchingService {

  private static final Logger log = LoggerFactory.getLogger(SocieteMedilineMatchingService.class);
  private static final int MAX_CANDIDATES = 5;
  private static final long CACHE_TTL_MS = 5 * 60_000L;

  private final ErpFacturationRelationalStore store;
  private final SocieteMedilineMappingService mappingService;
  private final JdbcTemplate medilineJdbc;
  private final AtomicReference<AnalysisCache> cache = new AtomicReference<>();

  public SocieteMedilineMatchingService(
      ErpFacturationRelationalStore store,
      SocieteMedilineMappingService mappingService,
      @Autowired(required = false) @Qualifier("medilineJdbcTemplate") JdbcTemplate medilineJdbc) {
    this.store = store;
    this.mappingService = mappingService;
    this.medilineJdbc = medilineJdbc;
  }

  public Map<String, Object> analyze(boolean forceRefresh) {
    AnalysisCache current = cache.get();
    if (!forceRefresh && current != null && !current.expired()) {
      return toStatsPayload(current);
    }
    AnalysisCache fresh = runAnalysis();
    cache.set(fresh);
    return toStatsPayload(fresh);
  }

  public Map<String, Object> stats() {
    AnalysisCache current = cache.get();
    if (current == null || current.expired()) {
      return analyze(true);
    }
    return toStatsPayload(current);
  }

  public Map<String, Object> listProposals(String level, String q, int page, int size) {
    AnalysisCache current = ensureCache();
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
    String filter = level == null || level.isBlank() ? "ALL" : level.trim().toUpperCase(Locale.ROOT);

    List<Map<String, Object>> filtered = new ArrayList<>();
    for (ProposalRow row : current.rows) {
      if (!ReferentialLabelMatcher.levelMatchesFilter(filter, row.bestScore, row.ambiguous)) {
        continue;
      }
      if (needle != null) {
        String hay = ((row.societeCode == null ? "" : row.societeCode) + " "
            + (row.societeDesignation == null ? "" : row.societeDesignation) + " "
            + (row.bestCompteCode == null ? "" : row.bestCompteCode) + " "
            + (row.bestCompteIntitule == null ? "" : row.bestCompteIntitule)).toLowerCase(Locale.ROOT);
        if (!hay.contains(needle)) {
          continue;
        }
      }
      filtered.add(toProposalMap(row));
    }

    int from = Math.min(safePage * safeSize, filtered.size());
    int to = Math.min(from + safeSize, filtered.size());
    Map<String, Object> result = new LinkedHashMap<>(toStatsPayload(current));
    result.put("items", filtered.subList(from, to));
    result.put("filteredTotal", filtered.size());
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("level", filter);
    result.put("validationRequired", true);
    return result;
  }

  public Map<String, Object> batchValidate(List<Map<String, Object>> items, Long userId) {
    if (items == null || items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune correspondance sélectionnée.");
    }
    if (items.size() > 500) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lot trop volumineux (max 500).");
    }

    int requested = items.size();
    int validated = 0;
    int skipped = 0;
    int conflicts = 0;
    int failed = 0;
    List<Map<String, Object>> details = new ArrayList<>();
    Set<Long> societesInBatch = new HashSet<>();
    // Plusieurs sociétés peuvent partager un compte après validation explicite.

    for (Map<String, Object> item : items) {
      Long societeId = longOrNull(item.get("societeId"));
      String compteCode = str(item.get("medilineCompteCode"));
      String compteIntitule = str(item.get("medilineCompteIntitule"));
      Map<String, Object> detail = new LinkedHashMap<>();
      detail.put("societeId", societeId);
      detail.put("medilineCompteCode", compteCode);

      if (societeId == null || compteCode == null) {
        failed++;
        detail.put("status", "FAILED");
        detail.put("message", "Société ou compte manquant.");
        details.add(detail);
        continue;
      }
      if (!societesInBatch.add(societeId)) {
        skipped++;
        detail.put("status", "SKIPPED");
        detail.put("message", "Société dupliquée dans le lot.");
        details.add(detail);
        continue;
      }

      var existing = store.findSocieteMedilineMapping(societeId);
      if (existing.isEmpty()) {
        failed++;
        detail.put("status", "FAILED");
        detail.put("message", "Société introuvable.");
        details.add(detail);
        continue;
      }
      String currentStatus = str(existing.get().get("medilineMappingStatus"));
      String currentCode = str(existing.get().get("medilineCompteCode"));
      if ("MATCHED".equalsIgnoreCase(currentStatus) && currentCode != null) {
        if (currentCode.equalsIgnoreCase(compteCode)) {
          skipped++;
          detail.put("status", "SKIPPED");
          detail.put("message", "Déjà associée à ce compte.");
          details.add(detail);
          continue;
        }
        conflicts++;
        detail.put("status", "CONFLICT");
        detail.put("message", "Société déjà associée à " + currentCode + ".");
        details.add(detail);
        continue;
      }

      if (medilineJdbc != null && !compteExists(compteCode)) {
        failed++;
        detail.put("status", "FAILED");
        detail.put("message", "Compte Mediline introuvable.");
        details.add(detail);
        continue;
      }

      try {
        if (compteIntitule == null) {
          compteIntitule = loadCompteIntitule(compteCode).orElse(null);
        }
        Map<String, Object> bound = mappingService.bind(societeId, compteCode, compteIntitule, userId);
        if (!"MATCHED".equalsIgnoreCase(str(bound.get("medilineMappingStatus")))
            || str(bound.get("medilineCompteCode")) == null) {
          failed++;
          detail.put("status", "FAILED");
          detail.put("message", "Persistance non confirmée.");
          details.add(detail);
          continue;
        }
        validated++;
        detail.put("status", "VALIDATED");
        detail.put("medilineCompteIntitule", bound.get("medilineCompteIntitule"));
        details.add(detail);
      } catch (ResponseStatusException ex) {
        failed++;
        detail.put("status", "FAILED");
        detail.put("message", ex.getReason() == null ? ex.getMessage() : ex.getReason());
        details.add(detail);
      } catch (RuntimeException ex) {
        failed++;
        detail.put("status", "FAILED");
        detail.put("message", ex.getMessage());
        details.add(detail);
      }
    }

    // Invalide le cache : les sociétés validées doivent sortir des propositions.
    cache.set(null);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("requested", requested);
    result.put("validated", validated);
    result.put("skipped", skipped);
    result.put("conflicts", conflicts);
    result.put("failed", failed);
    result.put("success", failed == 0 && conflicts == 0);
    result.put("details", details);
    return result;
  }

  private AnalysisCache ensureCache() {
    AnalysisCache current = cache.get();
    if (current == null || current.expired()) {
      current = runAnalysis();
      cache.set(current);
    }
    return current;
  }

  private AnalysisCache runAnalysis() {
    if (medilineJdbc == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mediline indisponible.");
    }
    long t0 = System.currentTimeMillis();
    long societesTotal = store.countSocieteMedilineMappings(null, null);
    long matchedTotal = store.countSocieteMedilineMappings(null, "MATCHED");
    long unmatchedTotal = store.countSocieteMedilineMappings(null, "UNMATCHED");

    List<Map<String, Object>> societes = store.listAllSocieteMedilineMappings("UNMATCHED", 5000);
    List<Map<String, Object>> comptes = medilineJdbc.queryForList(
        """
            SELECT CODE, INTITULE, CATEGORIE_PATIENT, ETAT
            FROM t_compte
            WHERE CODE <> ''
            ORDER BY INTITULE, CODE
            LIMIT 5000
            """);

    Map<String, List<CompteRef>> byCompact = new HashMap<>();
    Map<String, List<CompteRef>> byToken = new HashMap<>();
    List<CompteRef> allComptes = new ArrayList<>(comptes.size());
    for (Map<String, Object> c : comptes) {
      CompteRef ref = CompteRef.from(c);
      allComptes.add(ref);
      if (!ref.compact.isEmpty()) {
        byCompact.computeIfAbsent(ref.compact, k -> new ArrayList<>()).add(ref);
      }
      for (String token : ReferentialLabelMatcher.tokens(ref.spaced)) {
        byToken.computeIfAbsent(token, k -> new ArrayList<>()).add(ref);
      }
    }

    List<ProposalRow> rows = new ArrayList<>();
    int exact = 0;
    int veryProbable = 0;
    int probable = 0;
    int uncertain = 0;
    int weak = 0;
    int none = 0;
    int ambiguous = 0;

    for (Map<String, Object> societe : societes) {
      String designation = str(societe.get("societeDesignation"));
      String code = str(societe.get("societeCode"));
      String compact = ReferentialLabelMatcher.normalizeCompact(designation);
      String spaced = ReferentialLabelMatcher.normalizeSpaced(designation);

      Set<CompteRef> pool = new HashSet<>();
      List<CompteRef> exactHits = byCompact.get(compact);
      if (exactHits != null) {
        pool.addAll(exactHits);
      }
      for (String token : ReferentialLabelMatcher.tokens(spaced)) {
        List<CompteRef> hits = byToken.get(token);
        if (hits == null) {
          continue;
        }
        int n = Math.min(hits.size(), 120);
        for (int i = 0; i < n; i++) {
          pool.add(hits.get(i));
        }
      }
      // Filet de sécurité si index pauvre (abréviations très courtes).
      if (pool.isEmpty() && compact.length() >= 3) {
        for (CompteRef ref : allComptes) {
          if (ref.compact.contains(compact) || compact.contains(ref.compact)) {
            pool.add(ref);
          }
          if (pool.size() > 80) {
            break;
          }
        }
      }

      List<Candidate> scored = new ArrayList<>();
      for (CompteRef ref : pool) {
        int score = ReferentialLabelMatcher.scorePercent(designation, ref.intitule);
        // Code égalité n'accorde jamais 100 % — bonus secondaire max 70.
        if (code != null && ReferentialLabelMatcher.normalizeCompact(code).equals(ref.compactCode)
            && score < 70) {
          score = Math.max(score, 70);
        }
        if (score < 1) {
          continue;
        }
        scored.add(new Candidate(ref, score));
      }
      scored.sort(Comparator.comparingInt(Candidate::score).reversed()
          .thenComparing(c -> c.ref().code, String.CASE_INSENSITIVE_ORDER));

      ProposalRow row = new ProposalRow();
      row.societeId = ((Number) societe.get("societeId")).longValue();
      row.societeCode = code;
      row.societeDesignation = designation;
      row.contratCode = str(societe.get("contratCode"));
      row.contratDesignation = str(societe.get("contratDesignation"));
      row.mappingStatus = str(societe.get("medilineMappingStatus"));

      if (scored.isEmpty()) {
        row.bestScore = 0;
        row.ambiguous = false;
        row.reason = "Aucun candidat textuel";
        none++;
      } else {
        int top = scored.get(0).score();
        long topCount = scored.stream().filter(c -> c.score() == top).count();
        row.ambiguous = topCount > 1;
        row.bestScore = top;
        row.bestCompteCode = scored.get(0).ref().code;
        row.bestCompteIntitule = scored.get(0).ref().intitule;
        row.bestCategorie = scored.get(0).ref().categorie;
        row.bestEtat = scored.get(0).ref().etat;
        row.reason = row.ambiguous
            ? "Ambiguë : " + topCount + " comptes au score " + top + " %"
            : (top >= 100 ? "Identité après normalisation" : "Similarité textuelle");
        int limit = Math.min(MAX_CANDIDATES, scored.size());
        for (int i = 0; i < limit; i++) {
          Candidate c = scored.get(i);
          Map<String, Object> cand = new LinkedHashMap<>();
          cand.put("medilineCompteCode", c.ref().code);
          cand.put("medilineCompteIntitule", c.ref().intitule);
          cand.put("categoriePatient", c.ref().categorie);
          cand.put("etat", c.ref().etat);
          cand.put("score", c.score());
          cand.put("level", ReferentialLabelMatcher.levelOf(c.score()));
          row.candidates.add(cand);
        }
        if (row.ambiguous) {
          ambiguous++;
        }
        String level = ReferentialLabelMatcher.levelOf(top);
        switch (level) {
          case "EXACT" -> exact++;
          case "VERY_PROBABLE" -> veryProbable++;
          case "PROBABLE" -> probable++;
          case "UNCERTAIN" -> uncertain++;
          case "WEAK" -> weak++;
          default -> none++;
        }
      }
      rows.add(row);
    }

    rows.sort(Comparator.comparingInt((ProposalRow r) -> r.bestScore).reversed()
        .thenComparing(r -> r.societeCode == null ? "" : r.societeCode));

    AnalysisCache built = new AnalysisCache(
        Instant.now(),
        societesTotal,
        matchedTotal,
        unmatchedTotal,
        comptes.size(),
        exact,
        veryProbable,
        probable,
        uncertain,
        weak,
        none,
        ambiguous,
        rows);
    log.info(
        "Rapprochement sociétés/Mediline : societes={} unmatched={} comptes={} exact={} 90+={} 80+={} ambiguës={} en {} ms",
        societesTotal, unmatchedTotal, comptes.size(), exact, veryProbable, probable, ambiguous,
        System.currentTimeMillis() - t0);
    return built;
  }

  private Map<String, Object> toStatsPayload(AnalysisCache c) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("analyzedAt", c.analyzedAt.toString());
    result.put("societesTotal", c.societesTotal);
    result.put("medilineComptesTotal", c.medilineComptesTotal);
    result.put("matchedTotal", c.matchedTotal);
    result.put("unmatchedTotal", c.unmatchedTotal);
    result.put("exact", c.exact);
    result.put("veryProbable", c.veryProbable);
    result.put("probable", c.probable);
    result.put("uncertain", c.uncertain);
    result.put("weak", c.weak);
    result.put("none", c.none);
    result.put("ambiguous", c.ambiguous);
    result.put("proposalsTotal", c.rows.size());
    result.put("cacheTtlMs", CACHE_TTL_MS);
    return result;
  }

  private static Map<String, Object> toProposalMap(ProposalRow row) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("societeId", row.societeId);
    m.put("societeCode", row.societeCode);
    m.put("societeDesignation", row.societeDesignation);
    m.put("contratCode", row.contratCode);
    m.put("contratDesignation", row.contratDesignation);
    m.put("medilineMappingStatus", row.mappingStatus);
    m.put("bestScore", row.bestScore);
    m.put("level", ReferentialLabelMatcher.levelOf(row.bestScore));
    m.put("ambiguous", row.ambiguous);
    m.put("batchEligible", row.bestScore > 0 && !row.ambiguous);
    m.put("reason", row.reason);
    m.put("medilineCompteCode", row.bestCompteCode);
    m.put("medilineCompteIntitule", row.bestCompteIntitule);
    m.put("categoriePatient", row.bestCategorie);
    m.put("etat", row.bestEtat);
    m.put("candidates", row.candidates);
    return m;
  }

  private boolean compteExists(String code) {
    Integer n = medilineJdbc.queryForObject(
        "SELECT COUNT(*) FROM t_compte WHERE CODE = ?", Integer.class, code);
    return n != null && n > 0;
  }

  private java.util.Optional<String> loadCompteIntitule(String code) {
    try {
      List<String> rows = medilineJdbc.query(
          "SELECT INTITULE FROM t_compte WHERE CODE = ? LIMIT 1",
          (rs, i) -> rs.getString(1),
          code);
      return rows.stream().findFirst().map(s -> s == null ? null : s.trim()).filter(s -> s != null && !s.isEmpty());
    } catch (Exception ex) {
      return java.util.Optional.empty();
    }
  }

  private static Long longOrNull(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(raw).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String str(Object raw) {
    if (raw == null) {
      return null;
    }
    String text = String.valueOf(raw).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }

  private record CompteRef(
      String code,
      String intitule,
      String categorie,
      Object etat,
      String spaced,
      String compact,
      String compactCode) {
    static CompteRef from(Map<String, Object> row) {
      String code = String.valueOf(row.getOrDefault("CODE", "")).trim();
      String intitule = row.get("INTITULE") == null ? "" : String.valueOf(row.get("INTITULE")).trim();
      return new CompteRef(
          code,
          intitule,
          row.get("CATEGORIE_PATIENT") == null ? null : String.valueOf(row.get("CATEGORIE_PATIENT")),
          row.get("ETAT"),
          ReferentialLabelMatcher.normalizeSpaced(intitule),
          ReferentialLabelMatcher.normalizeCompact(intitule),
          ReferentialLabelMatcher.normalizeCompact(code));
    }
  }

  private record Candidate(CompteRef ref, int score) {}

  private static final class ProposalRow {
    long societeId;
    String societeCode;
    String societeDesignation;
    String contratCode;
    String contratDesignation;
    String mappingStatus;
    int bestScore;
    boolean ambiguous;
    String reason;
    String bestCompteCode;
    String bestCompteIntitule;
    String bestCategorie;
    Object bestEtat;
    final List<Map<String, Object>> candidates = new ArrayList<>();
  }

  private static final class AnalysisCache {
    final Instant analyzedAt;
    final long societesTotal;
    final long matchedTotal;
    final long unmatchedTotal;
    final int medilineComptesTotal;
    final int exact;
    final int veryProbable;
    final int probable;
    final int uncertain;
    final int weak;
    final int none;
    final int ambiguous;
    final List<ProposalRow> rows;

    AnalysisCache(
        Instant analyzedAt,
        long societesTotal,
        long matchedTotal,
        long unmatchedTotal,
        int medilineComptesTotal,
        int exact,
        int veryProbable,
        int probable,
        int uncertain,
        int weak,
        int none,
        int ambiguous,
        List<ProposalRow> rows) {
      this.analyzedAt = analyzedAt;
      this.societesTotal = societesTotal;
      this.matchedTotal = matchedTotal;
      this.unmatchedTotal = unmatchedTotal;
      this.medilineComptesTotal = medilineComptesTotal;
      this.exact = exact;
      this.veryProbable = veryProbable;
      this.probable = probable;
      this.uncertain = uncertain;
      this.weak = weak;
      this.none = none;
      this.ambiguous = ambiguous;
      this.rows = rows;
    }

    boolean expired() {
      return Instant.now().toEpochMilli() - analyzedAt.toEpochMilli() > CACHE_TTL_MS;
    }
  }
}
