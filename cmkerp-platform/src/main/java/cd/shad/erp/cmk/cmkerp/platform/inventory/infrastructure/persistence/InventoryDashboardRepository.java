package cd.shad.erp.cmk.cmkerp.platform.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.platform.inventory.application.dto.ProduitWithStockDTO;

/**
 * Repository complexe pour les requêtes avancées du dashboard Inventory.
 *
 * <p>
 * Ce repository gère des requêtes avec JOINs, agrégations et filtres complexes
 * pour calculer les statistiques du dashboard Inventory.
 */
@Repository
public class InventoryDashboardRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public InventoryDashboardRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Compte le nombre de produits en rupture de stock.
   *
   * <p>Un produit est en rupture si :
   * <ul>
   * <li>qte <= qtcritique OU qte <= 0</li>
   * <li>operationnel = TRUE</li>
   * </ul>
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits en rupture de stock
   */
  public Integer countRuptureStock(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT st.id)
        FROM stock_produits st
        INNER JOIN produits p ON st.fkProduits = p.id
        WHERE st.operationnel = TRUE
          AND (st.qte <= p.qtcritique OR st.qte <= 0)
        """);

    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte les produits opérationnels suivis pour une pharmacie.
   */
  public Integer countProduitsSuivis(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT st.id)
        FROM stock_produits st
        WHERE st.operationnel = TRUE
        """);

    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de produits qui expireront dans 3 mois.
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits qui expireront dans 3 mois
   */
  public Integer countPerimeDans3Mois(Long pharmacieId) {
    LocalDate dans3Mois = LocalDate.now().plusMonths(3);

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT pas.fkStock)
        FROM perimable_alerte_stock pas
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        WHERE pas.notifactif = TRUE
          AND pas.dateperemtion BETWEEN CURDATE() AND :dans3Mois
          AND st.operationnel = TRUE
          AND st.qte > 0
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("dans3Mois", dans3Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de produits qui expireront dans 1 mois.
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits qui expireront dans 1 mois
   */
  public Integer countPerimeDans1Mois(Long pharmacieId) {
    LocalDate dans1Mois = LocalDate.now().plusMonths(1);

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT pas.fkStock)
        FROM perimable_alerte_stock pas
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        WHERE pas.notifactif = TRUE
          AND pas.dateperemtion BETWEEN CURDATE() AND :dans1Mois
          AND st.operationnel = TRUE
          AND st.qte > 0
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("dans1Mois", dans1Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de produits avec achat conforme (faible risque >= 18 mois).
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits avec achat conforme
   */
  public Integer countAchatConforme(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT pas.fkStock)
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 540
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT COUNT(DISTINCT pas.fkStock)
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 540
          """);
      params = new HashMap<>();
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de produits avec achat acceptable (à surveiller, entre 12 et 17 mois).
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits avec achat acceptable
   */
  public Integer countAchatAcceptable(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT pas.fkStock)
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 360
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 540
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT COUNT(DISTINCT pas.fkStock)
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 360
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 540
          """);
      params = new HashMap<>();
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de produits avec achat à risque élevé (entre 6 et 11 mois).
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits avec achat à risque élevé
   */
  public Integer countAchatRisqueEleve(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT pas.fkStock)
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 180
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 360
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT COUNT(DISTINCT pas.fkStock)
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 180
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 360
          """);
      params = new HashMap<>();
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de produits avec achat non conforme (à refuser, < 6 mois).
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits avec achat non conforme
   */
  public Integer countAchatNonConforme(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT pas.fkStock)
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 180
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT COUNT(DISTINCT pas.fkStock)
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 180
          """);
      params = new HashMap<>();
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de produits en stock dormant.
   *
   * <p>Un stock est considéré comme dormant si :
   * <ul>
   * <li>La date de dernière mise à jour (dateupdate) est supérieure à 6 mois</li>
   * <li>ET qte > 0</li>
   * </ul>
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de produits en stock dormant
   */
  public Integer countStockDormant(Long pharmacieId) {
    LocalDate ilYA6Mois = LocalDate.now().minusMonths(6);

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT st.id)
        FROM stock_produits st
        WHERE st.operationnel = TRUE
          AND st.qte > 0
          AND DATE(st.dateupdate) < :ilYA6Mois
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("ilYA6Mois", ilYA6Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de stocks les plus mouvementés.
   *
   * <p>Un stock est considéré comme très mouvementé si :
   * <ul>
   * <li>La date de dernière mise à jour (dateupdate) est récente (< 7 jours)</li>
   * </ul>
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de stocks les plus mouvementés
   */
  public Integer countStockPlusMouvementes(Long pharmacieId) {
    LocalDate ilYA7Jours = LocalDate.now().minusDays(7);

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT st.id)
        FROM stock_produits st
        WHERE st.operationnel = TRUE
          AND st.qte > 0
          AND DATE(st.dateupdate) >= :ilYA7Jours
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("ilYA7Jours", ilYA7Jours);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de stocks les moins mouvementés.
   *
   * <p>Un stock est considéré comme peu mouvementé si :
   * <ul>
   * <li>La date de dernière mise à jour (dateupdate) est ancienne (> 3 mois)</li>
   * <li>ET qte > 0</li>
   * </ul>
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de stocks les moins mouvementés
   */
  public Integer countStockMoinsMouvementes(Long pharmacieId) {
    LocalDate ilYA3Mois = LocalDate.now().minusMonths(3);

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT st.id)
        FROM stock_produits st
        WHERE st.operationnel = TRUE
          AND st.qte > 0
          AND DATE(st.dateupdate) < :ilYA3Mois
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("ilYA3Mois", ilYA3Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre total de fournisseurs.
   *
   * @return nombre total de fournisseurs
   */
  public Integer countFournisseurs() {
    String sql = "SELECT COUNT(*) FROM fournisseurs";
    Long count = jdbc.queryForObject(sql, new HashMap<>(), Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de demandes (requisitions) en attente.
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de demandes en attente
   */
  public Integer countDemandesEnAttente(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*)
        FROM requisitions
        WHERE statut = 'EN ATTENTE'
        """);

    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND fkPharmacie = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * Compte le nombre de réceptions en attente.
   *
   * <p>Une réception est liée à une pharmacie via :
   * reception_stock -> transferts_stock -> requisitions -> fkPharmacie
   * où fkPharmacie est la pharmacie qui a fait la demande (qui va recevoir).
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return nombre de réceptions en attente
   */
  public Integer countReceptionEnAttente(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT rs.id)
        FROM reception_stock rs
        INNER JOIN transferts_stock ts ON rs.fkTransfert = ts.id
        INNER JOIN requisitions r ON ts.fkRequisition = r.id
        WHERE rs.statut = 'EN ATTENTE'
        """);

    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND r.fkPharmacie = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count.intValue() : 0;
  }

  /**
   * RowMapper pour convertir les résultats SQL en ProduitWithStockDTO.
   */
  private static final RowMapper<ProduitWithStockDTO> PRODUIT_WITH_STOCK_MAPPER = (rs, rowNum) -> {
    ProduitWithStockDTO.ProduitWithStockDTOBuilder builder = ProduitWithStockDTO.builder()
        .id(getLongOrNull(rs, "id"))
        .codebarre(getStringOrNull(rs, "codebarre"))
        .nomcommercial(getStringOrNull(rs, "nomcommercial"))
        .nomscientifique(getStringOrNull(rs, "nomscientifique"))
        .forme(getStringOrNull(rs, "forme"))
        .dosage(getStringOrNull(rs, "dosage"))
        .conditionnement(getStringOrNull(rs, "conditionnement"))
        .categorie(getStringOrNull(rs, "categorie"))
        .stockId(getLongOrNull(rs, "stockId"))
        .stockencours(getFloatOrNull(rs, "stockencours"))
        .isactif(getBooleanOrNull(rs, "isactif"))
        .peremption(getStringOrNull(rs, "peremption"))
        .prixachat(getBigDecimalOrNull(rs, "prixachat"))
        .qtealert(getFloatOrNull(rs, "qtealert"))
        .qtcritique(getFloatOrNull(rs, "qtcritique"))
        .perimable(getBooleanOrNull(rs, "perimable"))
        .dateCreate(getLocalDateTimeOrNull(rs, "dateCreate"))
        .dateApprov(getLocalDateTimeOrNullFromDate(rs, "dateApprov"));

    return builder.build();
  };

  private static Long getLongOrNull(java.sql.ResultSet rs, String column) {
    try {
      long value = rs.getLong(column);
      return rs.wasNull() ? null : value;
    } catch (java.sql.SQLException e) {
      return null;
    }
  }

  private static Float getFloatOrNull(java.sql.ResultSet rs, String column) {
    try {
      float value = rs.getFloat(column);
      return rs.wasNull() ? null : value;
    } catch (java.sql.SQLException e) {
      return null;
    }
  }

  private static String getStringOrNull(java.sql.ResultSet rs, String column) {
    try {
      return rs.getString(column);
    } catch (java.sql.SQLException e) {
      return null;
    }
  }

  private static BigDecimal getBigDecimalOrNull(java.sql.ResultSet rs, String column) {
    try {
      return rs.getBigDecimal(column);
    } catch (java.sql.SQLException e) {
      return null;
    }
  }

  private static LocalDateTime getLocalDateTimeOrNull(java.sql.ResultSet rs, String column) {
    try {
      java.sql.Timestamp timestamp = rs.getTimestamp(column);
      return timestamp != null ? timestamp.toLocalDateTime() : null;
    } catch (java.sql.SQLException e) {
      return null;
    }
  }

  private static Boolean getBooleanOrNull(java.sql.ResultSet rs, String column) {
    try {
      boolean value = rs.getBoolean(column);
      return rs.wasNull() ? null : value;
    } catch (java.sql.SQLException e) {
      return null;
    }
  }

  private static LocalDateTime getLocalDateTimeOrNullFromDate(java.sql.ResultSet rs, String column) {
    try {
      java.sql.Date date = rs.getDate(column);
      if (date == null || rs.wasNull()) {
        return null;
      }
      return date.toLocalDate().atStartOfDay();
    } catch (java.sql.SQLException e) {
      return null;
    }
  }

  /**
   * Récupère la liste des produits en rupture de stock pour le rapport.
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return liste des produits en rupture de stock
   */
  public List<ProduitWithStockDTO> findRuptureStock(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pa.peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable
        FROM stock_produits st
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = TRUE
            GROUP BY fkStock
        ) pa ON pa.fkStock = st.id AND st.qte > 0
        WHERE st.operationnel = TRUE
          AND (st.qte <= p.qtcritique OR st.qte <= 0)
        """);

    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    sql.append(" ORDER BY p.nomcommercial ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Liste des produits opérationnels suivis (détail KPI).
   */
  public List<ProduitWithStockDTO> findProduitsSuivis(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pa.peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable
        FROM stock_produits st
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = 1
            GROUP BY fkStock
        ) pa ON pa.fkStock = st.id AND st.qte > 0
        WHERE st.operationnel = TRUE
        """);

    Map<String, Object> params = new HashMap<>();
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    sql.append(" ORDER BY p.nomcommercial ASC LIMIT 500");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des produits qui expireront dans 3 mois pour le rapport.
   */
  public List<ProduitWithStockDTO> findPerimeDans3Mois(Long pharmacieId) {
    LocalDate dans3Mois = LocalDate.now().plusMonths(3);

    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pa.peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable
        FROM perimable_alerte_stock pas
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = TRUE
            GROUP BY fkStock
        ) pa ON pa.fkStock = st.id
        WHERE pas.notifactif = TRUE
          AND pas.dateperemtion BETWEEN CURDATE() AND :dans3Mois
          AND st.operationnel = TRUE
          AND st.qte > 0
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("dans3Mois", dans3Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    sql.append(" ORDER BY p.nomcommercial ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des produits qui expireront dans 1 mois pour le rapport.
   */
  public List<ProduitWithStockDTO> findPerimeDans1Mois(Long pharmacieId) {
    LocalDate dans1Mois = LocalDate.now().plusMonths(1);

    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pa.peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable
        FROM perimable_alerte_stock pas
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = TRUE
            GROUP BY fkStock
        ) pa ON pa.fkStock = st.id
        WHERE pas.notifactif = TRUE
          AND pas.dateperemtion BETWEEN CURDATE() AND :dans1Mois
          AND st.operationnel = TRUE
          AND st.qte > 0
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("dans1Mois", dans1Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    sql.append(" ORDER BY p.nomcommercial ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des produits avec achat conforme (faible risque >= 18 mois) pour le rapport.
   * Retourne une ligne par produit/approvisionnement avec date d'approvisionnement et date de péremption.
   */
  public List<ProduitWithStockDTO> findAchatConforme(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pas.dateperemtion AS peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable,
            DATE(ap.datecreate) AS dateApprov
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 540
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT
              p.id,
              p.codebarre,
              p.nomcommercial,
              p.nomscientifique,
              p.dateCreate,
              f.designation AS forme,
              d.designation AS dosage,
              c.designation AS conditionnement,
              ct.designation AS categorie,
              st.id AS stockId,
              st.qte AS stockencours,
              st.operationnel AS isactif,
              pas.dateperemtion AS peremption,
              p.prixachat,
              p.qtealert,
              p.qtcritique,
              p.perimable,
              DATE(ap.datecreate) AS dateApprov
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          INNER JOIN stock_produits st ON pas.fkStock = st.id
          INNER JOIN produits p ON st.fkProduits = p.id
          INNER JOIN formes f ON p.fkForme = f.id
          INNER JOIN dosages d ON p.fkDosage = d.id
          INNER JOIN conditionnements c ON p.fkConditionnement = c.id
          INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 540
          """);
      params = new HashMap<>();
    }

    sql.append(" ORDER BY p.nomcommercial ASC, ap.datecreate ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des produits avec achat acceptable (à surveiller, entre 12 et 17 mois) pour le rapport.
   * Retourne une ligne par produit/approvisionnement avec date d'approvisionnement et date de péremption.
   */
  public List<ProduitWithStockDTO> findAchatAcceptable(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pas.dateperemtion AS peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable,
            DATE(ap.datecreate) AS dateApprov
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 360
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 540
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT
              p.id,
              p.codebarre,
              p.nomcommercial,
              p.nomscientifique,
              p.dateCreate,
              f.designation AS forme,
              d.designation AS dosage,
              c.designation AS conditionnement,
              ct.designation AS categorie,
              st.id AS stockId,
              st.qte AS stockencours,
              st.operationnel AS isactif,
              pas.dateperemtion AS peremption,
              p.prixachat,
              p.qtealert,
              p.qtcritique,
              p.perimable,
              DATE(ap.datecreate) AS dateApprov
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          INNER JOIN stock_produits st ON pas.fkStock = st.id
          INNER JOIN produits p ON st.fkProduits = p.id
          INNER JOIN formes f ON p.fkForme = f.id
          INNER JOIN dosages d ON p.fkDosage = d.id
          INNER JOIN conditionnements c ON p.fkConditionnement = c.id
          INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 360
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 540
          """);
      params = new HashMap<>();
    }

    sql.append(" ORDER BY p.nomcommercial ASC, ap.datecreate ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des produits avec achat à risque élevé (entre 6 et 11 mois) pour le rapport.
   * Retourne une ligne par produit/approvisionnement avec date d'approvisionnement et date de péremption.
   */
  public List<ProduitWithStockDTO> findAchatRisqueEleve(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pas.dateperemtion AS peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable,
            DATE(ap.datecreate) AS dateApprov
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 180
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 360
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT
              p.id,
              p.codebarre,
              p.nomcommercial,
              p.nomscientifique,
              p.dateCreate,
              f.designation AS forme,
              d.designation AS dosage,
              c.designation AS conditionnement,
              ct.designation AS categorie,
              st.id AS stockId,
              st.qte AS stockencours,
              st.operationnel AS isactif,
              pas.dateperemtion AS peremption,
              p.prixachat,
              p.qtealert,
              p.qtcritique,
              p.perimable,
              DATE(ap.datecreate) AS dateApprov
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          INNER JOIN stock_produits st ON pas.fkStock = st.id
          INNER JOIN produits p ON st.fkProduits = p.id
          INNER JOIN formes f ON p.fkForme = f.id
          INNER JOIN dosages d ON p.fkDosage = d.id
          INNER JOIN conditionnements c ON p.fkConditionnement = c.id
          INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) >= 180
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 360
          """);
      params = new HashMap<>();
    }

    sql.append(" ORDER BY p.nomcommercial ASC, ap.datecreate ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des produits avec achat non conforme (à refuser, < 6 mois) pour le rapport.
   * Retourne une ligne par produit/approvisionnement avec date d'approvisionnement et date de péremption.
   */
  public List<ProduitWithStockDTO> findAchatNonConforme(Long pharmacieId) {
    StringBuilder sql = new StringBuilder("""
        SELECT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pas.dateperemtion AS peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable,
            DATE(ap.datecreate) AS dateApprov
        FROM perimable_alerte_stock pas
        INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
        INNER JOIN stock_produits st ON pas.fkStock = st.id
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        WHERE ap.fkPharmacie = :pharmacieId
          AND pas.notifactif = TRUE
          AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 180
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("pharmacieId", pharmacieId);
    if (pharmacieId == null) {
      sql = new StringBuilder("""
          SELECT
              p.id,
              p.codebarre,
              p.nomcommercial,
              p.nomscientifique,
              p.dateCreate,
              f.designation AS forme,
              d.designation AS dosage,
              c.designation AS conditionnement,
              ct.designation AS categorie,
              st.id AS stockId,
              st.qte AS stockencours,
              st.operationnel AS isactif,
              pas.dateperemtion AS peremption,
              p.prixachat,
              p.qtealert,
              p.qtcritique,
              p.perimable,
              DATE(ap.datecreate) AS dateApprov
          FROM perimable_alerte_stock pas
          INNER JOIN approvsionnements ap ON pas.fkAprov = ap.id
          INNER JOIN stock_produits st ON pas.fkStock = st.id
          INNER JOIN produits p ON st.fkProduits = p.id
          INNER JOIN formes f ON p.fkForme = f.id
          INNER JOIN dosages d ON p.fkDosage = d.id
          INNER JOIN conditionnements c ON p.fkConditionnement = c.id
          INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
          WHERE pas.notifactif = TRUE
            AND DATEDIFF(pas.dateperemtion, DATE(ap.datecreate)) < 180
          """);
      params = new HashMap<>();
    }

    sql.append(" ORDER BY p.nomcommercial ASC, ap.datecreate ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des produits en stock dormant pour le rapport.
   */
  public List<ProduitWithStockDTO> findStockDormant(Long pharmacieId) {
    LocalDate ilYA6Mois = LocalDate.now().minusMonths(6);

    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pa.peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable
        FROM stock_produits st
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = TRUE
            GROUP BY fkStock
        ) pa ON pa.fkStock = st.id AND st.qte > 0
        WHERE st.operationnel = TRUE
          AND st.qte > 0
          AND DATE(st.dateupdate) < :ilYA6Mois
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("ilYA6Mois", ilYA6Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    sql.append(" ORDER BY p.nomcommercial ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des stocks les plus mouvementés pour le rapport.
   */
  public List<ProduitWithStockDTO> findStockPlusMouvementes(Long pharmacieId) {
    LocalDate ilYA7Jours = LocalDate.now().minusDays(7);

    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pa.peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable,
            st.dateupdate
        FROM stock_produits st
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = TRUE
            GROUP BY fkStock
        ) pa ON pa.fkStock = st.id AND st.qte > 0
        WHERE st.operationnel = TRUE
          AND st.qte > 0
          AND DATE(st.dateupdate) >= :ilYA7Jours
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("ilYA7Jours", ilYA7Jours);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    sql.append(" ORDER BY st.dateupdate DESC, p.nomcommercial ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }

  /**
   * Récupère la liste des stocks les moins mouvementés pour le rapport.
   */
  public List<ProduitWithStockDTO> findStockMoinsMouvementes(Long pharmacieId) {
    LocalDate ilYA3Mois = LocalDate.now().minusMonths(3);

    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT
            p.id,
            p.codebarre,
            p.nomcommercial,
            p.nomscientifique,
            p.dateCreate,
            f.designation AS forme,
            d.designation AS dosage,
            c.designation AS conditionnement,
            ct.designation AS categorie,
            st.id AS stockId,
            st.qte AS stockencours,
            st.operationnel AS isactif,
            pa.peremption,
            p.prixachat,
            p.qtealert,
            p.qtcritique,
            p.perimable,
            st.dateupdate
        FROM stock_produits st
        INNER JOIN produits p ON st.fkProduits = p.id
        INNER JOIN formes f ON p.fkForme = f.id
        INNER JOIN dosages d ON p.fkDosage = d.id
        INNER JOIN conditionnements c ON p.fkConditionnement = c.id
        INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
        LEFT JOIN (
            SELECT
                fkStock,
                GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE notifactif = TRUE
            GROUP BY fkStock
        ) pa ON pa.fkStock = st.id AND st.qte > 0
        WHERE st.operationnel = TRUE
          AND st.qte > 0
          AND DATE(st.dateupdate) < :ilYA3Mois
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("ilYA3Mois", ilYA3Mois);
    if (pharmacieId != null) {
      sql.append(" AND st.fkPharmacies = :pharmacieId");
      params.put("pharmacieId", pharmacieId);
    }

    sql.append(" ORDER BY st.dateupdate ASC, p.nomcommercial ASC");

    return jdbc.query(sql.toString(), params, PRODUIT_WITH_STOCK_MAPPER);
  }
}

