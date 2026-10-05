package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Normalisation + score de similarité pour propositions de liaison ERP ↔ CLINIQUE.
 */
final class FusionNameMatcher {

  private FusionNameMatcher() {}

  static String normalize(String raw) {
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

  static Set<String> tokens(String normalized) {
    if (normalized.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(normalized.split(" "))
        .filter(t -> t.length() >= 2)
        .collect(Collectors.toCollection(HashSet::new));
  }

  /**
   * Score 0..1 entre deux libellés normalisés.
   */
  static double scoreLabels(String aRaw, String bRaw) {
    String a = normalize(aRaw);
    String b = normalize(bRaw);
    if (a.isEmpty() || b.isEmpty()) {
      return 0.0;
    }
    if (a.equals(b)) {
      return 1.0;
    }
    if (a.contains(b) || b.contains(a)) {
      int min = Math.min(a.length(), b.length());
      int max = Math.max(a.length(), b.length());
      return 0.88 + 0.08 * ((double) min / max);
    }
    Set<String> ta = tokens(a);
    Set<String> tb = tokens(b);
    if (ta.isEmpty() || tb.isEmpty()) {
      return 0.0;
    }
    int inter = 0;
    for (String t : ta) {
      if (tb.contains(t)) {
        inter++;
      }
    }
    if (inter == 0) {
      return 0.0;
    }
    double jaccard = (double) inter / (ta.size() + tb.size() - inter);
    double coverage = (double) inter / Math.min(ta.size(), tb.size());
    return Math.max(jaccard, coverage * 0.92);
  }

  static Match bestMatch(
      String nomCommercial,
      String nomScientifique,
      String formeErp,
      String dosageErp,
      String designation,
      String libelle,
      String formeCli,
      String dosageCli) {

    double best = 0.0;
    String reason = "";

    double s1 = scoreLabels(nomCommercial, designation);
    if (s1 > best) {
      best = s1;
      reason = "nomcommercial↔designation";
    }
    double s2 = scoreLabels(nomCommercial, libelle);
    if (s2 > best) {
      best = s2;
      reason = "nomcommercial↔libelle";
    }
    double s3 = scoreLabels(nomScientifique, designation);
    if (s3 > best) {
      best = s3;
      reason = "nomscientifique↔designation";
    }
    double s4 = scoreLabels(nomScientifique, libelle);
    if (s4 > best) {
      best = s4;
      reason = "nomscientifique↔libelle";
    }

    String nf = normalize(formeErp);
    String nd = normalize(dosageErp);
    String cf = normalize(formeCli);
    String cd = normalize(dosageCli);
    double boost = 0.0;
    if (!nf.isEmpty() && nf.equals(cf)) {
      boost += 0.04;
    }
    if (!nd.isEmpty() && nd.equals(cd)) {
      boost += 0.04;
    }
    best = Math.min(1.0, best + boost);
    if (boost > 0 && best > 0) {
      reason = reason + "+forme/dosage";
    }
    return new Match(best, reason);
  }

  record Match(double score, String reason) {}
}
