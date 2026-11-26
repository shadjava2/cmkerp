package cd.shad.erp.cmk.cmkerp.gateway.facturation;

/**
 * Calcul déterministe de la PEC à partir de la société et du titulaire.
 * Règle métier : titulaire → LIBELLE_SOCIETE ; dépendant → LIBELLE_SOCIETE/NOM_TITULAIRE.
 */
public final class AffiliationCoverageCalculator {

  private AffiliationCoverageCalculator() {}

  public static boolean isPrincipal(String relation) {
    if (relation == null || relation.isBlank()) {
      return true;
    }
    return "A".equalsIgnoreCase(relation.trim());
  }

  /**
   * @return PEC, ou {@code null} si incomplete (société absente, ou titulaire requis manquant)
   */
  public static String computePec(String societeLibelle, String relation, String titulaireNom) {
    String society = normalizeLabel(societeLibelle);
    if (society == null) {
      return null;
    }
    if (isPrincipal(relation)) {
      return truncate(society, 75);
    }
    String nom = normalizeLabel(titulaireNom);
    if (nom == null) {
      return null;
    }
    return truncate(society + "/" + nom, 75);
  }

  public static String normalizeLabel(String raw) {
    if (raw == null) {
      return null;
    }
    String text = raw.trim().replaceAll("\\s+", " ");
    return text.isEmpty() ? null : text;
  }

  private static String truncate(String value, int max) {
    if (value.length() <= max) {
      return value;
    }
    return value.substring(0, max);
  }
}
