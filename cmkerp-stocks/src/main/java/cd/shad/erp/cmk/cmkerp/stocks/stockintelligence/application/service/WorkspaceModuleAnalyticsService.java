package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceModuleAnalyticsService {

  private final NamedParameterJdbcTemplate jdbc;

  public Map<String, Object> kpis(String module, String scope, Long pharmacieId, LocalDate from, LocalDate to) {
    Map<String, Object> params = baseParams(scope, pharmacieId, from, to);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("module", module);
    out.put("total", countForModule(module, params));
    out.put("enAttente", countForModuleStatut(module, params, "EN ATTENTE"));
    out.put("valides", countForModuleStatut(module, params, "VALIDEE"));
    return out;
  }

  public List<Map<String, Object>> liste(
      String module, String scope, Long pharmacieId, LocalDate from, LocalDate to, int limit) {
    Map<String, Object> params = baseParams(scope, pharmacieId, from, to);
    params.put("limit", Math.min(Math.max(limit, 1), 500));
    String sql = listSql(module);
    if (sql == null) {
      return List.of();
    }
    return jdbc.queryForList(sql, params);
  }

  private Map<String, Object> baseParams(String scope, Long pharmacieId, LocalDate from, LocalDate to) {
    Map<String, Object> p = new HashMap<>();
    p.put("scope", scope);
    p.put("pharmacieId", pharmacieId);
    p.put("from", from);
    p.put("to", to);
    return p;
  }

  private long countForModule(String module, Map<String, Object> params) {
    String sql = countSql(module);
    if (sql == null) {
      return 0L;
    }
    Long n = jdbc.queryForObject(sql, params, Long.class);
    return n != null ? n : 0L;
  }

  private long countForModuleStatut(String module, Map<String, Object> params, String statut) {
    Map<String, Object> p = new HashMap<>(params);
    String dbStatut = mapStatut(module, statut);
    if (dbStatut == null) {
      return 0L;
    }
    p.put("statut", dbStatut);
    String sql = statutCountSql(module);
    if (sql == null) {
      return 0L;
    }
    Long n = jdbc.queryForObject(sql, p, Long.class);
    return n != null ? n : 0L;
  }

  /** Adapte le libellé KPI générique au statut réel de la table. */
  private static String mapStatut(String module, String statut) {
    if ("EN ATTENTE".equals(statut)) {
      return switch (module) {
        case "inventaires" -> "EN COURS";
        case "commandes-cotations", "invitations_fournisseur", "demandes_cotation" -> "BROUILLON";
        case "commandes-bons", "bons_commande" -> "BROUILLON";
        case "commandes-modifs", "demandes_modif_fournisseur",
            "commandes-reouvertures", "demandes_reouverture_offre" -> "EN_ATTENTE";
        case "receptions-commande", "receptions_commande" -> "BROUILLON";
        default -> "EN ATTENTE";
      };
    }
    if ("VALIDEE".equals(statut)) {
      return switch (module) {
        case "inventaires" -> "TERMINE";
        case "ventes" -> "PAYEE";
        case "receptions-stock", "reception_stock",
            "receptions-transfert-interne", "reception_transfert_interne" -> "RECEPTIONNEE";
        case "ordonnances" -> "LIVRE";
        case "commandes-cotations", "invitations_fournisseur", "demandes_cotation" -> "ATTRIBUEE";
        case "commandes-bons", "bons_commande" -> "VALIDE";
        case "commandes-modifs", "demandes_modif_fournisseur",
            "commandes-reouvertures", "demandes_reouverture_offre" -> "APPROUVEE";
        case "receptions-commande", "receptions_commande" -> "VALIDEE";
        default -> "VALIDEE";
      };
    }
    return statut;
  }

  private static String dateFilter(String alias) {
    return " AND (:from IS NULL OR DATE(" + alias + ".datecreate) >= :from)"
        + " AND (:to IS NULL OR DATE(" + alias + ".datecreate) <= :to)";
  }

  private static String pharmacieFilter(String column) {
    return " AND (:pharmacieId IS NULL OR " + column + " = :pharmacieId)";
  }

  private String countSql(String module) {
    return switch (module) {
      case "requisitions" ->
          "SELECT COUNT(*) FROM requisitions r WHERE 1=1"
              + pharmacieFilter("r.fkPharmacie")
              + dateFilter("r");
      case "receptions-commande", "receptions_commande" ->
          """
          SELECT COUNT(*) FROM receptions_commande rc
          INNER JOIN bons_commande bc ON bc.id = rc.fk_bon_commande
          WHERE 1=1
          """
              + pharmacieFilter("bc.fk_pharmacie")
              + dateFilter("rc");
      case "receptions-transfert-interne", "reception_transfert_interne" ->
          """
          SELECT COUNT(*) FROM reception_transfert_interne rt
          INNER JOIN transfert_interne ti ON ti.id = rt.fkTransfertInterne
          WHERE 1=1
          """
              + " AND (:pharmacieId IS NULL OR ti.fkPharmacieDestination = :pharmacieId OR ti.fkPharmacieSource = :pharmacieId)"
              + dateFilter("rt");
      case "perimables-alertes", "perimable_alerte_stock" ->
          """
          SELECT COUNT(*) FROM perimable_alerte_stock pas
          INNER JOIN stock_produits sp ON sp.id = pas.fkStock
          WHERE pas.notifactif = 1
          """
              + pharmacieFilter("sp.fkPharmacies");
      case "inventaires" ->
          "SELECT COUNT(*) FROM inventaires i WHERE 1=1"
              + pharmacieFilter("i.fkPharmacie")
              + dateFilter("i");
      case "fournisseurs" -> "SELECT COUNT(*) FROM fournisseurs f";
      case "point-appel", "point_appel" ->
          "SELECT COUNT(*) FROM point_appel pa WHERE 1=1"
              + pharmacieFilter("pa.fkService")
              + dateFilter("pa");
      case "ventes" ->
          "SELECT COUNT(*) FROM ventes v WHERE 1=1"
              + pharmacieFilter("v.fkPharmacie")
              + dateFilter("v");
      case "transferts-internes", "transfert_interne" ->
          "SELECT COUNT(*) FROM transfert_interne t WHERE 1=1"
              + " AND (:pharmacieId IS NULL OR t.fkPharmacieSource = :pharmacieId OR t.fkPharmacieDestination = :pharmacieId)"
              + dateFilter("t");
      case "receptions-stock", "reception_stock" ->
          """
          SELECT COUNT(*) FROM reception_stock es
          INNER JOIN transferts_stock ts ON ts.id = es.fkTransfert
          INNER JOIN requisitions r ON r.id = ts.fkRequisition
          WHERE 1=1
          """
              + " AND (:pharmacieId IS NULL OR r.fkPharmacie = :pharmacieId OR r.fkPharmacieStock = :pharmacieId)"
              + dateFilter("es");
      case "ordonnances" ->
          "SELECT COUNT(*) FROM ordonnances o WHERE 1=1"
              + pharmacieFilter("o.fkService")
              + dateFilter("o");
      case "commandes-cotations", "invitations_fournisseur", "demandes_cotation" ->
          "SELECT COUNT(*) FROM demandes_cotation dc WHERE 1=1"
              + pharmacieFilter("dc.fk_pharmacie_demandeur")
              + dateFilter("dc");
      case "commandes-bons", "bons_commande" ->
          "SELECT COUNT(*) FROM bons_commande bc WHERE 1=1"
              + pharmacieFilter("bc.fk_pharmacie")
              + dateFilter("bc");
      case "commandes-modifs", "demandes_modif_fournisseur" ->
          "SELECT COUNT(*) FROM demandes_modif_fournisseur dm WHERE 1=1" + dateFilter("dm");
      case "commandes-reouvertures", "demandes_reouverture_offre" ->
          "SELECT COUNT(*) FROM demandes_reouverture_offre d WHERE 1=1" + dateFilter("d");
      default -> null;
    };
  }

  private String statutCountSql(String module) {
    return switch (module) {
      case "requisitions" ->
          """
          SELECT COUNT(*) FROM requisitions r
          WHERE (:pharmacieId IS NULL OR r.fkPharmacie = :pharmacieId)
            AND (:from IS NULL OR DATE(r.datecreate) >= :from)
            AND (:to IS NULL OR DATE(r.datecreate) <= :to)
            AND r.statut = :statut
          """;
      case "transferts-internes", "transfert_interne" ->
          """
          SELECT COUNT(*) FROM transfert_interne t
          WHERE (:pharmacieId IS NULL OR t.fkPharmacieSource = :pharmacieId OR t.fkPharmacieDestination = :pharmacieId)
            AND (:from IS NULL OR DATE(t.datecreate) >= :from)
            AND (:to IS NULL OR DATE(t.datecreate) <= :to)
            AND t.statut = :statut
          """;
      case "inventaires" ->
          """
          SELECT COUNT(*) FROM inventaires i
          WHERE (:pharmacieId IS NULL OR i.fkPharmacie = :pharmacieId)
            AND (:from IS NULL OR DATE(i.datecreate) >= :from)
            AND (:to IS NULL OR DATE(i.datecreate) <= :to)
            AND i.statut = :statut
          """;
      case "ventes" ->
          """
          SELECT COUNT(*) FROM ventes v
          WHERE (:pharmacieId IS NULL OR v.fkPharmacie = :pharmacieId)
            AND (:from IS NULL OR DATE(v.datecreate) >= :from)
            AND (:to IS NULL OR DATE(v.datecreate) <= :to)
            AND v.statut = :statut
          """;
      case "receptions-stock", "reception_stock" ->
          """
          SELECT COUNT(*) FROM reception_stock es
          INNER JOIN transferts_stock ts ON ts.id = es.fkTransfert
          INNER JOIN requisitions r ON r.id = ts.fkRequisition
          WHERE (:pharmacieId IS NULL OR r.fkPharmacie = :pharmacieId OR r.fkPharmacieStock = :pharmacieId)
            AND (:from IS NULL OR DATE(es.datecreate) >= :from)
            AND (:to IS NULL OR DATE(es.datecreate) <= :to)
            AND es.statut = :statut
          """;
      case "receptions-transfert-interne", "reception_transfert_interne" ->
          """
          SELECT COUNT(*) FROM reception_transfert_interne rt
          INNER JOIN transfert_interne ti ON ti.id = rt.fkTransfertInterne
          WHERE (:pharmacieId IS NULL OR ti.fkPharmacieDestination = :pharmacieId OR ti.fkPharmacieSource = :pharmacieId)
            AND (:from IS NULL OR DATE(rt.datecreate) >= :from)
            AND (:to IS NULL OR DATE(rt.datecreate) <= :to)
            AND rt.statut = :statut
          """;
      case "ordonnances" ->
          """
          SELECT COUNT(*) FROM ordonnances o
          WHERE (:pharmacieId IS NULL OR o.fkService = :pharmacieId)
            AND (:from IS NULL OR DATE(o.datecreate) >= :from)
            AND (:to IS NULL OR DATE(o.datecreate) <= :to)
            AND o.statut = :statut
          """;
      case "commandes-cotations", "invitations_fournisseur", "demandes_cotation" ->
          """
          SELECT COUNT(*) FROM demandes_cotation dc
          WHERE (:pharmacieId IS NULL OR dc.fk_pharmacie_demandeur = :pharmacieId)
            AND (:from IS NULL OR DATE(dc.datecreate) >= :from)
            AND (:to IS NULL OR DATE(dc.datecreate) <= :to)
            AND dc.statut = :statut
          """;
      case "commandes-bons", "bons_commande" ->
          """
          SELECT COUNT(*) FROM bons_commande bc
          WHERE (:pharmacieId IS NULL OR bc.fk_pharmacie = :pharmacieId)
            AND (:from IS NULL OR DATE(bc.datecreate) >= :from)
            AND (:to IS NULL OR DATE(bc.datecreate) <= :to)
            AND bc.statut = :statut
          """;
      case "commandes-modifs", "demandes_modif_fournisseur" ->
          """
          SELECT COUNT(*) FROM demandes_modif_fournisseur dm
          WHERE (:from IS NULL OR DATE(dm.datecreate) >= :from)
            AND (:to IS NULL OR DATE(dm.datecreate) <= :to)
            AND dm.statut = :statut
          """;
      case "commandes-reouvertures", "demandes_reouverture_offre" ->
          """
          SELECT COUNT(*) FROM demandes_reouverture_offre d
          WHERE (:from IS NULL OR DATE(d.datecreate) >= :from)
            AND (:to IS NULL OR DATE(d.datecreate) <= :to)
            AND d.statut = :statut
          """;
      case "receptions-commande", "receptions_commande" ->
          """
          SELECT COUNT(*) FROM receptions_commande rc
          INNER JOIN bons_commande bc ON bc.id = rc.fk_bon_commande
          WHERE (:pharmacieId IS NULL OR bc.fk_pharmacie = :pharmacieId)
            AND (:from IS NULL OR DATE(rc.datecreate) >= :from)
            AND (:to IS NULL OR DATE(rc.datecreate) <= :to)
            AND rc.statut = :statut
          """;
      default -> null;
    };
  }

  private String listSql(String module) {
    return switch (module) {
      case "requisitions" ->
          """
          SELECT r.id, r.statut, r.datecreate, ph.designation AS service_nom, r.commentaire
          FROM requisitions r
          LEFT JOIN pharmacies ph ON ph.id = r.fkPharmacie
          WHERE 1=1
          """
              + pharmacieFilter("r.fkPharmacie")
              + dateFilter("r")
              + " ORDER BY r.datecreate DESC LIMIT :limit";
      case "receptions-commande", "receptions_commande" ->
          """
          SELECT rc.id, rc.statut, rc.datecreate, rc.numero,
                 ph.designation AS pharmacie_nom, f.nom AS fournisseur_nom
          FROM receptions_commande rc
          INNER JOIN bons_commande bc ON bc.id = rc.fk_bon_commande
          LEFT JOIN pharmacies ph ON ph.id = bc.fk_pharmacie
          LEFT JOIN fournisseurs f ON f.id = bc.fk_fournisseur
          WHERE 1=1
          """
              + pharmacieFilter("bc.fk_pharmacie")
              + dateFilter("rc")
              + " ORDER BY rc.datecreate DESC LIMIT :limit";
      case "receptions-transfert-interne", "reception_transfert_interne" ->
          """
          SELECT rt.id, rt.statut, rt.datecreate,
                 ps.designation AS source_nom, pd.designation AS dest_nom
          FROM reception_transfert_interne rt
          INNER JOIN transfert_interne ti ON ti.id = rt.fkTransfertInterne
          LEFT JOIN pharmacies ps ON ps.id = ti.fkPharmacieSource
          LEFT JOIN pharmacies pd ON pd.id = ti.fkPharmacieDestination
          WHERE 1=1
          """
              + " AND (:pharmacieId IS NULL OR ti.fkPharmacieDestination = :pharmacieId OR ti.fkPharmacieSource = :pharmacieId)"
              + dateFilter("rt")
              + " ORDER BY rt.datecreate DESC LIMIT :limit";
      case "perimables-alertes", "perimable_alerte_stock" ->
          """
          SELECT pas.id, pas.dateperemtion, pas.lot, pas.stockexpiree,
                 p.nomcommercial AS produit_nom, ph.designation AS pharmacie_nom
          FROM perimable_alerte_stock pas
          INNER JOIN stock_produits sp ON sp.id = pas.fkStock
          INNER JOIN produits p ON p.id = sp.fkProduits
          LEFT JOIN pharmacies ph ON ph.id = sp.fkPharmacies
          WHERE pas.notifactif = 1
          """
              + pharmacieFilter("sp.fkPharmacies")
              + " ORDER BY pas.dateperemtion ASC LIMIT :limit";
      case "inventaires" ->
          """
          SELECT i.id, i.statut, i.typeinventaire, i.datecreate, ph.designation AS pharmacie_nom
          FROM inventaires i
          LEFT JOIN pharmacies ph ON ph.id = i.fkPharmacie
          WHERE 1=1
          """
              + pharmacieFilter("i.fkPharmacie")
              + dateFilter("i")
              + " ORDER BY i.datecreate DESC LIMIT :limit";
      case "fournisseurs" ->
          """
          SELECT f.id, f.nom, f.email, f.telephone, f.adresse, f.datecreate
          FROM fournisseurs f
          ORDER BY f.nom ASC LIMIT :limit
          """;
      case "point-appel", "point_appel" ->
          """
          SELECT pa.id, pa.designation, pa.message, pa.cabinet,
                 ph.designation AS service_nom, pa.datecreate
          FROM point_appel pa
          LEFT JOIN pharmacies ph ON ph.id = pa.fkService
          WHERE 1=1
          """
              + pharmacieFilter("pa.fkService")
              + dateFilter("pa")
              + " ORDER BY pa.datecreate DESC LIMIT :limit";
      case "ventes" ->
          """
          SELECT v.id, v.statut, v.datecreate, v.typepaiement, v.demandeur,
                 ph.designation AS service_nom
          FROM ventes v
          LEFT JOIN pharmacies ph ON ph.id = v.fkPharmacie
          WHERE 1=1
          """
              + pharmacieFilter("v.fkPharmacie")
              + dateFilter("v")
              + " ORDER BY v.datecreate DESC LIMIT :limit";
      case "transferts-internes", "transfert_interne" ->
          """
          SELECT t.id, t.statut, t.datecreate,
                 ps.designation AS source_nom, pd.designation AS dest_nom
          FROM transfert_interne t
          LEFT JOIN pharmacies ps ON ps.id = t.fkPharmacieSource
          LEFT JOIN pharmacies pd ON pd.id = t.fkPharmacieDestination
          WHERE 1=1
          """
              + " AND (:pharmacieId IS NULL OR t.fkPharmacieSource = :pharmacieId OR t.fkPharmacieDestination = :pharmacieId)"
              + dateFilter("t")
              + " ORDER BY t.datecreate DESC LIMIT :limit";
      case "receptions-stock", "reception_stock" ->
          """
          SELECT es.id, es.statut, es.datecreate,
                 phd.designation AS dest_nom, phs.designation AS source_nom
          FROM reception_stock es
          INNER JOIN transferts_stock ts ON ts.id = es.fkTransfert
          INNER JOIN requisitions r ON r.id = ts.fkRequisition
          LEFT JOIN pharmacies phd ON phd.id = r.fkPharmacie
          LEFT JOIN pharmacies phs ON phs.id = r.fkPharmacieStock
          WHERE 1=1
          """
              + " AND (:pharmacieId IS NULL OR r.fkPharmacie = :pharmacieId OR r.fkPharmacieStock = :pharmacieId)"
              + dateFilter("es")
              + " ORDER BY es.datecreate DESC LIMIT :limit";
      case "ordonnances" ->
          """
          SELECT o.id, o.statut, o.codeprescription, o.datecreate, ph.designation AS service_nom
          FROM ordonnances o
          LEFT JOIN pharmacies ph ON ph.id = o.fkService
          WHERE 1=1
          """
              + pharmacieFilter("o.fkService")
              + dateFilter("o")
              + " ORDER BY o.datecreate DESC LIMIT :limit";
      case "commandes-cotations", "invitations_fournisseur", "demandes_cotation" ->
          """
          SELECT dc.id, dc.numero AS reference, dc.objet, dc.statut, dc.datecreate,
                 ph.designation AS pharmacie_nom
          FROM demandes_cotation dc
          LEFT JOIN pharmacies ph ON ph.id = dc.fk_pharmacie_demandeur
          WHERE 1=1
          """
              + pharmacieFilter("dc.fk_pharmacie_demandeur")
              + dateFilter("dc")
              + " ORDER BY dc.datecreate DESC LIMIT :limit";
      case "commandes-bons", "bons_commande" ->
          """
          SELECT bc.id, bc.numero AS reference, bc.statut, bc.datecreate,
                 f.nom AS fournisseur_nom, ph.designation AS pharmacie_nom
          FROM bons_commande bc
          LEFT JOIN fournisseurs f ON f.id = bc.fk_fournisseur
          LEFT JOIN pharmacies ph ON ph.id = bc.fk_pharmacie
          WHERE 1=1
          """
              + pharmacieFilter("bc.fk_pharmacie")
              + dateFilter("bc")
              + " ORDER BY bc.datecreate DESC LIMIT :limit";
      case "commandes-modifs", "demandes_modif_fournisseur" ->
          """
          SELECT dm.id, dm.statut, dm.motif, dm.datecreate, f.nom AS fournisseur_nom
          FROM demandes_modif_fournisseur dm
          LEFT JOIN fournisseurs f ON f.id = dm.fk_fournisseur
          WHERE 1=1
          """
              + dateFilter("dm")
              + " ORDER BY dm.datecreate DESC LIMIT :limit";
      case "commandes-reouvertures", "demandes_reouverture_offre" ->
          """
          SELECT d.id, d.statut, d.motif, d.datecreate,
                 f.nom AS fournisseur_nom, dc.numero AS cotation_reference
          FROM demandes_reouverture_offre d
          INNER JOIN offres_fournisseur o ON o.id = d.fk_offre
          LEFT JOIN fournisseurs f ON f.id = o.fk_fournisseur
          LEFT JOIN demandes_cotation dc ON dc.id = o.fk_demande_cotation
          WHERE 1=1
          """
              + dateFilter("d")
              + " ORDER BY d.datecreate DESC LIMIT :limit";
      default -> null;
    };
  }
}
