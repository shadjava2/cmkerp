package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalogue central path API → permission(s) requises (anyOf).
 *
 * <p>Refuse par défaut tout endpoint métier non listé (hors whitelist publique).
 */
public final class ApiPermissionCatalog {

  public record Rule(String method, String pathPrefix, String... anyOfPermissions) {}

  private static final List<Rule> RULES = new ArrayList<>();

  static {
    // --- Stock / mouvements ---
    rule("GET", "/api/v1/stocks", "MODULE_STOCK", "OUVRIR_MODULE_STOCK", "STOCK_VIEW");
    rule("*", "/api/v1/stocks/products", "STOCK_PRODUIT_VIEW", "MODULE_STOCK", "OUVRIR_MODULE_STOCK");
    rule("POST", "/api/v1/stocks/products", "STOCK_PRODUIT_CREATE", "MODULE_STOCK");
    rule("PUT", "/api/v1/stocks/products", "STOCK_PRODUIT_UPDATE", "MODULE_STOCK");
    rule("DELETE", "/api/v1/stocks/products", "STOCK_PRODUIT_DELETE", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/produit-fusion", "STOCK_PRODUIT_FUSION", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/prix-ajustement", "STOCK_PRIX_AJUSTER", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/facturation-sync", "STOCK_FACTURATION_SYNC", "MODULE_STOCK");
    rule("GET", "/api/v1/stocks/transferts", "STOCK_TRANSFERT_VIEW", "MODULE_STOCK");
    rule("POST", "/api/v1/stocks/transferts", "STOCK_TRANSFERT_CREATE", "MODULE_STOCK");
    rule("PUT", "/api/v1/stocks/transferts", "STOCK_TRANSFERT_VALIDATE", "MODULE_STOCK");
    rule("GET", "/api/v1/stocks/requisitions", "STOCK_REQUISITION_VIEW", "MODULE_STOCK");
    rule("POST", "/api/v1/stocks/requisitions", "STOCK_REQUISITION_CREATE", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/transferts-internes", "STOCK_TRANSFERT_INTERNE", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/receptions-transferts-internes", "STOCK_RECEPTION_TI", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/stocks-disponibles", "STOCK_VIEW", "MODULE_STOCK", "OUVRIR_MODULE_STOCK");
    rule("*", "/api/v1/stocks/dashboard", "STOCK_DASHBOARD_VIEW", "MODULE_STOCK", "OUVRIR_MODULE_STOCK");
    rule("*", "/api/v1/stocks/reports", "STOCK_REPORT_EXPORT", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/perimable-alertes", "STOCK_PERIMABLE_VIEW", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/ordonnances", "STOCK_ORDONNANCE_VIEW", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/point-appel", "STOCK_POINT_APPEL", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/fournisseurs", "STOCK_FOURNISSEUR_VIEW", "MODULE_STOCK");
    rule("*", "/api/v1/stocks/references", "STOCK_VIEW", "MODULE_STOCK", "OUVRIR_MODULE_STOCK");
    rule("*", "/api/v1/stocks", "MODULE_STOCK", "OUVRIR_MODULE_STOCK", "STOCK_VIEW");

    rule("GET", "/api/v1/approvisionnements", "STOCK_APPROV_VIEW", "MODULE_STOCK");
    rule("POST", "/api/v1/approvisionnements", "STOCK_APPROV_CREATE", "MODULE_STOCK");
    rule("PUT", "/api/v1/approvisionnements", "STOCK_APPROV_UPDATE", "MODULE_STOCK");
    rule("DELETE", "/api/v1/approvisionnements", "STOCK_APPROV_CANCEL", "MODULE_STOCK");
    rule("*", "/api/v1/approvisionnements", "STOCK_APPROV_VIEW", "MODULE_STOCK");

    rule("GET", "/api/v1/inventaires", "STOCK_INVENTAIRE_VIEW", "MODULE_STOCK");
    rule("POST", "/api/v1/inventaires", "STOCK_INVENTAIRE_CREATE", "MODULE_STOCK");
    rule("*", "/api/v1/inventaires", "STOCK_INVENTAIRE_VIEW", "MODULE_STOCK");

    rule("GET", "/api/v1/ventes", "STOCK_VENTE_VIEW", "MODULE_STOCK", "OUVRIR_MODULE_CAISSE");
    rule("POST", "/api/v1/ventes", "STOCK_VENTE_CREATE", "MODULE_STOCK", "OUVRIR_MODULE_CAISSE");
    rule("*", "/api/v1/ventes", "STOCK_VENTE_VIEW", "MODULE_STOCK");

    rule("GET", "/api/v1/commandes", "CMD_CONSULTER", "MODULE_STOCK");
    rule("POST", "/api/v1/commandes", "CMD_CREER_COTATION", "MODULE_STOCK");
    rule("*", "/api/v1/commandes", "CMD_CONSULTER", "MODULE_STOCK");

    rule("*", "/api/v1/autorisations-operations", "STOCK_AUTORISATION_DECIDE", "MODULE_ADMINISTRATION",
        "OUVRIR_ADMINISTRATION", "ADMIN_ROLES_MANAGE");

    // --- Pilotage / stock intelligence ---
    rule("GET", "/api/v1/stock-intelligence", "MODULE_PILOTAGE_STOCK", "MODULE_STOCK",
        "OUVRIR_MODULE_STOCK", "PILOTAGE_VIEW");
    rule("POST", "/api/v1/stock-intelligence", "PILOTAGE_REPORT_RUN", "MODULE_PILOTAGE_STOCK",
        "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION");
    rule("*", "/api/v1/stock-intelligence", "MODULE_PILOTAGE_STOCK", "MODULE_STOCK",
        "OUVRIR_MODULE_STOCK");

    // --- GMAO ---
    rule("GET", "/api/v1/gmao", "MODULE_GMAO", "OUVRIR_MODULE_GMAO", "GMAO_VIEW");
    rule("POST", "/api/v1/gmao/equipements", "GMAO_EQUIPEMENT_CREATE", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");
    rule("PUT", "/api/v1/gmao/equipements", "GMAO_EQUIPEMENT_UPDATE", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");
    rule("DELETE", "/api/v1/gmao/equipements", "GMAO_EQUIPEMENT_DELETE", "MODULE_GMAO");
    rule("*", "/api/v1/gmao/equipements", "GMAO_EQUIPEMENT_VIEW", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");
    rule("POST", "/api/v1/gmao/interventions", "GMAO_INTERVENTION_CREATE", "MODULE_GMAO");
    rule("*", "/api/v1/gmao/interventions", "GMAO_INTERVENTION_VIEW", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");
    rule("*", "/api/v1/gmao/plans-preventifs", "GMAO_PLAN_VIEW", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");
    rule("*", "/api/v1/gmao/inventaires", "GMAO_INVENTAIRE_VIEW", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");
    rule("*", "/api/v1/gmao/dashboard", "GMAO_VIEW", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");
    rule("*", "/api/v1/gmao", "MODULE_GMAO", "OUVRIR_MODULE_GMAO");

    // --- DMI / consultations / cardex ---
    rule("GET", "/api/v1/consultations", "CONSULTER_CONSULTATION", "DMI_CABINET_PATIENT_VIEW",
        "SIDEBAR_CABINET", "MODULE_DMI");
    rule("POST", "/api/v1/consultations", "CREER_CONSULTATION", "DMI_CABINET_CONSULTATION_CREATE",
        "DMI_CABINET_PRISE_EN_CHARGE_CREATE", "SIDEBAR_CABINET", "MODULE_DMI");
    rule("PUT", "/api/v1/consultations", "MODIFIER_CONSULTATION", "DMI_CABINET_CONSULTATION_CREATE",
        "MODULE_DMI");
    rule("*", "/api/v1/consultations", "CONSULTER_CONSULTATION", "DMI_CABINET_PATIENT_VIEW",
        "DMI_CABINET_REGISTRE_CONSULTATIONS_VIEW", "MODULE_DMI", "SIDEBAR_CABINET");
    rule("POST", "/api/v1/cardex", "DMI_RECEPTION_PATIENT_CREATE", "DMI_ACCUEIL_NOUVEAU_DOSSIER",
        "DMI_CARDEX_NOUVEAU_DOSSIER", "SIDEBAR_RECEPTION", "SIDEBAR_CARDEX", "MODULE_DMI");
    rule("GET", "/api/v1/cardex", "DMI_CARDEX_VIEW", "DMI_CABINET_FICHE_OPEN",
        "DMI_CABINET_FILE_ACTIVE_VIEW", "SIDEBAR_CARDEX", "SIDEBAR_CABINET", "MODULE_DMI");
    rule("*", "/api/v1/mediline", "MODULE_DMI", "SIDEBAR_CARDEX", "DMI_CARDEX_VIEW",
        "DMI_RECEPTION_PATIENT_VIEW");
    rule("*", "/api/v1/cardex", "SIDEBAR_CARDEX", "DMI_CARDEX_VIEW", "MODULE_DMI");

    // --- Administration / sécurité ---
    rule("GET", "/api/v1/users", "ADMIN_USERS_VIEW", "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION",
        "view_users", "manage_users");
    rule("POST", "/api/v1/users", "ADMIN_USERS_CREATE", "MODULE_ADMINISTRATION", "manage_users");
    rule("PUT", "/api/v1/users", "ADMIN_USERS_UPDATE", "MODULE_ADMINISTRATION", "manage_users");
    rule("DELETE", "/api/v1/users", "ADMIN_USERS_UPDATE", "MODULE_ADMINISTRATION", "manage_users");
    rule("*", "/api/v1/users", "ADMIN_USERS_VIEW", "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION");

    rule("GET", "/api/v1/roles", "ADMIN_ROLES_MANAGE", "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION",
        "view_roles", "manage_roles");
    rule("*", "/api/v1/roles", "ADMIN_ROLES_MANAGE", "MODULE_ADMINISTRATION", "manage_roles");
    rule("GET", "/api/v1/permissions", "ADMIN_ROLES_MANAGE", "MODULE_ADMINISTRATION",
        "OUVRIR_ADMINISTRATION", "view_roles");
    rule("*", "/api/v1/rbac", "ADMIN_ROLES_MANAGE", "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION");
    rule("*", "/api/v1/admin", "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION");
    rule("*", "/api/v1/usage-events", "ADMIN_USAGE_VIEW", "MODULE_ADMINISTRATION",
        "OUVRIR_ADMINISTRATION");
    rule("*", "/api/v1/datasources", "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION",
        "ADMIN_ROLES_MANAGE");

    rule("*", "/api/v1/sites", "MODULE_ADMINISTRATION", "OUVRIR_ADMINISTRATION", "view_sites",
        "manage_sites");
    rule("*", "/api/v1/pharmacies", "MODULE_STOCK", "OUVRIR_MODULE_STOCK", "VIEW_PHARMACIES",
        "view_pharmacies", "manage_pharmacies");
    rule("*", "/api/v1/dashboard/pharmacies", "MODULE_STOCK", "OUVRIR_MODULE_STOCK");
    rule("*", "/api/v1/notifications", "MODULE_ADMINISTRATION", "view_notifications",
        "manage_notifications");
    rule("*", "/api/v1/inventory", "MODULE_STOCK", "OUVRIR_MODULE_STOCK", "STOCK_INVENTAIRE_VIEW");

    // --- POS ---
    rule("*", "/api/v1/pos", "OUVRIR_MODULE_CAISSE", "MODULE_STOCK", "STOCK_VENTE_VIEW");
  }

  private ApiPermissionCatalog() {}

  private static void rule(String method, String pathPrefix, String... anyOf) {
    RULES.add(new Rule(method, pathPrefix, anyOf));
  }

  public static boolean isPublicPath(String path) {
    if (path == null) {
      return false;
    }
    String p = normalize(path);
    return p.startsWith("/api/v1/auth")
        || p.equals("/api/v1/health")
        || p.startsWith("/api/v1/portail-fournisseur")
        || p.startsWith("/api/v1/stock-intelligence/whatsapp/webhook")
        || p.startsWith("/actuator/health")
        || p.startsWith("/actuator/info")
        || p.startsWith("/swagger-ui")
        || p.startsWith("/v3/api-docs")
        || p.startsWith("/debug/");
  }

  /**
   * Endpoints authentifiés sans permission métier (refresh droits, profil).
   */
  public static boolean isAuthenticatedOnly(String path, String method) {
    String p = normalize(path);
    String m = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
    return ("GET".equals(m) && (p.equals("/api/v1/auth/me")
        || p.equals("/api/v1/auth/session")
        || p.equals("/api/v1/auth/permissions")))
        || ("POST".equals(m) && (p.equals("/api/v1/auth/logout")
            || p.equals("/api/v1/auth/refresh")
            || p.equals("/api/v1/auth/change-password")
            || p.equals("/api/v1/auth/init-password")));
  }

  public static Optional<Rule> match(String method, String path) {
    String p = normalize(path);
    String m = method == null ? "*" : method.toUpperCase(Locale.ROOT);
    Rule best = null;
    int bestLen = -1;
    for (Rule rule : RULES) {
      if (!methodMatches(rule.method(), m)) {
        continue;
      }
      if (p.equals(rule.pathPrefix()) || p.startsWith(rule.pathPrefix() + "/")) {
        if (rule.pathPrefix().length() > bestLen) {
          best = rule;
          bestLen = rule.pathPrefix().length();
        }
      }
    }
    return Optional.ofNullable(best);
  }

  public static List<Rule> allRules() {
    return List.copyOf(RULES);
  }

  private static boolean methodMatches(String ruleMethod, String requestMethod) {
    return "*".equals(ruleMethod) || ruleMethod.equalsIgnoreCase(requestMethod);
  }

  private static String normalize(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }
    String p = path;
    int q = p.indexOf('?');
    if (q >= 0) {
      p = p.substring(0, q);
    }
    // Strip context path variants
    if (p.startsWith("/cmkerp-gateway")) {
      p = p.substring("/cmkerp-gateway".length());
    }
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    if (p.length() > 1 && p.endsWith("/")) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }
}
