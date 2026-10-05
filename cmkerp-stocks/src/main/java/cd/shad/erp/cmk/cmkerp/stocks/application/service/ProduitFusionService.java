package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueProduit;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.BatchLinkCodeCliniqueRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.BatchLinkResultResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FusionStatsResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FusionSuggestionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitFusionItemResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Fusion produits CMKERP ↔ CLINIQUE via colonne produits.CODECLINIQUE.
 * Écrit uniquement dans cmkerp-v24prod. CLINIQUE reste en lecture seule.
 */
@Service
@Slf4j
public class ProduitFusionService {

  private static final int MATCH_ERP_MAX = 4000;
  private static final int MATCH_CLINIQUE_MAX = 12000;

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;
  private final CliniqueLookupRepository cliniqueLookupRepository;
  /** Proxy self pour batch : chaque link() = transaction isolée. */
  private ProduitFusionService self;

  public ProduitFusionService(
      @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc,
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbc,
      CliniqueLookupRepository cliniqueLookupRepository) {
    this.jdbc = jdbc;
    this.namedJdbc = namedJdbc;
    this.cliniqueLookupRepository = cliniqueLookupRepository;
  }

  @Autowired
  @Lazy
  void setSelf(ProduitFusionService self) {
    this.self = self;
  }

  @Transactional(readOnly = true)
  public FusionStatsResponse stats() {
    try {
      Long total = jdbc.queryForObject("SELECT COUNT(*) FROM produits", Long.class);
      Long linked = jdbc.queryForObject(
          "SELECT COUNT(*) FROM produits WHERE CODECLINIQUE IS NOT NULL AND TRIM(CODECLINIQUE) <> ''",
          Long.class);
      long erpTotal = total != null ? total : 0L;
      long erpLinked = linked != null ? linked : 0L;
      long erpUnlinked = Math.max(0L, erpTotal - erpLinked);

      Set<String> used = loadUsedCodeClinique();
      boolean cliOk = cliniqueLookupRepository.isAvailable();
      long cliniqueTotal = cliOk ? cliniqueLookupRepository.countProduits(null) : 0L;
      long cliniqueUsed = used.size();
      long cliniqueAvailable = Math.max(0L, cliniqueTotal - cliniqueUsed);

      return FusionStatsResponse.builder()
          .erpTotal(erpTotal)
          .erpLinked(erpLinked)
          .erpUnlinked(erpUnlinked)
          .cliniqueTotal(cliniqueTotal)
          .cliniqueUsed(cliniqueUsed)
          .cliniqueAvailable(cliniqueAvailable)
          .cliniqueAvailableFlag(cliOk)
          .build();
    } catch (DataAccessException ex) {
      throw sqlFailure("stats", ex);
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listCmkerp(String query, String linkFilter, int page, int size) {
    int safeSize = Math.max(1, Math.min(size, 100));
    int safePage = Math.max(0, page);
    int offset = safePage * safeSize;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> args = new ArrayList<>();

    if (query != null && !query.isBlank()) {
      where.append(
          " AND (LOWER(IFNULL(p.nomcommercial,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.nomscientifique,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.codebarre,'')) LIKE ?"
              + " OR LOWER(IFNULL(p.CODECLINIQUE,'')) LIKE ?"
              + " OR LOWER(IFNULL(f.designation,'')) LIKE ?"
              + " OR LOWER(IFNULL(d.designation,'')) LIKE ?"
              + " OR LOWER(IFNULL(c.designation,'')) LIKE ?"
              + " OR LOWER(IFNULL(ct.designation,'')) LIKE ?)");
      String like = "%" + query.trim().toLowerCase() + "%";
      for (int i = 0; i < 8; i++) {
        args.add(like);
      }
    }
    if ("linked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND p.CODECLINIQUE IS NOT NULL AND TRIM(p.CODECLINIQUE) <> ''");
    } else if ("unlinked".equalsIgnoreCase(linkFilter)) {
      where.append(" AND (p.CODECLINIQUE IS NULL OR TRIM(p.CODECLINIQUE) = '')");
    }

    String fromJoins =
        " FROM produits p"
            + " LEFT JOIN formes f ON p.fkForme = f.id"
            + " LEFT JOIN dosages d ON p.fkDosage = d.id"
            + " LEFT JOIN conditionnements c ON p.fkConditionnement = c.id"
            + " LEFT JOIN categorie_produit ct ON p.fkCategorie = ct.id";

    try {
      Long total = jdbc.queryForObject(
          "SELECT COUNT(*)" + fromJoins + where,
          Long.class,
          args.toArray());

      String sql =
          "SELECT p.id, p.codebarre, p.nomcommercial, p.nomscientifique, p.prixachat, p.CODECLINIQUE,"
              + " f.designation AS forme, d.designation AS dosage,"
              + " c.designation AS conditionnement, ct.designation AS categorie"
              + fromJoins
              + where
              + " ORDER BY p.nomcommercial ASC"
              + " LIMIT "
              + safeSize
              + " OFFSET "
              + offset;

      List<ProduitFusionItemResponse> items = jdbc.query(sql, this::mapErpRow, args.toArray());

      Map<String, Object> result = new HashMap<>();
      result.put("content", items);
      result.put("totalElements", total != null ? total : 0L);
      result.put("page", safePage);
      result.put("size", safeSize);
      return result;
    } catch (DataAccessException ex) {
      throw sqlFailure("listCmkerp", ex);
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listClinique(String query, String linkFilter, int page, int size) {
    int safeSize = Math.max(1, Math.min(size, 100));
    int safePage = Math.max(0, page);
    int offset = safePage * safeSize;
    boolean cliniqueOk = cliniqueLookupRepository.isAvailable();

    Set<String> used = loadUsedCodeClinique();
    boolean filterUnlinked = "unlinked".equalsIgnoreCase(linkFilter);
    boolean filterLinked = "linked".equalsIgnoreCase(linkFilter);

    if (!filterUnlinked && !filterLinked) {
      long total = cliniqueLookupRepository.countProduits(query);
      List<CliniqueProduit> items = cliniqueLookupRepository.searchProduits(query, offset, safeSize);
      return cliniquePage(items, total, safePage, safeSize, cliniqueOk, used.size());
    }

    // Filtre cross-base : on page en mémoire après exclusion / inclusion des codes déjà liés ERP
    List<CliniqueProduit> filtered = new ArrayList<>();
    int scanOffset = 0;
    final int chunk = 200;
    int scanned = 0;
    final int maxScan = 20000;
    while (scanned < maxScan) {
      List<CliniqueProduit> chunkItems =
          cliniqueLookupRepository.searchProduits(query, scanOffset, chunk);
      if (chunkItems.isEmpty()) {
        break;
      }
      for (CliniqueProduit p : chunkItems) {
        String code = p.code() == null ? "" : p.code().trim();
        boolean isUsed = used.contains(code);
        if (filterUnlinked && !isUsed) {
          filtered.add(p);
        } else if (filterLinked && isUsed) {
          filtered.add(p);
        }
      }
      scanned += chunkItems.size();
      scanOffset += chunkItems.size();
      if (chunkItems.size() < chunk) {
        break;
      }
    }

    long total = filtered.size();
    int from = Math.min(offset, filtered.size());
    int to = Math.min(from + safeSize, filtered.size());
    List<CliniqueProduit> pageItems = filtered.subList(from, to);
    return cliniquePage(pageItems, total, safePage, safeSize, cliniqueOk, used.size());
  }

  @Transactional(readOnly = true)
  public List<FusionSuggestionResponse> suggest(double minScore, int limit) {
    double threshold = Math.max(0.55, Math.min(minScore, 0.99));
    int safeLimit = Math.max(1, Math.min(limit, 300));

    if (!cliniqueLookupRepository.isAvailable()) {
      return List.of();
    }

    List<ProduitFusionItemResponse> erpUnlinked = loadUnlinkedErp(MATCH_ERP_MAX);
    List<CliniqueProduit> cliniqueAll = loadCliniqueAll(MATCH_CLINIQUE_MAX);
    Set<String> usedCodes = loadUsedCodeClinique();

    List<CliniqueProduit> cliniqueFree = new ArrayList<>();
    Map<String, List<CliniqueProduit>> byExact = new HashMap<>();
    Map<String, List<CliniqueProduit>> byToken = new HashMap<>();

    for (CliniqueProduit c : cliniqueAll) {
      if (c.code() == null || c.code().isBlank()) {
        continue;
      }
      String code = c.code().trim();
      if (usedCodes.contains(code)) {
        continue;
      }
      cliniqueFree.add(c);
      indexLabel(byExact, byToken, c.designation(), c);
      indexLabel(byExact, byToken, c.libelle(), c);
    }

    record Candidate(
        ProduitFusionItemResponse erp,
        CliniqueProduit cli,
        double score,
        String reason) {}

    List<Candidate> candidates = new ArrayList<>();
    for (ProduitFusionItemResponse erp : erpUnlinked) {
      Set<CliniqueProduit> pool = new HashSet<>();
      addExactCandidates(pool, byExact, erp.getNomcommercial());
      addExactCandidates(pool, byExact, erp.getNomscientifique());
      addTokenCandidates(pool, byToken, erp.getNomcommercial());
      addTokenCandidates(pool, byToken, erp.getNomscientifique());
      // Si aucun candidat indexé, on ne scanne pas tout (trop coûteux)
      if (pool.isEmpty()) {
        continue;
      }

      FusionNameMatcher.Match best = null;
      CliniqueProduit bestCli = null;
      for (CliniqueProduit cli : pool) {
        FusionNameMatcher.Match m = FusionNameMatcher.bestMatch(
            erp.getNomcommercial(),
            erp.getNomscientifique(),
            erp.getForme(),
            erp.getDosage(),
            cli.designation(),
            cli.libelle(),
            cli.forme(),
            cli.dosage());
        if (m.score() < threshold) {
          continue;
        }
        if (best == null || m.score() > best.score()) {
          best = m;
          bestCli = cli;
        }
      }
      if (best != null && bestCli != null) {
        candidates.add(new Candidate(erp, bestCli, best.score(), best.reason()));
      }
    }

    candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());

    Set<Long> usedErp = new HashSet<>();
    Set<String> usedCli = new HashSet<>();
    List<FusionSuggestionResponse> out = new ArrayList<>();
    for (Candidate c : candidates) {
      String code = c.cli().code().trim();
      if (usedErp.contains(c.erp().getId()) || usedCli.contains(code)) {
        continue;
      }
      usedErp.add(c.erp().getId());
      usedCli.add(code);
      out.add(FusionSuggestionResponse.builder()
          .produitId(c.erp().getId())
          .nomcommercial(c.erp().getNomcommercial())
          .nomscientifique(c.erp().getNomscientifique())
          .forme(c.erp().getForme())
          .dosage(c.erp().getDosage())
          .conditionnement(c.erp().getConditionnement())
          .codeClinique(code)
          .designationClinique(c.cli().designation())
          .libelleClinique(c.cli().libelle())
          .formeClinique(c.cli().forme())
          .dosageClinique(c.cli().dosage())
          .pau(c.cli().pau())
          .score(Math.round(c.score() * 1000.0) / 1000.0)
          .matchReason(c.reason())
          .build());
      if (out.size() >= safeLimit) {
        break;
      }
    }
    log.info(
        "Fusion suggestions: {} propositions (seuil={}, erp={}, cliniqueLibre={}, candidatsBruts={})",
        out.size(),
        threshold,
        erpUnlinked.size(),
        cliniqueFree.size(),
        candidates.size());
    return out;
  }

  private static void indexLabel(
      Map<String, List<CliniqueProduit>> byExact,
      Map<String, List<CliniqueProduit>> byToken,
      String label,
      CliniqueProduit produit) {
    String norm = FusionNameMatcher.normalize(label);
    if (norm.isEmpty()) {
      return;
    }
    byExact.computeIfAbsent(norm, k -> new ArrayList<>()).add(produit);
    for (String token : FusionNameMatcher.tokens(norm)) {
      byToken.computeIfAbsent(token, k -> new ArrayList<>()).add(produit);
    }
  }

  private static void addExactCandidates(
      Set<CliniqueProduit> pool, Map<String, List<CliniqueProduit>> byExact, String label) {
    String norm = FusionNameMatcher.normalize(label);
    if (norm.isEmpty()) {
      return;
    }
    List<CliniqueProduit> hits = byExact.get(norm);
    if (hits != null) {
      pool.addAll(hits);
    }
  }

  private static void addTokenCandidates(
      Set<CliniqueProduit> pool, Map<String, List<CliniqueProduit>> byToken, String label) {
    for (String token : FusionNameMatcher.tokens(FusionNameMatcher.normalize(label))) {
      List<CliniqueProduit> hits = byToken.get(token);
      if (hits == null) {
        continue;
      }
      // Limite anti-explosion pour tokens trop communs
      int n = Math.min(hits.size(), 80);
      for (int i = 0; i < n; i++) {
        pool.add(hits.get(i));
      }
    }
  }

  @Transactional
  public ProduitFusionItemResponse link(Long produitId, String codeClinique, Long userId) {
    try {
      Integer exists = namedJdbc.queryForObject(
          "SELECT COUNT(*) FROM produits WHERE id = :id",
          Map.of("id", produitId),
          Integer.class);
      if (exists == null || exists == 0) {
        throw NotFoundException.entity("Produit", produitId);
      }

      String code = codeClinique == null || codeClinique.isBlank() ? null : codeClinique.trim();
      long uid = userId != null ? userId : 0L;

      if (code != null) {
        Map<String, Object> clearParams = new HashMap<>();
        clearParams.put("code", code);
        clearParams.put("id", produitId);
        clearParams.put("userId", uid);
        namedJdbc.update(
            "UPDATE produits SET CODECLINIQUE = NULL, dateupdate = NOW(), userupdateid = :userId"
                + " WHERE CODECLINIQUE = :code AND id <> :id",
            clearParams);
      }

      Map<String, Object> params = new HashMap<>();
      params.put("id", produitId);
      params.put("code", code);
      params.put("userId", uid);
      namedJdbc.update(
          "UPDATE produits SET CODECLINIQUE = :code, dateupdate = NOW(), userupdateid = :userId WHERE id = :id",
          params);

      log.info("Produit {} lié à CODECLINIQUE={}", produitId, code);
      return loadErpById(produitId);
    } catch (NotFoundException ex) {
      throw ex;
    } catch (DataAccessException ex) {
      throw sqlFailure("link", ex);
    }
  }

  /** Pas de @Transactional ici : chaque succès/échec est isolé via self.link(). */
  public BatchLinkResultResponse batchLink(BatchLinkCodeCliniqueRequest request, Long userId) {
    List<BatchLinkResultResponse.Failure> failures = new ArrayList<>();
    List<ProduitFusionItemResponse> linked = new ArrayList<>();
    Set<String> codesInBatch = new HashSet<>();
    Set<Long> idsInBatch = new HashSet<>();

    List<BatchLinkCodeCliniqueRequest.Item> items =
        request.getLinks() != null ? request.getLinks() : List.of();
    ProduitFusionService linker = self != null ? self : this;

    for (BatchLinkCodeCliniqueRequest.Item item : items) {
      Long id = item.getProduitId();
      String code = item.getCodeClinique() == null ? null : item.getCodeClinique().trim();
      if (id == null || code == null || code.isBlank()) {
        failures.add(BatchLinkResultResponse.Failure.builder()
            .produitId(id)
            .codeClinique(code)
            .error("produitId / codeClinique manquant")
            .build());
        continue;
      }
      if (!idsInBatch.add(id)) {
        failures.add(BatchLinkResultResponse.Failure.builder()
            .produitId(id)
            .codeClinique(code)
            .error("produitId dupliqué dans le lot")
            .build());
        continue;
      }
      if (!codesInBatch.add(code)) {
        failures.add(BatchLinkResultResponse.Failure.builder()
            .produitId(id)
            .codeClinique(code)
            .error("codeClinique dupliqué dans le lot")
            .build());
        continue;
      }
      try {
        linked.add(linker.link(id, code, userId));
      } catch (Exception ex) {
        failures.add(BatchLinkResultResponse.Failure.builder()
            .produitId(id)
            .codeClinique(code)
            .error(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
            .build());
      }
    }

    return BatchLinkResultResponse.builder()
        .requested(items.size())
        .successCount(linked.size())
        .failureCount(failures.size())
        .linked(linked)
        .failures(failures)
        .build();
  }

  private Map<String, Object> cliniquePage(
      List<CliniqueProduit> items,
      long total,
      int page,
      int size,
      boolean available,
      int usedCount) {
    Map<String, Object> result = new HashMap<>();
    result.put("content", items);
    result.put("totalElements", total);
    result.put("page", page);
    result.put("size", size);
    result.put("available", available);
    result.put("usedInErp", usedCount);
    return result;
  }

  private Set<String> loadUsedCodeClinique() {
    try {
      List<String> codes = jdbc.query(
          "SELECT TRIM(CODECLINIQUE) FROM produits"
              + " WHERE CODECLINIQUE IS NOT NULL AND TRIM(CODECLINIQUE) <> ''",
          (rs, i) -> rs.getString(1));
      Set<String> used = new HashSet<>();
      for (String c : codes) {
        if (c != null && !c.isBlank()) {
          used.add(c.trim());
        }
      }
      return used;
    } catch (DataAccessException ex) {
      log.warn("loadUsedCodeClinique: {}", ex.getMessage());
      return Set.of();
    }
  }

  private List<ProduitFusionItemResponse> loadUnlinkedErp(int max) {
    int safe = Math.max(1, Math.min(max, MATCH_ERP_MAX));
    try {
      return jdbc.query(
          "SELECT p.id, p.codebarre, p.nomcommercial, p.nomscientifique, p.prixachat, p.CODECLINIQUE,"
              + " f.designation AS forme, d.designation AS dosage,"
              + " c.designation AS conditionnement, ct.designation AS categorie"
              + " FROM produits p"
              + " LEFT JOIN formes f ON p.fkForme = f.id"
              + " LEFT JOIN dosages d ON p.fkDosage = d.id"
              + " LEFT JOIN conditionnements c ON p.fkConditionnement = c.id"
              + " LEFT JOIN categorie_produit ct ON p.fkCategorie = ct.id"
              + " WHERE p.CODECLINIQUE IS NULL OR TRIM(p.CODECLINIQUE) = ''"
              + " ORDER BY p.nomcommercial ASC"
              + " LIMIT "
              + safe,
          this::mapErpRow);
    } catch (DataAccessException ex) {
      throw sqlFailure("loadUnlinkedErp", ex);
    }
  }

  private List<CliniqueProduit> loadCliniqueAll(int max) {
    List<CliniqueProduit> all = new ArrayList<>();
    int offset = 0;
    final int chunk = 500;
    while (all.size() < max) {
      int need = Math.min(chunk, max - all.size());
      List<CliniqueProduit> page = cliniqueLookupRepository.searchProduits(null, offset, need);
      if (page.isEmpty()) {
        break;
      }
      all.addAll(page);
      offset += page.size();
      if (page.size() < need) {
        break;
      }
    }
    return all;
  }

  private ProduitFusionItemResponse loadErpById(Long produitId) {
    return jdbc.query(
            "SELECT p.id, p.codebarre, p.nomcommercial, p.nomscientifique, p.prixachat, p.CODECLINIQUE,"
                + " f.designation AS forme, d.designation AS dosage,"
                + " c.designation AS conditionnement, ct.designation AS categorie"
                + " FROM produits p"
                + " LEFT JOIN formes f ON p.fkForme = f.id"
                + " LEFT JOIN dosages d ON p.fkDosage = d.id"
                + " LEFT JOIN conditionnements c ON p.fkConditionnement = c.id"
                + " LEFT JOIN categorie_produit ct ON p.fkCategorie = ct.id"
                + " WHERE p.id = ?",
            this::mapErpRow,
            produitId)
        .get(0);
  }

  private ProduitFusionItemResponse mapErpRow(ResultSet rs, int rowNum) throws SQLException {
    return ProduitFusionItemResponse.builder()
        .id(rs.getLong("id"))
        .codebarre(rs.getString("codebarre"))
        .nomcommercial(rs.getString("nomcommercial"))
        .nomscientifique(rs.getString("nomscientifique"))
        .forme(rs.getString("forme"))
        .dosage(rs.getString("dosage"))
        .conditionnement(rs.getString("conditionnement"))
        .categorie(rs.getString("categorie"))
        .prixachat(rs.getBigDecimal("prixachat"))
        .codeClinique(readCodeClinique(rs))
        .build();
  }

  private static String readCodeClinique(ResultSet rs) throws SQLException {
    try {
      return rs.getString("CODECLINIQUE");
    } catch (SQLException first) {
      try {
        return rs.getString("codeclinique");
      } catch (SQLException second) {
        return null;
      }
    }
  }

  private static BusinessException sqlFailure(String op, DataAccessException ex) {
    Throwable root = ex;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String detail = root.getMessage() != null ? root.getMessage() : ex.getMessage();
    log.error("Fusion ERP {} failed: {}", op, detail, ex);
    return new BusinessException(
        "Erreur SQL fusion ERP (" + op + "): " + detail
            + " — vérifier que la colonne produits.CODECLINIQUE existe sur la base du .env gateway.");
  }
}
