package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.ConsumptionTrend;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceMultiSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductCategory;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductInsightDTO;

/**
 * Synthèse textuelle du rapport stock (sans OpenAI) — lecture direction / achats.
 */
@Service
public class StockIntelligenceReportNarrativeService {

  private static final DateTimeFormatter MONTH_FMT =
      DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

  public String moisEnCoursLabel() {
    return capitalize(MONTH_FMT.format(YearMonth.now()));
  }

  public String moisPrecedentLabel() {
    return capitalize(MONTH_FMT.format(YearMonth.now().minusMonths(1)));
  }

  public String buildHtmlNarrative(StockIntelligenceMultiSnapshotDTO snapshot) {
    var global = snapshot.resumeGlobal();
    Totals current = aggregateTotals(snapshot, true);
    Totals previous = aggregateTotals(snapshot, false);

    String sortiesEvolution = evolutionLabel(current.sorties, previous.sorties);
    String entreesEvolution = evolutionLabel(current.entrees, previous.entrees);

    int hausse = countByTrend(snapshot, ConsumptionTrend.HAUSSE);
    int baisse = countByTrend(snapshot, ConsumptionTrend.BAISSE);
    int sansActiviteCeMois = global.totalStockSansMouvement() + global.totalRuptureSansMouvement();

    StringBuilder sb = new StringBuilder();
    sb.append("<h3>Lecture rapide (sans IA)</h3>");
    sb.append("<p><strong>Période :</strong> ")
        .append(esc(moisEnCoursLabel()))
        .append(" — comparé à ")
        .append(esc(moisPrecedentLabel()))
        .append(" (du 1<sup>er</sup> au jour J).</p>");

    sb.append("<ul>");
    sb.append("<li><strong>").append(global.totalAvecMouvement())
        .append("</strong> produits avec au moins une <strong>entrée ou sortie ce mois</strong>.</li>");
    sb.append("<li><strong>").append(sansActiviteCeMois)
        .append("</strong> produits <strong>sans aucune activité ce mois</strong> (stock normal : ")
        .append(global.totalStockSansMouvement())
        .append(" · rupture ou sous seuil : ")
        .append(global.totalRuptureSansMouvement())
        .append(").</li>");
    sb.append("<li><strong>Entrées :</strong> ")
        .append(formatQty(current.entrees))
        .append(" ce mois vs ")
        .append(formatQty(previous.entrees))
        .append(" le mois passé (")
        .append(entreesEvolution)
        .append(").</li>");
    sb.append("<li><strong>Sorties :</strong> ")
        .append(formatQty(current.sorties))
        .append(" ce mois vs ")
        .append(formatQty(previous.sorties))
        .append(" le mois passé (")
        .append(sortiesEvolution)
        .append(").</li>");
    sb.append("<li><strong>Tendance sorties</strong> (produits actifs ce mois) : ")
        .append(hausse).append(" en hausse, ")
        .append(baisse).append(" en baisse.</li>");
    sb.append("</ul>");

    appendPharmacyAlerts(sb, snapshot);
    return sb.toString();
  }

  private void appendPharmacyAlerts(StringBuilder sb, StockIntelligenceMultiSnapshotDTO snapshot) {
    List<StockIntelligenceSnapshotDTO> ranked = snapshot.pharmacies().stream()
        .sorted(Comparator.comparingInt(
            (StockIntelligenceSnapshotDTO ph) -> ph.resume().totalRuptureSansMouvement()).reversed())
        .toList();

    boolean anyAlert = ranked.stream().anyMatch(ph -> ph.resume().totalRuptureSansMouvement() > 0
        || ph.resume().totalRuptures() > 10);
    if (!anyAlert) {
      return;
    }

    sb.append("<p><strong>Points d'attention :</strong></p><ul>");
    for (StockIntelligenceSnapshotDTO ph : ranked) {
      var r = ph.resume();
      if (r.totalRuptureSansMouvement() == 0 && r.totalRuptures() <= 10) {
        continue;
      }
      sb.append("<li><strong>").append(esc(ph.pharmacieLabel())).append("</strong> — ")
          .append(r.totalRuptures()).append(" rupture(s) au total, dont ")
          .append(r.totalRuptureSansMouvement())
          .append(" sans activité ce mois (à réapprovisionner en priorité).</li>");
    }
    sb.append("</ul>");
  }

  private int countByTrend(StockIntelligenceMultiSnapshotDTO snapshot, ConsumptionTrend trend) {
    return (int) allProducts(snapshot)
        .filter(p -> p.categorie() == StockProductCategory.AVEC_MOUVEMENT)
        .filter(p -> p.tendanceSorties() == trend)
        .count();
  }

  private Totals aggregateTotals(StockIntelligenceMultiSnapshotDTO snapshot, boolean currentMonth) {
    BigDecimal entrees = BigDecimal.ZERO;
    BigDecimal sorties = BigDecimal.ZERO;
    for (StockProductInsightDTO p : allProducts(snapshot).toList()) {
      if (currentMonth) {
        entrees = entrees.add(nullToZero(p.entreesMoisEnCours()));
        sorties = sorties.add(nullToZero(p.sortiesMoisEnCours()));
      } else {
        entrees = entrees.add(nullToZero(p.entreesMoisPrecedent()));
        sorties = sorties.add(nullToZero(p.sortiesMoisPrecedent()));
      }
    }
    return new Totals(entrees, sorties);
  }

  private Stream<StockProductInsightDTO> allProducts(StockIntelligenceMultiSnapshotDTO snapshot) {
    return snapshot.pharmacies().stream()
        .flatMap(ph -> ph.produitsParCategorie().values().stream())
        .flatMap(List::stream);
  }

  private String evolutionLabel(BigDecimal current, BigDecimal previous) {
    if (previous.compareTo(BigDecimal.ZERO) == 0) {
      return current.compareTo(BigDecimal.ZERO) > 0 ? "nouvelle activité" : "stable";
    }
    BigDecimal pct = current.subtract(previous)
        .multiply(BigDecimal.valueOf(100))
        .divide(previous, 1, RoundingMode.HALF_UP);
    if (pct.abs().compareTo(new BigDecimal("5")) < 0) {
      return "stable";
    }
    return (pct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + pct + " %";
  }

  private static String formatQty(BigDecimal value) {
    return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
  }

  private static BigDecimal nullToZero(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private static String esc(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private record Totals(BigDecimal entrees, BigDecimal sorties) {}
}
