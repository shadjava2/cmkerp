package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Rapport consommation service : sorties PAYEE (vente privée), FACTUREE (conventionnée),
 * SORTIE-USAGE (utilisation interne).
 */
@Repository
public class ConsommationServiceAnalyticsRepository {

  private static final String USAGE_LABEL =
      """
      CASE v.statut
        WHEN 'PAYEE' THEN 'VENTE PRIVEE'
        WHEN 'FACTUREE' THEN 'FACTUREE'
        WHEN 'SORTIE-USAGE' THEN 'USAGE'
        ELSE v.statut
      END
      """;

  private static final String PATIENT_LABEL =
      """
      NULLIF(TRIM(CONCAT_WS(' ',
        NULLIF(TRIM(pa.prenom), ''),
        NULLIF(TRIM(pa.nom), ''),
        NULLIF(TRIM(pa.postnom), '')
      )), '')
      """;

  private static final String FROM_LIGNES =
      """
      FROM lignes_vente lv
      INNER JOIN ventes v ON v.id = lv.fkVente
      INNER JOIN pharmacies ph ON ph.id = v.fkPharmacie
      LEFT JOIN stock_produits sp ON sp.id = lv.fkStock
      LEFT JOIN produits p ON p.id = sp.fkProduits
      LEFT JOIN patients pa ON pa.id = v.fkPatient
      LEFT JOIN entreprises e ON e.id = v.fkEntreprise AND COALESCE(v.fkEntreprise, 0) <> 0
      WHERE v.statut IN ('PAYEE', 'FACTUREE', 'SORTIE-USAGE')
      """;

  private final NamedParameterJdbcTemplate jdbc;

  public ConsommationServiceAnalyticsRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> kpis(Long pharmacieId, LocalDate from, LocalDate to, String q) {
    Map<String, Object> params = baseParams(pharmacieId, from, to, q);
    String sql =
        """
        SELECT
          COUNT(DISTINCT v.id) AS nb_sorties,
          COUNT(*) AS nb_lignes,
          COUNT(DISTINCT p.id) AS nb_produits,
          COALESCE(SUM(COALESCE(lv.qt, 0)), 0) AS quantite_totale,
          COALESCE(SUM(COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0)), 0) AS montant_total,
          COALESCE(SUM(CASE WHEN v.statut = 'PAYEE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_vente_privee,
          COALESCE(SUM(CASE WHEN v.statut = 'FACTUREE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_facturee,
          COALESCE(SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_usage,
          COALESCE(SUM(CASE WHEN v.statut = 'PAYEE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_vente_privee,
          COALESCE(SUM(CASE WHEN v.statut = 'FACTUREE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_facturee,
          COALESCE(SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_usage,
          SUM(CASE WHEN v.statut = 'PAYEE' THEN 1 ELSE 0 END) AS lignes_vente_privee,
          SUM(CASE WHEN v.statut = 'FACTUREE' THEN 1 ELSE 0 END) AS lignes_facturee,
          SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN 1 ELSE 0 END) AS lignes_usage
        """
            + FROM_LIGNES
            + pharmacieFilter()
            + dateFilter()
            + produitFilter(params);
    List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
    return rows.isEmpty() ? Map.of() : rows.get(0);
  }

  public List<Map<String, Object>> statsMensuel(
      Long pharmacieId, LocalDate from, LocalDate to, String q) {
    Map<String, Object> params = baseParams(pharmacieId, from, to, q);
    String sql =
        """
        SELECT
          DATE_FORMAT(v.datecreate, '%Y-%m') AS periode,
          COUNT(DISTINCT v.id) AS nb_sorties,
          COUNT(*) AS nb_lignes,
          COUNT(DISTINCT p.id) AS nb_produits,
          COALESCE(SUM(COALESCE(lv.qt, 0)), 0) AS quantite_totale,
          COALESCE(SUM(COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0)), 0) AS montant_total,
          COALESCE(SUM(CASE WHEN v.statut = 'PAYEE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_vente_privee,
          COALESCE(SUM(CASE WHEN v.statut = 'FACTUREE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_facturee,
          COALESCE(SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_usage,
          COALESCE(SUM(CASE WHEN v.statut = 'PAYEE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_vente_privee,
          COALESCE(SUM(CASE WHEN v.statut = 'FACTUREE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_facturee,
          COALESCE(SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_usage
        """
            + FROM_LIGNES
            + pharmacieFilter()
            + dateFilter()
            + produitFilter(params)
            + """
            GROUP BY DATE_FORMAT(v.datecreate, '%Y-%m')
            ORDER BY periode DESC
            """;
    return jdbc.queryForList(sql, params);
  }

  public List<Map<String, Object>> statsProduits(
      Long pharmacieId, LocalDate from, LocalDate to, String q, int limit) {
    Map<String, Object> params = baseParams(pharmacieId, from, to, q);
    params.put("limit", Math.min(Math.max(limit, 1), 500));
    String sql =
        """
        SELECT
          p.id AS produit_id,
          COALESCE(MAX(p.nomcommercial), MAX(p.nomscientifique), CONCAT('Produit #', COALESCE(p.id, 0))) AS produit,
          MAX(p.codebarre) AS codebarre,
          COUNT(*) AS nb_lignes,
          COUNT(DISTINCT v.id) AS nb_sorties,
          COALESCE(SUM(COALESCE(lv.qt, 0)), 0) AS quantite_totale,
          COALESCE(SUM(COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0)), 0) AS montant_total,
          COALESCE(SUM(CASE WHEN v.statut = 'PAYEE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_vente_privee,
          COALESCE(SUM(CASE WHEN v.statut = 'FACTUREE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_facturee,
          COALESCE(SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN COALESCE(lv.qt, 0) ELSE 0 END), 0) AS qty_usage,
          COALESCE(SUM(CASE WHEN v.statut = 'PAYEE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_vente_privee,
          COALESCE(SUM(CASE WHEN v.statut = 'FACTUREE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_facturee,
          COALESCE(SUM(CASE WHEN v.statut = 'SORTIE-USAGE' THEN COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0) ELSE 0 END), 0) AS montant_usage
        """
            + FROM_LIGNES
            + pharmacieFilter()
            + dateFilter()
            + produitFilter(params)
            + """
            GROUP BY p.id
            ORDER BY quantite_totale DESC
            LIMIT :limit
            """;
    return jdbc.queryForList(sql, params);
  }

  /**
   * Quantités sorties par produit et par mois (pour tableau croisé).
   */
  public List<Map<String, Object>> statsProduitsParMois(
      Long pharmacieId, LocalDate from, LocalDate to, String q, int limitProduits) {
    Map<String, Object> params = baseParams(pharmacieId, from, to, q);
    params.put("limit", Math.min(Math.max(limitProduits, 1), 500));
    // Top produits de la période, puis ventilation mensuelle uniquement pour ceux-là
    String sql =
        """
        SELECT
          p.id AS produit_id,
          COALESCE(MAX(p.nomcommercial), MAX(p.nomscientifique), CONCAT('Produit #', COALESCE(p.id, 0))) AS produit,
          MAX(p.codebarre) AS codebarre,
          DATE_FORMAT(v.datecreate, '%Y-%m') AS periode,
          COALESCE(SUM(COALESCE(lv.qt, 0)), 0) AS quantite
        """
            + FROM_LIGNES
            + pharmacieFilter()
            + dateFilter()
            + produitFilter(params)
            + """
            AND p.id IN (
              SELECT top_p.produit_id FROM (
                SELECT p2.id AS produit_id
                FROM lignes_vente lv2
                INNER JOIN ventes v2 ON v2.id = lv2.fkVente
                LEFT JOIN stock_produits sp2 ON sp2.id = lv2.fkStock
                LEFT JOIN produits p2 ON p2.id = sp2.fkProduits
                WHERE v2.statut IN ('PAYEE', 'FACTUREE', 'SORTIE-USAGE')
                  AND (:pharmacieId IS NULL OR v2.fkPharmacie = :pharmacieId)
                  AND (:from IS NULL OR DATE(v2.datecreate) >= :from)
                  AND (:to IS NULL OR DATE(v2.datecreate) <= :to)
            """;
    if (params.containsKey("q")) {
      sql +=
          """
                  AND (
                    LOWER(COALESCE(p2.nomcommercial, '')) LIKE :q
                    OR LOWER(COALESCE(p2.nomscientifique, '')) LIKE :q
                    OR LOWER(COALESCE(p2.codebarre, '')) LIKE :q
                  )
          """;
    }
    sql +=
        """
                GROUP BY p2.id
                ORDER BY COALESCE(SUM(COALESCE(lv2.qt, 0)), 0) DESC
                LIMIT :limit
              ) top_p
            )
            GROUP BY p.id, DATE_FORMAT(v.datecreate, '%Y-%m')
            ORDER BY produit ASC, periode ASC
            """;
    return jdbc.queryForList(sql, params);
  }

  public List<Map<String, Object>> details(
      Long pharmacieId, LocalDate from, LocalDate to, String usageType, String q, int limit) {
    Map<String, Object> params = baseParams(pharmacieId, from, to, q);
    params.put("limit", Math.min(Math.max(limit, 1), 1000));
    StringBuilder sql = new StringBuilder(
        """
        SELECT
          lv.id AS ligne_id,
          v.id AS vente_id,
          v.datecreate,
          v.statut,
        """
            + USAGE_LABEL
            + " AS usage_type,\n"
            + """
          ph.designation AS service_nom,
          COALESCE(p.nomcommercial, p.nomscientifique, CONCAT('Stock #', lv.fkStock)) AS produit,
          p.codebarre,
          lv.qt AS quantite,
          lv.prixventes AS prix_unitaire,
          (COALESCE(lv.qt, 0) * COALESCE(lv.prixventes, 0)) AS montant_ligne,
          """
            + PATIENT_LABEL
            + " AS patient,\n"
            + """
          pa.codeipp AS patient_codeipp,
          e.designation AS entreprise,
          e.code AS entreprise_code,
          v.demandeur,
          v.raisonsortie AS commentaire,
          v.typepaiement
        """
            + FROM_LIGNES
            + pharmacieFilter()
            + dateFilter()
            + produitFilter(params));

    if (usageType != null && !usageType.isBlank()) {
      String statut = mapUsageToStatut(usageType.trim());
      if (statut != null) {
        sql.append(" AND v.statut = :usageStatut");
        params.put("usageStatut", statut);
      }
    }

    sql.append(" ORDER BY v.datecreate DESC, lv.id DESC LIMIT :limit");
    return jdbc.queryForList(sql.toString(), params);
  }

  private static String mapUsageToStatut(String usage) {
    return switch (usage.toUpperCase()) {
      case "VENTE PRIVEE", "PAYEE", "PRIVEE" -> "PAYEE";
      case "FACTUREE", "CONVENTIONNEE" -> "FACTUREE";
      case "USAGE", "SORTIE-USAGE", "UTILISATION" -> "SORTIE-USAGE";
      default -> null;
    };
  }

  private static Map<String, Object> baseParams(
      Long pharmacieId, LocalDate from, LocalDate to, String q) {
    Map<String, Object> p = new HashMap<>();
    p.put("pharmacieId", pharmacieId);
    p.put("from", from);
    p.put("to", to);
    if (q != null && !q.isBlank()) {
      p.put("q", "%" + q.trim().toLowerCase() + "%");
    }
    return p;
  }

  private static String pharmacieFilter() {
    return " AND (:pharmacieId IS NULL OR v.fkPharmacie = :pharmacieId)";
  }

  private static String dateFilter() {
    return " AND (:from IS NULL OR DATE(v.datecreate) >= :from)"
        + " AND (:to IS NULL OR DATE(v.datecreate) <= :to)";
  }

  /** Filtre produit (nom commercial, scientifique, code-barres). */
  private static String produitFilter(Map<String, Object> params) {
    if (!params.containsKey("q")) {
      return "";
    }
    return """
         AND (
           LOWER(COALESCE(p.nomcommercial, '')) LIKE :q
           OR LOWER(COALESCE(p.nomscientifique, '')) LIKE :q
           OR LOWER(COALESCE(p.codebarre, '')) LIKE :q
         )
        """;
  }
}
