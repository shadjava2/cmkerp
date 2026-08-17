package cd.shad.erp.cmk.cmkerp.sharedkernel.config;

/**
 * Classe centrale de gestion des chemins d'API versionnés.
 *
 * <p>
 * Cette classe centralise toutes les constantes de chemins d'API pour garantir :
 * <ul>
 * <li>La cohérence des URLs entre tous les contrôleurs</li>
 * <li>La facilité de migration vers de nouvelles versions (v2, v3, etc.)</li>
 * <li>L'évitement des chemins hardcodés dans les contrôleurs</li>
 * </ul>
 *
 * <p>
 * <strong>RÈGLE IMPORTANTE :</strong> Toute nouvelle route REST doit obligatoirement utiliser les
 * constantes définies dans cette classe. Ne jamais écrire de chemins comme {@code "/api/..."}
 * directement dans les contrôleurs.
 *
 * <p>
 * Exemple d'utilisation :
 *
 * <pre>
 * {
 *   &#64;code
 *   &#64;RestController
 *   &#64;RequestMapping(ApiPaths.SITES_BASE)
 *   public class SiteRestController {
 *     @GetMapping
 *     public ResponseEntity<List<SiteResponse>> findAll() {
 *       // GET /api/v1/sites
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * <strong>Version actuelle :</strong> v1 (stable pour front-end Next.js) <br>
 * <strong>Version future :</strong> v2 (préparée pour évolution sans casser v1)
 *
 *
 */
public final class ApiPaths {

  /**
   * Constructeur privé pour empêcher l'instanciation. Cette classe ne contient que des constantes
   * statiques.
   */
  private ApiPaths() {
    throw new UnsupportedOperationException("Cette classe ne doit pas être instanciée");
  }

  // ==========================================
  // VERSIONS D'API
  // ==========================================

  /**
   * Version actuelle de l'API (stable pour front-end). Tous les endpoints publics sont sous ce
   * préfixe.
   */
  public static final String API_V1 = "/api/v1";

  /**
   * Version future de l'API (préparée pour évolution). Actuellement non utilisée, mais prête pour
   * migration progressive.
   * <p>
   * <strong>Note :</strong> Quand v2 sera activée, on pourra maintenir v1 en parallèle pour assurer
   * la compatibilité avec les clients existants.
   */
  public static final String API_V2 = "/api/v2";

  // ==========================================
  // BASES PAR RESSOURCE (API v1)
  // ==========================================

  /**
   * Base pour les endpoints d'authentification.
   * <p>
   * Exemples :
   * <ul>
   * <li>POST /api/v1/auth/login</li>
   * <li>POST /api/v1/auth/refresh (si implémenté)</li>
   * </ul>
   */
  public static final String AUTH_BASE = API_V1 + "/auth";

  /**
   * Base pour les endpoints de gestion des utilisateurs.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/users</li>
   * <li>GET /api/v1/users/{id}</li>
   * <li>POST /api/v1/users</li>
   * <li>PUT /api/v1/users/{id}</li>
   * <li>DELETE /api/v1/users/{id}</li>
   * </ul>
   */
  public static final String USERS_BASE = API_V1 + "/users";

  /**
   * Base pour les endpoints de gestion des rôles.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/roles</li>
   * <li>GET /api/v1/roles/{id}</li>
   * <li>POST /api/v1/roles</li>
   * </ul>
   */
  public static final String ROLES_BASE = API_V1 + "/roles";

  /**
   * Base pour les endpoints de gestion des permissions.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/permissions</li>
   * <li>GET /api/v1/permissions/{id}</li>
   * </ul>
   */
  public static final String PERMISSIONS_BASE = API_V1 + "/permissions";

  /**
   * Base pour les endpoints de gestion des sites.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/sites</li>
   * <li>GET /api/v1/sites/{id}</li>
   * <li>POST /api/v1/sites</li>
   * <li>PUT /api/v1/sites/{id}</li>
   * <li>DELETE /api/v1/sites/{id}</li>
   * </ul>
   */
  public static final String SITES_BASE = API_V1 + "/sites";

  /**
   * Base pour les endpoints de gestion des pharmacies.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/pharmacies</li>
   * <li>GET /api/v1/pharmacies/{id}</li>
   * <li>POST /api/v1/pharmacies</li>
   * <li>PUT /api/v1/pharmacies/{id}</li>
   * <li>DELETE /api/v1/pharmacies/{id}</li>
   * </ul>
   */
  public static final String PHARMACIES_BASE = API_V1 + "/pharmacies";

  /**
   * Base pour les endpoints du dashboard des pharmacies.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/dashboard/pharmacies</li>
   * <li>GET /api/v1/dashboard/pharmacies/stock</li>
   * <li>GET /api/v1/dashboard/pharmacies/ruptures</li>
   * </ul>
   */
  public static final String PHARMACIES_DASHBOARD_BASE = API_V1 + "/dashboard/pharmacies";

  /**
   * Base pour les endpoints de gestion des notifications.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/notifications</li>
   * <li>GET /api/v1/notifications/{id}</li>
   * <li>POST /api/v1/notifications</li>
   * <li>PATCH /api/v1/notifications/{id}/read</li>
   * </ul>
   */
  public static final String NOTIFICATIONS_BASE = API_V1 + "/notifications";

  /**
   * Intelligence stock : snapshot, rapport IA, webhook WhatsApp.
   */
  public static final String STOCK_INTELLIGENCE_BASE = API_V1 + "/stock-intelligence";

  public static final String STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK = STOCK_INTELLIGENCE_BASE + "/whatsapp/webhook";

  /**
   * Base pour les endpoints de santé (health checks).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/health</li>
   * </ul>
   * <p>
   * <strong>Note :</strong> Les endpoints Actuator (/actuator/health) restent disponibles en
   * parallèle pour la compatibilité avec les outils de monitoring.
   */
  public static final String HEALTH_BASE = API_V1 + "/health";

  /**
   * Base pour les endpoints du module Inventory (Stock).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/inventory/dashboard/stats</li>
   * <li>GET /api/v1/inventory/dashboard/stock-plus-mouvementes</li>
   * <li>GET /api/v1/inventory/dashboard/stock-moins-mouvementes</li>
   * </ul>
   */
  public static final String INVENTORY_BASE = API_V1 + "/inventory";

  /**
   * Base pour les endpoints du module Stocks (cohérence avec backend cmkerp-stocks).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/stocks/products</li>
   * <li>GET /api/v1/stocks/products/{id}</li>
   * <li>GET /api/v1/stocks/products/{id}/detail</li>
   * <li>POST /api/v1/stocks/products</li>
   * <li>PUT /api/v1/stocks/products/{id}</li>
   * <li>DELETE /api/v1/stocks/products/{id}</li>
   * <li>GET /api/v1/stocks/references/formes</li>
   * <li>GET /api/v1/stocks/references/dosages</li>
   * <li>GET /api/v1/stocks/references/conditionnements</li>
   * <li>GET /api/v1/stocks/references/categories</li>
   * </ul>
   */
  public static final String STOCKS_BASE = API_V1 + "/stocks";

  /**
   * Base pour les endpoints du module Approvisionnements.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/approvisionnements</li>
   * <li>GET /api/v1/approvisionnements/{id}</li>
   * <li>POST /api/v1/approvisionnements</li>
   * <li>PUT /api/v1/approvisionnements/{id}</li>
   * <li>POST /api/v1/approvisionnements/{id}/valider</li>
   * <li>POST /api/v1/approvisionnements/{id}/annuler</li>
   * <li>GET /api/v1/approvisionnements/{id}/lignes</li>
   * </ul>
   */
  public static final String APPROVISIONNEMENTS_BASE = API_V1 + "/approvisionnements";

  /**
   * Base pour le module Commandes fournisseurs (cotation → BC → réception).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/commandes/dashboard</li>
   * <li>GET /api/v1/commandes/cotations</li>
   * <li>POST /api/v1/commandes/cotations/{id}/attribuer</li>
   * <li>GET /api/v1/commandes/bons-commande</li>
   * </ul>
   */
  public static final String COMMANDES_BASE = API_V1 + "/commandes";

  /**
   * Portail public fournisseur (accès par token, sans JWT).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/portail-fournisseur/{publicToken}</li>
   * <li>POST /api/v1/portail-fournisseur/{publicToken}/unlock</li>
   * </ul>
   */
  public static final String PORTAIL_FOURNISSEUR_BASE = API_V1 + "/portail-fournisseur";

  /**
   * Demandes d'autorisation centralisées (annulations tardives, etc.).
   */
  public static final String AUTORISATIONS_OPERATIONS_BASE = API_V1 + "/autorisations-operations";

  /**
   * Base pour les endpoints des Requisitions (dans le module Stocks).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/stocks/requisitions</li>
   * <li>GET /api/v1/stocks/requisitions/{id}</li>
   * </ul>
   */
  public static final String REQUISITIONS_BASE = STOCKS_BASE + "/requisitions";

  /**
   * Base pour les endpoints des Transferts de Stock (dans le module Stocks).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/stocks/transferts</li>
   * <li>GET /api/v1/stocks/transferts/{id}</li>
   * <li>POST /api/v1/stocks/transferts</li>
   * <li>POST /api/v1/stocks/transferts/{id}/annuler</li>
   * <li>GET /api/v1/stocks/transferts/{id}/lignes</li>
   * <li>PUT /api/v1/stocks/transferts/{id}/lignes/{ligneId}</li>
   * <li>DELETE /api/v1/stocks/transferts/{id}/lignes/{ligneId}</li>
   * </ul>
   */
  public static final String TRANSFERTS_BASE = STOCKS_BASE + "/transferts";

  /**
   * Base pour les endpoints des Transferts Internes (dans le module Stocks).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/stocks/transferts-internes</li>
   * <li>GET /api/v1/stocks/transferts-internes/{id}</li>
   * <li>POST /api/v1/stocks/transferts-internes</li>
   * <li>GET /api/v1/stocks/transferts-internes/destinations-eligibles</li>
   * <li>GET /api/v1/stocks/transferts-internes/{id}/lignes</li>
   * <li>POST /api/v1/stocks/transferts-internes/{id}/lignes</li>
   * <li>PUT /api/v1/stocks/transferts-internes/{id}/lignes/{ligneId}</li>
   * <li>DELETE /api/v1/stocks/transferts-internes/{id}/lignes/{ligneId}</li>
   * </ul>
   */
  public static final String TRANSFERTS_INTERNES_BASE = STOCKS_BASE + "/transferts-internes";

  /**
   * Base pour les endpoints des Réceptions de Transferts Internes (dans le module Stocks).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/stocks/receptions-transferts-internes</li>
   * <li>GET /api/v1/stocks/receptions-transferts-internes/{id}</li>
   * <li>POST /api/v1/stocks/receptions-transferts-internes</li>
   * <li>POST /api/v1/stocks/receptions-transferts-internes/{id}/receptionner</li>
   * <li>POST /api/v1/stocks/receptions-transferts-internes/{id}/annuler</li>
   * <li>GET /api/v1/stocks/receptions-transferts-internes/{id}/lignes</li>
   * <li>PUT /api/v1/stocks/receptions-transferts-internes/{id}/lignes/{ligneId}</li>
   * <li>DELETE /api/v1/stocks/receptions-transferts-internes/{id}/lignes/{ligneId}</li>
   * </ul>
   */
  public static final String RECEPTIONS_TRANSFERTS_INTERNES_BASE = STOCKS_BASE + "/receptions-transferts-internes";

  /**
   * Base pour les endpoints des Stocks Disponibles (dans le module Stocks).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/stocks/stocks-disponibles</li>
   * </ul>
   */
  public static final String STOCKS_DISPONIBLES_BASE = STOCKS_BASE + "/stocks-disponibles";

  /**
   * Base pour les endpoints des Ventes (Sorties pour usage).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/ventes</li>
   * <li>GET /api/v1/ventes/{id}</li>
   * <li>POST /api/v1/ventes</li>
   * <li>PUT /api/v1/ventes/{id}</li>
   * <li>POST /api/v1/ventes/{id}/valider</li>
   * <li>POST /api/v1/ventes/{id}/sortie-usage</li>
   * <li>POST /api/v1/ventes/{id}/annuler</li>
   * <li>GET /api/v1/ventes/{id}/lignes</li>
   * <li>POST /api/v1/ventes/{id}/lignes</li>
   * <li>PUT /api/v1/ventes/{id}/lignes/{ligneId}</li>
   * <li>DELETE /api/v1/ventes/{id}/lignes/{ligneId}</li>
   * </ul>
   */
  public static final String VENTES_BASE = API_V1 + "/ventes";

  // ==========================================
  // MODULE POS (Point of Sale)
  // ==========================================

  /**
   * Base pour les endpoints du module POS.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/pos/products</li>
   * <li>GET /api/v1/pos/products/{id}</li>
   * </ul>
   */
  public static final String POS_BASE = API_V1 + "/pos";

  /**
   * Base pour le dashboard POS (statistiques stock).
   */
  public static final String POS_DASHBOARD_BASE = POS_BASE + "/dashboard";

  /** Références produit (formes, dosages, etc.) — alias POS du module Stocks. */
  public static final String POS_REFERENCES_BASE = POS_BASE + "/references";

  /** Fournisseurs — alias POS. */
  public static final String POS_FOURNISSEURS_BASE = POS_BASE + "/fournisseurs";

  /** Alertes de péremption — alias POS. */
  public static final String POS_PERIMABLE_ALERTES_BASE = POS_BASE + "/perimable-alertes";

  /** Rapports produits/fournisseurs — alias POS. */
  public static final String POS_REPORTS_BASE = POS_BASE + "/reports";

  /** Réquisitions — alias POS. */
  public static final String POS_REQUISITIONS_BASE = POS_BASE + "/requisitions";

  /** Transferts de stock — alias POS. */
  public static final String POS_TRANSFERTS_BASE = POS_BASE + "/transferts";

  /** Stocks disponibles (remplacement) — alias POS. */
  public static final String POS_STOCKS_DISPONIBLES_BASE = POS_BASE + "/stocks-disponibles";

  /**
   * Base pour les endpoints des Transferts Internes (dans le module POS).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/pos/transferts-internes</li>
   * <li>GET /api/v1/pos/transferts-internes/{id}</li>
   * <li>POST /api/v1/pos/transferts-internes</li>
   * <li>GET /api/v1/pos/transferts-internes/destinations-eligibles</li>
   * <li>GET /api/v1/pos/transferts-internes/{id}/lignes</li>
   * <li>POST /api/v1/pos/transferts-internes/{id}/lignes</li>
   * <li>PUT /api/v1/pos/transferts-internes/{id}/lignes/{ligneId}</li>
   * <li>DELETE /api/v1/pos/transferts-internes/{id}/lignes/{ligneId}</li>
   * </ul>
   */
  public static final String POS_TRANSFERTS_INTERNES_BASE = POS_BASE + "/transferts-internes";

  /**
   * Base pour les endpoints des Réceptions de Transferts Internes (dans le module POS).
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/pos/receptions-transferts-internes</li>
   * <li>GET /api/v1/pos/receptions-transferts-internes/{id}</li>
   * <li>POST /api/v1/pos/receptions-transferts-internes</li>
   * <li>POST /api/v1/pos/receptions-transferts-internes/{id}/receptionner</li>
   * <li>POST /api/v1/pos/receptions-transferts-internes/{id}/annuler</li>
   * <li>GET /api/v1/pos/receptions-transferts-internes/{id}/lignes</li>
   * <li>PUT /api/v1/pos/receptions-transferts-internes/{id}/lignes/{ligneId}</li>
   * <li>DELETE /api/v1/pos/receptions-transferts-internes/{id}/lignes/{ligneId}</li>
   * </ul>
   */
  public static final String POS_RECEPTIONS_TRANSFERTS_INTERNES_BASE = POS_BASE + "/receptions-transferts-internes";

  // ==========================================
  // PATTERNS POUR SÉCURITÉ ET CORS
  // ==========================================

  /**
   * Pattern générique pour tous les endpoints de l'API v1. Utile pour la configuration de sécurité
   * et CORS.
   */
  public static final String API_V1_PATTERN = API_V1 + "/**";

  /**
   * Pattern pour les endpoints d'authentification (publics).
   */
  public static final String AUTH_PATTERN = AUTH_BASE + "/**";

  /**
   * Pattern pour les endpoints utilisateurs (authentifiés).
   */
  public static final String USERS_PATTERN = USERS_BASE + "/**";

  /**
   * Pattern pour les endpoints sites (authentifiés).
   */
  public static final String SITES_PATTERN = SITES_BASE + "/**";

  /**
   * Pattern pour les endpoints pharmacies (authentifiés).
   */
  public static final String PHARMACIES_PATTERN = PHARMACIES_BASE + "/**";

  /**
   * Pattern pour les endpoints notifications (authentifiés).
   */
  public static final String NOTIFICATIONS_PATTERN = NOTIFICATIONS_BASE + "/**";

  /**
   * Pattern pour les endpoints stocks (authentifiés).
   */
  public static final String STOCKS_PATTERN = STOCKS_BASE + "/**";

  /**
   * Pattern pour les endpoints approvisionnements (authentifiés).
   */
  public static final String APPROVISIONNEMENTS_PATTERN = APPROVISIONNEMENTS_BASE + "/**";

  /**
   * Pattern pour les endpoints commandes (authentifiés).
   */
  public static final String COMMANDES_PATTERN = COMMANDES_BASE + "/**";

  /**
   * Pattern pour le portail fournisseur (public).
   */
  public static final String PORTAIL_FOURNISSEUR_PATTERN = PORTAIL_FOURNISSEUR_BASE + "/**";

  /**
   * Pattern pour les endpoints ventes (authentifiés).
   */
  public static final String VENTES_PATTERN = VENTES_BASE + "/**";

  // ==========================================
  // INVENTAIRES
  // ==========================================

  /**
   * Base path pour les endpoints d'inventaires.
   * <p>
   * Endpoints disponibles :
   * <ul>
   * <li>GET /api/v1/inventaires</li>
   * <li>GET /api/v1/inventaires/{id}</li>
   * <li>POST /api/v1/inventaires</li>
   * <li>PUT /api/v1/inventaires/{id}</li>
   * <li>POST /api/v1/inventaires/{id}/terminer</li>
   * <li>POST /api/v1/inventaires/{id}/annuler</li>
   * <li>GET /api/v1/inventaires/{id}/report</li>
   * <li>GET /api/v1/inventaires/report</li>
   * <li>GET /api/v1/inventaires/{id}/lignes</li>
   * <li>PUT /api/v1/inventaires/{id}/lignes/{ligneId}</li>
   * </ul>
   */
  public static final String INVENTAIRES_BASE = API_V1 + "/inventaires";

  /**
   * Pattern pour les endpoints inventaires (authentifiés).
   */
  public static final String INVENTAIRES_PATTERN = INVENTAIRES_BASE + "/**";

  // ==========================================
  // MODULE GMAO (Gestion de Maintenance)
  // ==========================================

  /**
   * Base pour le module GMAO.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/gmao/equipements</li>
   * <li>GET /api/v1/gmao/interventions</li>
   * <li>GET /api/v1/gmao/plans-preventifs</li>
   * <li>GET /api/v1/gmao/dashboard/stats</li>
   * </ul>
   */
  public static final String GMAO_BASE = API_V1 + "/gmao";

  public static final String GMAO_EQUIPEMENTS_BASE = GMAO_BASE + "/equipements";

  public static final String GMAO_INTERVENTIONS_BASE = GMAO_BASE + "/interventions";

  public static final String GMAO_PLANS_BASE = GMAO_BASE + "/plans-preventifs";

  public static final String GMAO_DASHBOARD_BASE = GMAO_BASE + "/dashboard";

  public static final String GMAO_INVENTAIRES_BASE = GMAO_BASE + "/inventaires";

  public static final String GMAO_PATTERN = GMAO_BASE + "/**";

  /**
   * Base pour les endpoints d'analyse et d'administration.
   * <p>
   * Exemples :
   * <ul>
   * <li>GET /api/v1/admin/analysis/slow-queries</li>
   * <li>GET /api/v1/admin/analysis/index-recommendations</li>
   * </ul>
   */
  public static final String ADMIN_BASE = API_V1 + "/admin";

  /**
   * Pattern pour les endpoints admin (authentifiés, admin uniquement).
   */
  public static final String ADMIN_PATTERN = ADMIN_BASE + "/**";
}

