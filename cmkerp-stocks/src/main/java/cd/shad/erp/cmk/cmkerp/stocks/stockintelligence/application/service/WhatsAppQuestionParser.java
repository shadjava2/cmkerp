package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extraction heuristique des termes produit depuis une question WhatsApp (sans appel IA).
 */
final class WhatsAppQuestionParser {

  private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");
  private static final Set<String> STOP_WORDS = Set.of(
      "quel", "quelle", "quels", "quelles", "est", "etat", "stock", "stocks", "du", "de", "des",
      "le", "la", "les", "un", "une", "pour", "produit", "produits", "pharmacie", "pharmacies",
      "centrale", "centrales", "cmk", "bonjour", "salut", "merci", "combien", "reste", "restent",
      "il", "y", "a", "t", "nous", "avez", "vous", "mon", "mes", "notre", "dans", "sur", "avec",
      "sans", "et", "ou", "que", "qui", "donne", "donner", "moi", "svp", "stp", "please", "the",
      "status", "niveau", "disponible", "disponibilite", "quantite", "qte", "situation");

  private WhatsAppQuestionParser() {}

  static List<String> extractSearchTerms(String question) {
    if (question == null || question.isBlank()) {
      return List.of();
    }
    String normalized = normalize(question);
    String[] tokens = NON_WORD.split(normalized);
    List<String> terms = new ArrayList<>();
    for (String token : tokens) {
      if (token.length() < 3 || STOP_WORDS.contains(token)) {
        continue;
      }
      if (!terms.contains(token)) {
        terms.add(token);
      }
    }
    terms.sort(Comparator.comparingInt(String::length).reversed());
    return terms.size() > 3 ? terms.subList(0, 3) : terms;
  }

  static boolean isGeneralStockQuestion(String question) {
    if (question == null || question.isBlank()) {
      return true;
    }
    String q = normalize(question);
    if (extractSearchTerms(question).isEmpty()) {
      return true;
    }
    return q.contains("rupture") || q.contains("bilan") || q.contains("resume") || q.contains("global")
        || q.contains("synthese") || q.contains("combien") && q.contains("produit");
  }

  private static String normalize(String text) {
    String n = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
    return n.trim();
  }
}
