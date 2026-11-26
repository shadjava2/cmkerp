package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Normalisation + score déterministe de similarité textuelle pour rapprochements référentiels.
 * Aligné sur {@code FusionNameMatcher} (produits) avec forme compacte pour C.M.K. ↔ CMK.
 */
public final class ReferentialLabelMatcher {

  private ReferentialLabelMatcher() {}

  /** Forme espacée : accents retirés, ponctuation → espaces. */
  public static String normalizeSpaced(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String n = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .replaceAll("\\s+", " ")
        .trim();
    return n;
  }

  /** Forme compacte : uniquement alphanumérique (C.M.K. → cmk, CITI GROUP → citigroup). */
  public static String normalizeCompact(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "");
  }

  public static Set<String> tokens(String spacedNormalized) {
    if (spacedNormalized == null || spacedNormalized.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(spacedNormalized.split(" "))
        .filter(t -> t.length() >= 2)
        .collect(Collectors.toCollection(HashSet::new));
  }

  /**
   * Score 0..100. Le 100 % est réservé à l'identité après normalisation sûre (compacte ou espacée).
   */
  public static int scorePercent(String leftRaw, String rightRaw) {
    String leftSpaced = normalizeSpaced(leftRaw);
    String rightSpaced = normalizeSpaced(rightRaw);
    String leftCompact = normalizeCompact(leftRaw);
    String rightCompact = normalizeCompact(rightRaw);
    if (leftCompact.isEmpty() || rightCompact.isEmpty()) {
      return 0;
    }
    if (leftCompact.equals(rightCompact) || leftSpaced.equals(rightSpaced)) {
      return 100;
    }

    double best = 0.0;

    if (leftCompact.contains(rightCompact) || rightCompact.contains(leftCompact)) {
      int min = Math.min(leftCompact.length(), rightCompact.length());
      int max = Math.max(leftCompact.length(), rightCompact.length());
      best = Math.max(best, 0.88 + 0.08 * ((double) min / max));
    }
    if (!leftSpaced.isEmpty() && !rightSpaced.isEmpty()
        && (leftSpaced.contains(rightSpaced) || rightSpaced.contains(leftSpaced))) {
      int min = Math.min(leftSpaced.length(), rightSpaced.length());
      int max = Math.max(leftSpaced.length(), rightSpaced.length());
      best = Math.max(best, 0.86 + 0.08 * ((double) min / max));
    }

    Set<String> ta = tokens(leftSpaced);
    Set<String> tb = tokens(rightSpaced);
    if (!ta.isEmpty() && !tb.isEmpty()) {
      int inter = 0;
      for (String t : ta) {
        if (tb.contains(t)) {
          inter++;
        }
      }
      if (inter > 0) {
        double jaccard = (double) inter / (ta.size() + tb.size() - inter);
        double coverage = (double) inter / Math.min(ta.size(), tb.size());
        best = Math.max(best, Math.max(jaccard, coverage * 0.92));
      }
    }

    double lev = levenshteinRatio(leftCompact, rightCompact);
    if (lev >= 0.75) {
      best = Math.max(best, lev * 0.97);
    }

    if (best >= 0.995) {
      // Ne jamais forcer 100 % hors identité exacte.
      best = 0.99;
    }
    int pct = (int) Math.round(best * 100.0);
    return Math.max(0, Math.min(99, pct));
  }

  public static String levelOf(int scorePercent) {
    if (scorePercent >= 100) {
      return "EXACT";
    }
    if (scorePercent >= 90) {
      return "VERY_PROBABLE";
    }
    if (scorePercent >= 80) {
      return "PROBABLE";
    }
    if (scorePercent >= 60) {
      return "UNCERTAIN";
    }
    if (scorePercent >= 1) {
      return "WEAK";
    }
    return "NONE";
  }

  public static boolean levelMatchesFilter(String filter, int scorePercent, boolean ambiguous) {
    if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter)) {
      return true;
    }
    String f = filter.trim().toUpperCase(Locale.ROOT);
    if ("AMBIGUOUS".equals(f)) {
      return ambiguous;
    }
    if ("MATCHED".equals(f)) {
      return false;
    }
    return levelOf(scorePercent).equals(f)
        || ("BELOW_80".equals(f) && scorePercent > 0 && scorePercent < 80)
        || ("NONE".equals(f) && scorePercent <= 0);
  }

  private static double levenshteinRatio(String a, String b) {
    if (a.equals(b)) {
      return 1.0;
    }
    int max = Math.max(a.length(), b.length());
    if (max == 0) {
      return 1.0;
    }
    if (max > 64) {
      // Évite le coût O(n²) sur libellés très longs.
      return 0.0;
    }
    int dist = levenshtein(a, b);
    return 1.0 - ((double) dist / max);
  }

  private static int levenshtein(String a, String b) {
    int[] prev = new int[b.length() + 1];
    int[] cur = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      prev[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      cur[0] = i;
      char ca = a.charAt(i - 1);
      for (int j = 1; j <= b.length(); j++) {
        int cost = ca == b.charAt(j - 1) ? 0 : 1;
        cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
      }
      int[] tmp = prev;
      prev = cur;
      cur = tmp;
    }
    return prev[b.length()];
  }
}
