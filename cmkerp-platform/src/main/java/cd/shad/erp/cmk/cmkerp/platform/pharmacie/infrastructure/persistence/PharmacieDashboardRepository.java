package cd.shad.erp.cmk.cmkerp.platform.pharmacie.infrastructure.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieOverviewResponse;

/**
 * Repository complexe pour les requêtes avancées du dashboard Pharmacie.
 *
 * <p>
 * Ce repository gère des requêtes avec JOINs, agrégations et filtres complexes
 * qui ne sont pas couverts par les CRUD simples du shared-kernel.
 */
@Repository
public class PharmacieDashboardRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public PharmacieDashboardRepository(
      @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * RowMapper pour convertir les résultats SQL en PharmacieOverviewResponse.
   */
  private static final RowMapper<PharmacieOverviewResponse> PHARMACIE_OVERVIEW_MAPPER =
      (rs, rowNum) -> new PharmacieOverviewResponse(
          rs.getLong("pharmacie_id"),
          rs.getString("pharmacie_designation"),
          rs.getString("site_nom"),
          rs.getString("typepharmacie"),
          rs.getString("typehospi"),
          rs.getBoolean("has_access"),
          rs.getLong("nb_users_with_access"),
          rs.getLong("nb_notifications_en_cours"));

  /**
   * Recherche paginée de pharmacies avec métriques pour un utilisateur donné.
   *
   * <p>
   * La requête SQL effectue :
   * <ul>
   * <li>JOIN entre pharmacies et sites</li>
   * <li>Calcul de hasAccess via LEFT JOIN sur droits_pharmacies</li>
   * <li>Agrégation COUNT pour nbUsersWithAccess</li>
   * <li>Agrégation COUNT pour nbNotificationsEnCours (statut 'EN_ATTENTE')</li>
   * <li>Filtres optionnels sur siteId, typePharmacie, searchText</li>
   * <li>Pagination via LIMIT/OFFSET</li>
   * </ul>
   *
   * @param userId l'ID de l'utilisateur courant
   * @param siteId filtre optionnel sur le site (null = pas de filtre)
   * @param typePharmacie filtre optionnel sur le type de pharmacie (null = pas de filtre)
   * @param searchText filtre optionnel sur designation ou site.designation (null = pas de filtre)
   * @param limit nombre de résultats à retourner
   * @param offset offset pour la pagination
   * @return liste de PharmacieOverviewResponse
   */
  public List<PharmacieOverviewResponse> searchPharmaciesForUser(
      Long userId,
      Long siteId,
      String typePharmacie,
      String searchText,
      int limit,
      int offset) {

    // IMPORTANT: Cette requête retourne UNIQUEMENT les pharmacies auxquelles l'utilisateur a accès
    // via la table droits_pharmacies. Si l'utilisateur n'a pas de droits, la liste sera vide.
    // L'utilisateur connecté DOIT avoir des droits dans droits_pharmacies pour voir des pharmacies.
    StringBuilder sql = new StringBuilder("""
        SELECT
            p.id AS pharmacie_id,
            p.designation AS pharmacie_designation,
            s.designation AS site_nom,
            p.typepharmacie,
            p.typehospi,
            TRUE AS has_access,
            COALESCE(COUNT(DISTINCT dp_all.fkUtilisateur), 0) AS nb_users_with_access,
            COALESCE(COUNT(DISTINCT n.id), 0) AS nb_notifications_en_cours
        FROM pharmacies p
        INNER JOIN sites s ON p.fkSite = s.id
        INNER JOIN droits_pharmacies dp_user ON p.id = dp_user.fkPharmacie AND dp_user.fkUtilisateur = :userId
        LEFT JOIN droits_pharmacies dp_all ON p.id = dp_all.fkPharmacie
        LEFT JOIN notifications n ON n.fkUtilisateur = :userId
            AND n.statut = 'EN_ATTENTE'
            AND (n.adresse_destinataire LIKE CONCAT('%', COALESCE(p.codeimmo, ''), '%')
                 OR n.adresse_destinataire = CAST(p.id AS CHAR))
        WHERE 1=1
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    params.put("limit", limit);
    params.put("offset", offset);

    // Filtre sur siteId
    if (siteId != null) {
      sql.append(" AND p.fkSite = :siteId");
      params.put("siteId", siteId);
    }

    // Filtre sur typePharmacie
    if (typePharmacie != null && !typePharmacie.trim().isEmpty()) {
      sql.append(" AND p.typepharmacie = :typePharmacie");
      params.put("typePharmacie", typePharmacie);
    }

    // Filtre de recherche textuelle
    if (searchText != null && !searchText.trim().isEmpty()) {
      sql.append(" AND (p.designation LIKE :searchText OR s.designation LIKE :searchText)");
      params.put("searchText", "%" + searchText.trim() + "%");
    }

    // GROUP BY pour les agrégations
    // IMPORTANT: Utiliser des noms de colonnes explicites pour éviter les conflits avec les paramètres nommés
    sql.append(" GROUP BY p.id, p.designation, s.designation, p.typepharmacie, p.typehospi ");
    sql.append(" ORDER BY p.designation ASC ");
    sql.append(" LIMIT :limit OFFSET :offset ");

    return jdbc.query(sql.toString(), params, PHARMACIE_OVERVIEW_MAPPER);
  }

  /**
   * Compte le nombre total de pharmacies correspondant aux critères de recherche.
   *
   * <p>
   * Utilise les mêmes filtres que searchPharmaciesForUser mais sans pagination.
   *
   * @param userId l'ID de l'utilisateur courant
   * @param siteId filtre optionnel sur le site (null = pas de filtre)
   * @param typePharmacie filtre optionnel sur le type de pharmacie (null = pas de filtre)
   * @param searchText filtre optionnel sur designation ou site.designation (null = pas de filtre)
   * @return le nombre total de pharmacies correspondant aux critères
   */
  public long countPharmaciesForUser(
      Long userId,
      Long siteId,
      String typePharmacie,
      String searchText) {

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT p.id)
        FROM pharmacies p
        INNER JOIN sites s ON p.fkSite = s.id
        INNER JOIN droits_pharmacies dp_user ON p.id = dp_user.fkPharmacie AND dp_user.fkUtilisateur = :userId
        WHERE 1=1
        """);

    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    // Filtre sur siteId
    if (siteId != null) {
      sql.append(" AND p.fkSite = :siteId");
      params.put("siteId", siteId);
    }

    // Filtre sur typePharmacie
    if (typePharmacie != null && !typePharmacie.trim().isEmpty()) {
      sql.append(" AND p.typepharmacie = :typePharmacie");
      params.put("typePharmacie", typePharmacie);
    }

    // Filtre de recherche textuelle
    if (searchText != null && !searchText.trim().isEmpty()) {
      sql.append(" AND (p.designation LIKE :searchText OR s.designation LIKE :searchText)");
      params.put("searchText", "%" + searchText.trim() + "%");
    }

    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count != null ? count : 0L;
  }

  /**
   * RowMapper simple pour les pharmacies avec seulement id et designation.
   */
  private static final RowMapper<PharmacieSimpleResponse> PHARMACIE_SIMPLE_MAPPER =
      (rs, rowNum) -> new PharmacieSimpleResponse(
          rs.getLong("id"),
          rs.getString("designation"));

  /**
   * Récupère la liste des pharmacies auxquelles un utilisateur a accès, filtrée par type.
   *
   * <p>
   * Utilise la requête SQL exacte fournie :
   * <pre>
   * SELECT p.id, p.designation FROM pharmacies p
   * INNER JOIN droits_pharmacies d ON d.fkPharmacie = p.id
   * INNER JOIN utilisateurs u ON d.fkUtilisateur = u.id
   * WHERE u.id = ? and p.typepharmacie = ?
   * </pre>
   *
   * @param userId l'ID de l'utilisateur
   * @param typePharmacie le type de pharmacie (ex: "Centrale")
   * @return liste de pharmacies avec id et designation
   */
  public List<PharmacieSimpleResponse> findPharmaciesByUserAndType(Long userId, String typePharmacie) {
    String sql = """
        SELECT p.id, p.designation
        FROM pharmacies p
        INNER JOIN droits_pharmacies d ON d.fkPharmacie = p.id
        INNER JOIN utilisateurs u ON d.fkUtilisateur = u.id
        WHERE u.id = :userId AND p.typepharmacie = :typePharmacie
        ORDER BY p.designation ASC
        """;

    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    params.put("typePharmacie", typePharmacie);

    return jdbc.query(sql.toString(), params, PHARMACIE_SIMPLE_MAPPER);
  }

  /**
   * Classe interne simple pour représenter une pharmacie avec seulement id et designation.
   */
  public static class PharmacieSimpleResponse {
    private final Long id;
    private final String designation;

    public PharmacieSimpleResponse(Long id, String designation) {
      this.id = id;
      this.designation = designation;
    }

    public Long getId() {
      return id;
    }

    public String getDesignation() {
      return designation;
    }
  }
}

