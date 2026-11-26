package cd.shad.erp.cmk.cmkerp.gateway.mediline;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Similarité d'identité patient (déterministe, explicable).
 * Normalise uniquement pour la comparaison — ne modifie jamais les données source.
 */
public final class PatientIdentityMatcher {

  public static final int WEIGHT_IDENTITY = 50;
  public static final int WEIGHT_DOB = 25;
  public static final int WEIGHT_PHONE = 10;
  public static final int WEIGHT_SEX = 5;
  public static final int WEIGHT_BIRTHPLACE = 5;
  public static final int WEIGHT_EMAIL = 5;

  public static final int THRESHOLD_IDENTICAL = 95;
  public static final int THRESHOLD_VERY_PROBABLE = 85;
  public static final int THRESHOLD_POSSIBLE = 70;

  private PatientIdentityMatcher() {}

  public record IdentityParts(
      String nom,
      String postnom,
      String prenom,
      String sexe,
      String dateNaissance,
      String telephone,
      String lieuNaissance,
      String email) {}

  public record MatchScore(
      int scorePercent,
      int completenessPercent,
      String riskBand,
      int identityScore,
      List<String> reasons) {}

  public static String normalizeSpaced(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  public static String normalizeCompact(String raw) {
    return normalizeSpaced(raw).replace(" ", "");
  }

  public static String normalizePhone(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String digits = raw.replaceAll("\\D+", "");
    if (digits.length() > 9) {
      digits = digits.substring(digits.length() - 9);
    }
    return digits;
  }

  public static String composeIdentity(String nom, String postnom, String prenom) {
    return Arrays.stream(new String[] {nom, postnom, prenom})
        .map(PatientIdentityMatcher::normalizeSpaced)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining(" "));
  }

  /** Toutes les permutations non vides des 3 champs d'identité. */
  public static List<String> identityPermutations(String nom, String postnom, String prenom) {
    List<String> parts = Arrays.stream(new String[] {nom, postnom, prenom})
        .map(PatientIdentityMatcher::normalizeSpaced)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toCollection(ArrayList::new));
    LinkedHashSet<String> out = new LinkedHashSet<>();
    if (parts.isEmpty()) {
      return List.of();
    }
    permute(parts, 0, out);
    // Aussi sans post-nom (saisie incomplète).
    if (parts.size() >= 2) {
      for (int i = 0; i < parts.size(); i++) {
        for (int j = 0; j < parts.size(); j++) {
          if (i != j) {
            out.add(parts.get(i) + " " + parts.get(j));
          }
        }
      }
    }
    return new ArrayList<>(out);
  }

  private static void permute(List<String> parts, int start, Set<String> out) {
    if (start == parts.size()) {
      out.add(String.join(" ", parts));
      return;
    }
    for (int i = start; i < parts.size(); i++) {
      swap(parts, start, i);
      permute(parts, start + 1, out);
      swap(parts, start, i);
    }
  }

  private static void swap(List<String> parts, int i, int j) {
    String tmp = parts.get(i);
    parts.set(i, parts.get(j));
    parts.set(j, tmp);
  }

  public static int textSimilarityPercent(String leftRaw, String rightRaw) {
    String left = normalizeSpaced(leftRaw);
    String right = normalizeSpaced(rightRaw);
    if (left.isEmpty() || right.isEmpty()) {
      return 0;
    }
    if (left.equals(right) || normalizeCompact(left).equals(normalizeCompact(right))) {
      return 100;
    }

    double best = 0.0;
    best = Math.max(best, tokenSortRatio(left, right));
    best = Math.max(best, tokenSetRatio(left, right));
    best = Math.max(best, jaroWinkler(normalizeCompact(left), normalizeCompact(right)));
    best = Math.max(best, levenshteinRatio(normalizeCompact(left), normalizeCompact(right)));

    // Contenance courte : plafonnée pour éviter faux positifs sur noms fréquents.
    String lc = normalizeCompact(left);
    String rc = normalizeCompact(right);
    if (lc.length() >= 4 && rc.length() >= 4 && (lc.contains(rc) || rc.contains(lc))) {
      int min = Math.min(lc.length(), rc.length());
      int max = Math.max(lc.length(), rc.length());
      double ratio = (double) min / max;
      if (ratio >= 0.72) {
        best = Math.max(best, 0.78 + 0.16 * ratio);
      }
    }

    if (best >= 0.995) {
      best = 0.99;
    }
    return Math.max(0, Math.min(99, (int) Math.round(best * 100.0)));
  }

  /** Meilleure similarité d'identité en tenant compte des inversions de champs. */
  public static int bestIdentityScore(IdentityParts left, IdentityParts right) {
    List<String> leftPerms = identityPermutations(left.nom(), left.postnom(), left.prenom());
    List<String> rightPerms = identityPermutations(right.nom(), right.postnom(), right.prenom());
    if (leftPerms.isEmpty() || rightPerms.isEmpty()) {
      return 0;
    }
    int best = 0;
    for (String l : leftPerms) {
      for (String r : rightPerms) {
        best = Math.max(best, textSimilarityPercent(l, r));
        if (best >= 100) {
          return 100;
        }
      }
    }
    return best;
  }

  public static MatchScore score(IdentityParts probe, IdentityParts candidate) {
    List<String> reasons = new ArrayList<>();
    int identityScore = bestIdentityScore(probe, candidate);
    int availableWeight = 0;
    double weighted = 0.0;

    availableWeight += WEIGHT_IDENTITY;
    weighted += identityScore * (WEIGHT_IDENTITY / 100.0);
    if (identityScore >= 95) {
      reasons.add("Identité fortement similaire");
    } else if (identityScore >= 85) {
      reasons.add("Identité similaire");
    } else if (identityScore >= 70) {
      reasons.add("Correspondance partielle d'identité");
    }

    String probeId = composeIdentity(probe.nom(), probe.postnom(), probe.prenom());
    String candId = composeIdentity(candidate.nom(), candidate.postnom(), candidate.prenom());
    if (!probeId.equals(candId) && identityScore >= 85) {
      String probeSorted = tokenSort(probeId);
      String candSorted = tokenSort(candId);
      if (probeSorted.equals(candSorted)) {
        reasons.add("Inversion des noms");
      } else if (isPrefixName(probe, candidate) || isPrefixName(candidate, probe)) {
        reasons.add("Prénom incomplet ou abrégé");
      } else {
        reasons.add("Variation orthographique possible");
      }
    }

    String probeDob = normalizeDob(probe.dateNaissance());
    String candDob = normalizeDob(candidate.dateNaissance());
    if (!probeDob.isEmpty() && !candDob.isEmpty()) {
      availableWeight += WEIGHT_DOB;
      int dobScore = dobSimilarity(probeDob, candDob, reasons);
      weighted += dobScore * (WEIGHT_DOB / 100.0);
    }

    String probePhone = normalizePhone(probe.telephone());
    String candPhone = normalizePhone(candidate.telephone());
    if (!probePhone.isEmpty() && !candPhone.isEmpty()) {
      availableWeight += WEIGHT_PHONE;
      if (probePhone.equals(candPhone)) {
        weighted += 100.0 * (WEIGHT_PHONE / 100.0);
        reasons.add("Téléphone identique");
      }
    }

    String probeSex = normalizeSex(probe.sexe());
    String candSex = normalizeSex(candidate.sexe());
    if (!probeSex.isEmpty() && !candSex.isEmpty()) {
      availableWeight += WEIGHT_SEX;
      if (probeSex.equals(candSex)) {
        weighted += 100.0 * (WEIGHT_SEX / 100.0);
      } else {
        reasons.add("Sexe différent");
      }
    }

    String probePlace = normalizeSpaced(probe.lieuNaissance());
    String candPlace = normalizeSpaced(candidate.lieuNaissance());
    if (!probePlace.isEmpty() && !candPlace.isEmpty()) {
      availableWeight += WEIGHT_BIRTHPLACE;
      int placeScore = textSimilarityPercent(probePlace, candPlace);
      weighted += placeScore * (WEIGHT_BIRTHPLACE / 100.0);
      if (placeScore >= 90) {
        reasons.add("Lieu de naissance identique");
      }
    }

    String probeEmail = normalizeSpaced(probe.email()).replace(" ", "");
    String candEmail = normalizeSpaced(candidate.email()).replace(" ", "");
    if (!probeEmail.isEmpty() && !candEmail.isEmpty()) {
      availableWeight += WEIGHT_EMAIL;
      if (probeEmail.equalsIgnoreCase(candEmail)) {
        weighted += 100.0 * (WEIGHT_EMAIL / 100.0);
        reasons.add("E-mail identique");
      }
    }

    int completeness = availableWeight;
    // Renormalisation sur critères disponibles uniquement.
    double score = availableWeight == 0 ? 0 : (weighted * 100.0) / availableWeight;
    // Pénalité si identité faible malgré d'autres indices (téléphone familial, etc.).
    if (identityScore < 60 && score > identityScore + 15) {
      score = identityScore + 15;
      reasons.add("Indices complémentaires insuffisants sans identité proche");
    }
    int pct = Math.max(0, Math.min(100, (int) Math.round(score)));
    if (pct >= 100 && identityScore < 100) {
      pct = 99;
    }
    return new MatchScore(pct, completeness, riskBand(pct), identityScore, dedupeReasons(reasons));
  }

  public static String riskBand(int scorePercent) {
    if (scorePercent >= THRESHOLD_IDENTICAL) {
      return "IDENTICAL_LIKELY";
    }
    if (scorePercent >= THRESHOLD_VERY_PROBABLE) {
      return "VERY_PROBABLE";
    }
    if (scorePercent >= THRESHOLD_POSSIBLE) {
      return "POSSIBLE";
    }
    if (scorePercent >= 1) {
      return "WEAK";
    }
    return "NONE";
  }

  public static String riskLabel(String band) {
    return switch (band == null ? "" : band) {
      case "IDENTICAL_LIKELY" -> "Identité potentiellement identique";
      case "VERY_PROBABLE" -> "Doublon très probable";
      case "POSSIBLE" -> "Correspondance possible";
      case "WEAK" -> "Correspondance faible";
      default -> "Aucune correspondance";
    };
  }

  private static boolean isPrefixName(IdentityParts shortId, IdentityParts longId) {
    String a = normalizeSpaced(shortId.prenom());
    String b = normalizeSpaced(longId.prenom());
    return !a.isEmpty() && !b.isEmpty() && b.startsWith(a) && a.length() + 2 <= b.length();
  }

  private static int dobSimilarity(String left, String right, List<String> reasons) {
    if (left.equals(right)) {
      reasons.add("Date de naissance identique");
      return 100;
    }
    try {
      LocalDate a = LocalDate.parse(left);
      LocalDate b = LocalDate.parse(right);
      if (a.getYear() == b.getYear() && a.getMonthValue() == b.getDayOfMonth()
          && a.getDayOfMonth() == b.getMonthValue()
          && a.getMonthValue() <= 12 && b.getMonthValue() <= 12) {
        reasons.add("Jour et mois de naissance éventuellement inversés");
        return 82;
      }
      if (a.getYear() == b.getYear() && a.getMonth() == b.getMonth()) {
        reasons.add("Même mois et année de naissance");
        return 70;
      }
      if (Math.abs(a.getYear() - b.getYear()) == 1) {
        reasons.add("Année de naissance légèrement différente");
        return 55;
      }
      reasons.add("Date de naissance différente");
      return 15;
    } catch (DateTimeParseException ex) {
      return left.equalsIgnoreCase(right) ? 100 : 20;
    }
  }

  private static String normalizeDob(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String t = raw.trim();
    if (t.length() >= 10) {
      return t.substring(0, 10);
    }
    return t;
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

  private static double tokenSortRatio(String left, String right) {
    return levenshteinRatio(tokenSort(left), tokenSort(right));
  }

  private static String tokenSort(String spaced) {
    return Arrays.stream(spaced.split(" "))
        .filter(s -> !s.isBlank())
        .sorted()
        .collect(Collectors.joining(" "));
  }

  private static double tokenSetRatio(String left, String right) {
    Set<String> a = tokens(left);
    Set<String> b = tokens(right);
    if (a.isEmpty() || b.isEmpty()) {
      return 0;
    }
    int inter = 0;
    for (String t : a) {
      if (b.contains(t)) {
        inter++;
      }
    }
    double jaccard = (double) inter / (a.size() + b.size() - inter);
    double coverage = (double) inter / Math.min(a.size(), b.size());
    return Math.max(jaccard, coverage * 0.94);
  }

  private static Set<String> tokens(String spaced) {
    return Arrays.stream(spaced.split(" "))
        .filter(t -> t.length() >= 2)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static double levenshteinRatio(String a, String b) {
    if (Objects.equals(a, b)) {
      return 1.0;
    }
    int max = Math.max(a.length(), b.length());
    if (max == 0) {
      return 1.0;
    }
    if (max > 64) {
      return 0.0;
    }
    return 1.0 - ((double) levenshtein(a, b) / max);
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

  /** Jaro-Winkler 0..1. */
  static double jaroWinkler(String s1, String s2) {
    if (s1.equals(s2)) {
      return 1.0;
    }
    if (s1.isEmpty() || s2.isEmpty()) {
      return 0.0;
    }
    int matchDistance = Math.max(s1.length(), s2.length()) / 2 - 1;
    if (matchDistance < 0) {
      matchDistance = 0;
    }
    boolean[] s1Matches = new boolean[s1.length()];
    boolean[] s2Matches = new boolean[s2.length()];
    int matches = 0;
    int transpositions = 0;
    for (int i = 0; i < s1.length(); i++) {
      int start = Math.max(0, i - matchDistance);
      int end = Math.min(i + matchDistance + 1, s2.length());
      for (int j = start; j < end; j++) {
        if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) {
          continue;
        }
        s1Matches[i] = true;
        s2Matches[j] = true;
        matches++;
        break;
      }
    }
    if (matches == 0) {
      return 0.0;
    }
    int k = 0;
    for (int i = 0; i < s1.length(); i++) {
      if (!s1Matches[i]) {
        continue;
      }
      while (!s2Matches[k]) {
        k++;
      }
      if (s1.charAt(i) != s2.charAt(k)) {
        transpositions++;
      }
      k++;
    }
    double m = matches;
    double jaro = (m / s1.length() + m / s2.length() + (m - transpositions / 2.0) / m) / 3.0;
    int prefix = 0;
    int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));
    while (prefix < maxPrefix && s1.charAt(prefix) == s2.charAt(prefix)) {
      prefix++;
    }
    return jaro + prefix * 0.1 * (1 - jaro);
  }

  private static List<String> dedupeReasons(List<String> reasons) {
    return reasons.stream().filter(Objects::nonNull).distinct().sorted(Comparator.naturalOrder()).toList();
  }
}
