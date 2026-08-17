package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.CentralPharmacyOptionDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ConsumptionTrend;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.PharmacySummaryRowDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceMultiSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceOverviewDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceSummaryDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockMovementRowDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockPharmacyOverviewDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductCategory;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductInsightDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.CentralPharmacyRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.StockMovementAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StockMovementAnalyticsService {

  private final StockMovementAnalyticsRepository repository;
  private final CentralPharmacyRepository centralPharmacyRepository;

  /**
   * Aperçu rapide : une ligne par pharmacie centrale (agrégation SQL GROUP BY ph.id).
   * {@code pharmacieId = null} → toutes les centrales (comportement Navicat).
   */
  public StockIntelligenceOverviewDTO buildOverview(Long pharmacieId) {
    List<PharmacySummaryRowDTO> rows = repository.findCentralPharmacySummaries(pharmacieId);
    log.info("Stock intelligence aperçu: {} pharmacie(s) centrale(s) (filtre pharmacieId={})",
        rows.size(), pharmacieId);

    LocalDateTime generatedAt = LocalDateTime.now();
    List<StockPharmacyOverviewDTO> pharmacies = rows.stream()
        .map(r -> new StockPharmacyOverviewDTO(
            r.idPharmacie(),
            r.pharmacie(),
            new StockIntelligenceSummaryDTO(
                r.totalAvecMouvement(),
                r.totalStockSansMouvement(),
                r.totalRuptureSansMouvement(),
                r.totalRuptures(),
                r.totalProduitsAnalyses())))
        .toList();

    StockIntelligenceSummaryDTO global = sumSummaries(
        pharmacies.stream().map(StockPharmacyOverviewDTO::resume).toList());

    int centralCount = centralPharmacyRepository.findCentralPharmaciesWithStock().size();
    String scope = pharmacieId != null ? "PHARMACY" : "ALL";

    return new StockIntelligenceOverviewDTO(
        generatedAt, scope, pharmacieId, centralCount, global, pharmacies);
  }

  public List<CentralPharmacyOptionDTO> listCentralPharmacies() {
    return centralPharmacyRepository.findCentralPharmaciesWithStock();
  }

  public StockIntelligenceOverviewDTO toOverview(StockIntelligenceMultiSnapshotDTO multi) {
    List<StockPharmacyOverviewDTO> pharmacies = multi.pharmacies().stream()
        .map(s -> new StockPharmacyOverviewDTO(s.pharmacieId(), s.pharmacieLabel(), s.resume()))
        .toList();
    int centralCount = centralPharmacyRepository.findCentralPharmaciesWithStock().size();
    return new StockIntelligenceOverviewDTO(
        multi.generatedAt(), "ALL", null, centralCount, multi.resumeGlobal(), pharmacies);
  }

  /** Snapshot complet par pharmacie (rapports, Excel, IA). */
  public StockIntelligenceMultiSnapshotDTO buildMultiSnapshot(Long pharmacieId) {
    List<StockMovementRowDTO> rows = repository.findCentralStockMovements(pharmacieId);
    Map<Long, List<StockMovementRowDTO>> byPharmacy = rows.stream()
        .collect(Collectors.groupingBy(
            StockMovementRowDTO::idPharmacie,
            LinkedHashMap::new,
            Collectors.toList()));

    log.info("Stock intelligence détail: {} lignes, {} pharmacie(s) (filtre pharmacieId={})",
        rows.size(), byPharmacy.size(), pharmacieId);

    LocalDateTime generatedAt = LocalDateTime.now();
    List<StockIntelligenceSnapshotDTO> snapshots = new ArrayList<>();
    for (List<StockMovementRowDTO> phRows : byPharmacy.values()) {
      snapshots.add(buildSnapshotFromRows(phRows, generatedAt));
    }
    snapshots.sort(Comparator.comparing(StockIntelligenceSnapshotDTO::pharmacieLabel, String.CASE_INSENSITIVE_ORDER));

    List<StockIntelligenceSummaryDTO> summaries = snapshots.stream()
        .map(StockIntelligenceSnapshotDTO::resume)
        .toList();
    StockIntelligenceSummaryDTO global = sumSummaries(summaries);

    return new StockIntelligenceMultiSnapshotDTO(generatedAt, global, snapshots);
  }

  /** Une seule pharmacie (filtre explicite). */
  public StockIntelligenceSnapshotDTO buildSnapshot(Long pharmacieId) {
    List<StockMovementRowDTO> rows = repository.findCentralStockMovements(pharmacieId);
    return buildSnapshotFromRows(rows, LocalDateTime.now());
  }

  /**
   * Recherche ERP ciblée pour le chat WhatsApp (ex. « état stock paracétamol »).
   */
  public List<StockProductInsightDTO> searchProductsForChat(String question, int limit) {
    List<String> terms = WhatsAppQuestionParser.extractSearchTerms(question);
    if (terms.isEmpty()) {
      return List.of();
    }
    return repository.searchProductsByKeywords(terms, limit).stream()
        .map(this::toInsight)
        .toList();
  }

  /** Contexte JSON compact pour OpenAI — question produit ou liste vide. */
  public Map<String, Object> buildWhatsAppChatContext(String question, List<StockProductInsightDTO> products) {
    Map<String, Object> ctx = new LinkedHashMap<>();
    ctx.put("question", question);
    ctx.put("moisEnCours", YearMonth.now().toString());
    ctx.put("moisPrecedent", YearMonth.now().minusMonths(1).toString());
    ctx.put("termesRecherche", WhatsAppQuestionParser.extractSearchTerms(question));
    ctx.put("produitsTrouves", products.size());
    List<Map<String, Object>> items = new ArrayList<>();
    for (StockProductInsightDTO p : products) {
      items.add(productToWhatsAppMap(p));
    }
    ctx.put("produits", items);
    return ctx;
  }

  /** Contexte global (ruptures, synthèse par pharmacie) sans produit ciblé. */
  public Map<String, Object> buildWhatsAppOverviewContext(String question) {
    StockIntelligenceOverviewDTO overview = buildOverview(null);
    Map<String, Object> ctx = new LinkedHashMap<>();
    ctx.put("question", question);
    ctx.put("mode", "SYNTHESE_GLOBALE");
    ctx.put("moisEnCours", YearMonth.now().toString());
    ctx.put("resumeGlobal", overview.resumeGlobal());
    List<Map<String, Object>> pharmacies = new ArrayList<>();
    for (StockPharmacyOverviewDTO ph : overview.pharmacies()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("pharmacie", ph.pharmacieLabel());
      row.put("resume", ph.resume());
      pharmacies.add(row);
    }
    ctx.put("pharmacies", pharmacies);
    return ctx;
  }

  private Map<String, Object> productToWhatsAppMap(StockProductInsightDTO p) {
    Map<String, Object> m = productToMap(p);
    m.put("pharmacie", p.pharmacie());
    m.put("nomScientifique", p.nomScientifique());
    m.put("forme", p.forme());
    m.put("dosage", p.dosage());
    m.put("conditionnement", p.conditionnement());
    m.put("categorie", p.categorie().name());
    m.put("entreesMoisPrecedent", p.entreesMoisPrecedent());
    return m;
  }

  private StockIntelligenceSnapshotDTO buildSnapshotFromRows(
      List<StockMovementRowDTO> rows,
      LocalDateTime generatedAt) {

    if (rows.isEmpty()) {
      return new StockIntelligenceSnapshotDTO(
          generatedAt,
          null,
          "Pharmacie centrale",
          emptyCategories(),
          new StockIntelligenceSummaryDTO(0, 0, 0, 0, 0));
    }

    Long pharmacieId = rows.get(0).idPharmacie();
    String pharmacieLabel = rows.get(0).pharmacie();

    List<StockProductInsightDTO> insights = rows.stream()
        .map(this::toInsight)
        .filter(this::keepRelevantProduct)
        .toList();

    Map<StockProductCategory, List<StockProductInsightDTO>> byCategory = insights.stream()
        .collect(Collectors.groupingBy(
            StockProductInsightDTO::categorie,
            () -> new EnumMap<>(StockProductCategory.class),
            Collectors.toList()));

    for (StockProductCategory cat : StockProductCategory.values()) {
      byCategory.putIfAbsent(cat, List.of());
    }

    int ruptures = (int) insights.stream().filter(StockProductInsightDTO::enRupture).count();

    StockIntelligenceSummaryDTO summary = new StockIntelligenceSummaryDTO(
        byCategory.get(StockProductCategory.AVEC_MOUVEMENT).size(),
        byCategory.get(StockProductCategory.STOCK_SANS_MOUVEMENT).size(),
        byCategory.get(StockProductCategory.RUPTURE_SANS_MOUVEMENT).size(),
        ruptures,
        insights.size());

    return new StockIntelligenceSnapshotDTO(
        generatedAt,
        pharmacieId,
        pharmacieLabel,
        byCategory,
        summary);
  }

  private static Map<StockProductCategory, List<StockProductInsightDTO>> emptyCategories() {
    Map<StockProductCategory, List<StockProductInsightDTO>> map = new EnumMap<>(StockProductCategory.class);
    for (StockProductCategory cat : StockProductCategory.values()) {
      map.put(cat, List.of());
    }
    return map;
  }

  static StockIntelligenceSummaryDTO sumSummaries(List<StockIntelligenceSummaryDTO> summaries) {
    return new StockIntelligenceSummaryDTO(
        summaries.stream().mapToInt(StockIntelligenceSummaryDTO::totalAvecMouvement).sum(),
        summaries.stream().mapToInt(StockIntelligenceSummaryDTO::totalStockSansMouvement).sum(),
        summaries.stream().mapToInt(StockIntelligenceSummaryDTO::totalRuptureSansMouvement).sum(),
        summaries.stream().mapToInt(StockIntelligenceSummaryDTO::totalRuptures).sum(),
        summaries.stream().mapToInt(StockIntelligenceSummaryDTO::totalProduitsAnalyses).sum());
  }

  private boolean keepRelevantProduct(StockProductInsightDTO insight) {
    return switch (insight.categorie()) {
      case AVEC_MOUVEMENT, STOCK_SANS_MOUVEMENT, RUPTURE_SANS_MOUVEMENT -> true;
    };
  }

  private StockProductInsightDTO toInsight(StockMovementRowDTO row) {
    boolean hasMovementCeMois = hasMovementMoisEnCours(row);
    boolean enRupture = isRupture(row.stockActuel(), row.seuilCritique());
    StockProductCategory categorie = categorize(hasMovementCeMois, enRupture, row.stockActuel());

    LocalDate dernierMouvement = maxDate(row.dateDerniereEntree(), row.dateDerniereSortie());
    ConsumptionTrend tendance = computeTrend(row);
    BigDecimal sortieJourMoyenne = computeDailyAverage(row.sortiesMoisEnCours());
    BigDecimal joursCouverture = computeCoverageDays(row.stockActuel(), sortieJourMoyenne);

    return new StockProductInsightDTO(
        row.idStock(),
        row.idPharmacie(),
        row.pharmacie(),
        row.nomCommercial(),
        row.nomScientifique(),
        row.forme(),
        row.dosage(),
        row.conditionnement(),
        row.stockActuel(),
        row.seuilCritique(),
        row.entreesMoisEnCours(),
        row.entreesMoisPrecedent(),
        row.sortiesMoisEnCours(),
        row.sortiesMoisPrecedent(),
        dernierMouvement,
        tendance,
        sortieJourMoyenne,
        joursCouverture,
        categorie,
        enRupture);
  }

  /** Activité sur le mois en cours uniquement (cohérent avec les colonnes du rapport). */
  private boolean hasMovementMoisEnCours(StockMovementRowDTO row) {
    return gtZero(row.entreesMoisEnCours()) || gtZero(row.sortiesMoisEnCours());
  }

  private StockProductCategory categorize(boolean hasMovementCeMois, boolean enRupture, BigDecimal stock) {
    if (hasMovementCeMois) {
      return StockProductCategory.AVEC_MOUVEMENT;
    }
    if (enRupture || stock.compareTo(BigDecimal.ZERO) <= 0) {
      return StockProductCategory.RUPTURE_SANS_MOUVEMENT;
    }
    return StockProductCategory.STOCK_SANS_MOUVEMENT;
  }

  private boolean isRupture(BigDecimal stock, BigDecimal seuilCritique) {
    if (stock.compareTo(BigDecimal.ZERO) <= 0) {
      return true;
    }
    return seuilCritique != null && stock.compareTo(seuilCritique) <= 0;
  }

  private ConsumptionTrend computeTrend(StockMovementRowDTO row) {
    if (!gtZero(row.sortiesMoisEnCours()) && !gtZero(row.sortiesMoisPrecedent())) {
      return ConsumptionTrend.SANS_SORTIE;
    }
    int dayOfMonth = Math.max(LocalDate.now().getDayOfMonth(), 1);
    int daysPrevMonth = YearMonth.now().minusMonths(1).lengthOfMonth();

    BigDecimal rateCurrent = safeDivide(row.sortiesMoisEnCours(), BigDecimal.valueOf(dayOfMonth));
    BigDecimal ratePrevious = safeDivide(row.sortiesMoisPrecedent(), BigDecimal.valueOf(daysPrevMonth));

    if (ratePrevious.compareTo(BigDecimal.ZERO) == 0) {
      return rateCurrent.compareTo(BigDecimal.ZERO) > 0 ? ConsumptionTrend.HAUSSE : ConsumptionTrend.INCONNU;
    }

    BigDecimal ratio = safeDivide(rateCurrent, ratePrevious);
    if (ratio.compareTo(new BigDecimal("1.15")) >= 0) {
      return ConsumptionTrend.HAUSSE;
    }
    if (ratio.compareTo(new BigDecimal("0.85")) <= 0) {
      return ConsumptionTrend.BAISSE;
    }
    return ConsumptionTrend.STABLE;
  }

  private BigDecimal computeDailyAverage(BigDecimal sortiesMoisEnCours) {
    int dayOfMonth = Math.max(LocalDate.now().getDayOfMonth(), 1);
    return safeDivide(sortiesMoisEnCours, BigDecimal.valueOf(dayOfMonth));
  }

  private BigDecimal computeCoverageDays(BigDecimal stock, BigDecimal dailyOut) {
    if (dailyOut.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return safeDivide(stock, dailyOut);
  }

  private BigDecimal safeDivide(BigDecimal num, BigDecimal den) {
    if (den == null || den.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return num.divide(den, 2, RoundingMode.HALF_UP);
  }

  private boolean gtZero(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
  }

  private LocalDate maxDate(LocalDate a, LocalDate b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return a.isAfter(b) ? a : b;
  }

  /** Résumé compact pour prompt OpenAI (multi-pharmacies). */
  public Map<String, Object> toCompactMap(StockIntelligenceMultiSnapshotDTO multi, int maxPerCategory) {
    Map<String, Object> root = new java.util.LinkedHashMap<>();
    root.put("generatedAt", multi.generatedAt().toString());
    root.put("moisEnCours", YearMonth.now().toString());
    root.put("moisPrecedent", YearMonth.now().minusMonths(1).toString());
    root.put("legendeCategories", Map.of(
        "AVEC_MOUVEMENT", "Entrée ou sortie sur le mois en cours",
        "STOCK_SANS_MOUVEMENT", "Aucune activité ce mois — stock disponible",
        "RUPTURE_SANS_MOUVEMENT", "Aucune activité ce mois — rupture ou sous seuil"));
    root.put("resumeGlobal", multi.resumeGlobal());

    List<Map<String, Object>> pharmacies = new ArrayList<>();
    for (StockIntelligenceSnapshotDTO snap : multi.pharmacies()) {
      Map<String, Object> ph = new java.util.LinkedHashMap<>();
      ph.put("pharmacie", snap.pharmacieLabel());
      ph.put("resume", snap.resume());
      Map<String, List<Map<String, Object>>> categories = new java.util.LinkedHashMap<>();
      snap.produitsParCategorie().forEach((cat, list) -> {
        List<Map<String, Object>> items = new ArrayList<>();
        list.stream().limit(maxPerCategory).forEach(p -> items.add(productToMap(p)));
        categories.put(cat.name(), items);
      });
      ph.put("produits", categories);
      pharmacies.add(ph);
    }
    root.put("pharmacies", pharmacies);
    return root;
  }

  private Map<String, Object> productToMap(StockProductInsightDTO p) {
    Map<String, Object> m = new java.util.LinkedHashMap<>();
    m.put("nom", p.nomCommercial());
    m.put("stock", p.stockActuel());
    m.put("seuilCritique", p.seuilCritique());
    m.put("entreesMois", p.entreesMoisEnCours());
    m.put("sortiesMois", p.sortiesMoisEnCours());
    m.put("sortiesMoisPrecedent", p.sortiesMoisPrecedent());
    m.put("dernierMouvement", p.dateDernierMouvement() != null ? p.dateDernierMouvement().toString() : null);
    m.put("tendance", p.tendanceSorties().name());
    m.put("joursCouverture", p.joursCouvertureEstimes());
    m.put("rupture", p.enRupture());
    return m;
  }
}
