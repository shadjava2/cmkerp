package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

public final class WhatsAppConfigHelper {

  private WhatsAppConfigHelper() {}

  public static boolean isSecretConfigured(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String trimmed = value.trim();
    return !"CHANGEZ-MOI".equalsIgnoreCase(trimmed) && !trimmed.contains("XXXX");
  }

  public static String statusHint(boolean enabled, boolean configured) {
    if (!enabled) {
      return "WhatsApp OFF : mettez whatsapp.enabled: true dans application-dev-secrets.yml puis redémarrez.";
    }
    if (!configured) {
      return "Token Meta ou Phone Number ID manquant (remplacez CHANGEZ-MOI dans application-dev-secrets.yml).";
    }
    return "WhatsApp prêt — configurez le webhook Meta et ajoutez des numéros autorisés ci-dessous.";
  }
}
