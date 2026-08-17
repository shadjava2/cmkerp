package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitDetailResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitWithStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Produit;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.ProduitRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.CursorPageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Query Service pour la gestion des produits (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées aux produits.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 * Utilise les JOINs MySQL 8 pour récupérer les désignations des tables de référence.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class ProduitQueryService {

    private final ProduitRepository produitRepository;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public ProduitQueryService(
            ProduitRepository produitRepository,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.produitRepository = produitRepository;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * RowMapper pour convertir les résultats SQL en ProduitDetailResponse avec JOINs.
     * Optimisé pour récupérer toutes les désignations en une seule requête.
     */
    private static final RowMapper<ProduitDetailResponse> PRODUIT_DETAIL_MAPPER = (rs, rowNum) -> {
        ProduitDetailResponse.ProduitDetailResponseBuilder builder = ProduitDetailResponse.builder()
                .id(rs.getLong("id"))
                .codebarre(rs.getString("codebarre"))
                .nomcommercial(rs.getString("nomcommercial"))
                .nomscientifique(rs.getString("nomscientifique"))
                .fkForme(getLongOrNull(rs, "fkForme"))
                .fkDosage(getLongOrNull(rs, "fkDosage"))
                .fkConditionnement(getLongOrNull(rs, "fkConditionnement"))
                .fkCategorie(getLongOrNull(rs, "fkCategorie"))
                .prixachat(getBigDecimalOrNull(rs, "prixachat"))
                .prixachatcomptable(getBigDecimalOrNull(rs, "prixachatcomptable"))
                .qtealert(getFloatOrNull(rs, "qtealert"))
                .qtcritique(getFloatOrNull(rs, "qtcritique"))
                .perimable(rs.getBoolean("perimable"))
                .dateCreate(getLocalDateTimeOrNull(rs, "dateCreate"))
                .dateUpdate(getLocalDateTimeOrNull(rs, "dateUpdate"))
                // Désignations récupérées via JOINs
                .forme(rs.getString("forme_designation"))
                .dosage(rs.getString("dosage_designation"))
                .conditionnement(rs.getString("conditionnement_designation"))
                .categorie(rs.getString("categorie_designation"));

        return builder.build();
    };

    private static Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static BigDecimal getBigDecimalOrNull(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return rs.wasNull() ? null : value;
    }

    private static Float getFloatOrNull(ResultSet rs, String column) throws SQLException {
        float value = rs.getFloat(column);
        return rs.wasNull() ? null : value;
    }

    private static String getStringOrNull(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTimeOrNull(ResultSet rs, String column) throws SQLException {
        try {
            java.sql.Timestamp timestamp = rs.getTimestamp(column);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        } catch (SQLException e) {
            // Si la colonne n'existe pas, logger l'erreur et retourner null
            log.warn("Colonne '{}' introuvable dans le ResultSet: {}", column, e.getMessage());
            return null;
        }
    }

    /**
     * Récupère une page de produits avec pagination.
     */
    public Page<ProduitResponse> findAll(Pageable pageable) {
        log.debug("Récupération de la page de produits: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return produitRepository.findAllByOrderByNomcommercialAsc(pageable)
                .map(this::produitToResponse);
    }

    /**
     * Récupère une page de produits filtrés par nom commercial.
     */
    public Page<ProduitResponse> findByNomcommercial(String nomcommercial, Pageable pageable) {
        log.debug("Recherche de produits par nom commercial: '{}'", nomcommercial);
        return produitRepository.findByNomcommercialContainingIgnoreCase(nomcommercial, pageable)
                .map(this::produitToResponse);
    }

    /**
     * Récupère un produit par son ID (sans JOINs, pour liste).
     */
    public ProduitResponse findById(Long id) {
        log.debug("Récupération du produit ID: {}", id);
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Produit", id));

        return produitToResponse(produit);
    }

    /**
     * Récupère un produit par son ID avec toutes les relations (JOINs MySQL 8).
     * Cette méthode utilise une requête SQL native avec JOINs pour récupérer toutes les
     * désignations en une seule requête, évitant le problème N+1.
     *
     * <p>Performance optimisée : 1 requête SQL au lieu de 5 (1 produit + 4 références).
     */
    public ProduitDetailResponse findByIdWithRelations(Long id) {
        log.debug("Récupération du produit ID: {} avec relations (requête SQL optimisée)", id);

        String sql = """
            SELECT
                p.id, p.codebarre, p.nomcommercial, p.nomscientifique,
                p.fkForme, p.fkDosage, p.fkConditionnement, p.fkCategorie,
                p.prixachat, p.prixachatcomptable, p.qtealert, p.qtcritique, p.perimable,
                p.datecreate AS dateCreate, p.dateupdate AS dateUpdate,
                f.designation as forme_designation,
                d.designation as dosage_designation,
                c.designation as conditionnement_designation,
                ct.designation as categorie_designation
            FROM produits p
            LEFT JOIN formes f ON p.fkForme = f.id
            LEFT JOIN dosages d ON p.fkDosage = d.id
            LEFT JOIN conditionnements c ON p.fkConditionnement = c.id
            LEFT JOIN categorie_produit ct ON p.fkCategorie = ct.id
            WHERE p.id = :id
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        ProduitDetailResponse result = namedJdbcTemplate.query(
                sql,
                params,
                rs -> {
                    if (rs.next()) {
                        return PRODUIT_DETAIL_MAPPER.mapRow(rs, 0);
                    }
                    return null;
                }
        );

        if (result == null) {
            throw NotFoundException.entity("Produit", id);
        }

        return result;
    }

    /**
     * RowMapper pour convertir les résultats SQL en ProduitWithStockResponse.
     * Optimisé pour récupérer produit, stock, péremption et droits en une seule requête.
     */
    private static final RowMapper<ProduitWithStockResponse> PRODUIT_WITH_STOCK_MAPPER = (rs, rowNum) -> {
        ProduitWithStockResponse.ProduitWithStockResponseBuilder builder = ProduitWithStockResponse.builder()
                .id(rs.getLong("id"))
                .codebarre(rs.getString("codebarre"))
                .nomcommercial(rs.getString("nomcommercial"))
                .nomscientifique(rs.getString("nomscientifique"))
                .forme(rs.getString("forme"))
                .dosage(rs.getString("dosage"))
                .conditionnement(rs.getString("conditionnement"))
                .categorie(rs.getString("categorie"))
                .stockId(getLongOrNull(rs, "stockId"))
                .stockencours(getFloatOrNull(rs, "stockencours"))
                .isactif(rs.getBoolean("isactif"))
                .peremption(getStringOrNull(rs, "peremption"))
                .prixachat(getBigDecimalOrNull(rs, "prixachat"))
                .qtealert(getFloatOrNull(rs, "qtealert"))
                .qtcritique(getFloatOrNull(rs, "qtcritique"))
                .perimable(rs.getBoolean("perimable"))
                .dateCreate(getLocalDateTimeOrNull(rs, "dateCreate"));

        return builder.build();
    };

    /**
     * Récupère une page de produits avec stock et péremption, filtrés par droits de l'utilisateur.
     * Cette méthode utilise une requête SQL native optimisée avec JOINs pour récupérer :
     * - Les produits auxquels l'utilisateur a des droits (via droits_categorie)
     * - Le stock en cours (stock_produits)
     * - Les dates de péremption (perimable_alerte_stock)
     * - Les désignations des références (formes, dosages, conditionnements, categories)
     *
     * <p>Performance optimisée : 1 requête SQL avec tous les JOINs nécessaires.
     *
     * @param pharmacieId ID de la pharmacie pour filtrer les droits
     * @param nomcommercial Recherche optionnelle par nom commercial OU nom scientifique OU code-barres (recherche partielle insensible à la casse)
     * @param operationnel Filtre optionnel sur le statut opérationnel (true = actif, false = inactif, null = tous)
     * @param pageable Paramètres de pagination
     * @return Page de ProduitWithStockResponse
     */
    public Page<ProduitWithStockResponse> findProductsWithStock(
            Long pharmacieId,
            String nomcommercial,
            Boolean operationnel,
            Pageable pageable) {
        log.debug("Récupération des produits avec stock pour pharmacieId={}, nomcommercial={}, operationnel={}, page={}, size={}",
                pharmacieId, nomcommercial, operationnel, pageable.getPageNumber(), pageable.getPageSize());

        // Base FROM commune pour COUNT et SELECT - Filtrage direct par pharmacie dans les JOINs pour garantir l'unicité
        String baseFromClause = """
            FROM produits p
            INNER JOIN formes f ON p.fkForme = f.id
            INNER JOIN dosages d ON p.fkDosage = d.id
            INNER JOIN conditionnements c ON p.fkConditionnement = c.id
            INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
            INNER JOIN stock_produits st ON p.id = st.fkProduits AND st.fkPharmacies = :pharmacieId
            INNER JOIN droits_categorie dt ON dt.fkCategorie = ct.id AND dt.fkPharmacie = :pharmacieId
            """;

        // Construction de la clause WHERE commune (une seule fois)
        // Note: Le filtrage par pharmacie est maintenant dans les JOINs pour garantir l'unicité
        StringBuilder whereClause = new StringBuilder("WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        params.put("pharmacieId", pharmacieId);

        // Filtre sur operationnel (sauf recherche code-barres numérique : le scan doit toujours trouver le produit)
        boolean looksLikeBarcode = nomcommercial != null && nomcommercial.trim().matches("\\d{4,}");
        if (operationnel != null) {
            whereClause.append(" AND st.operationnel = :operationnel");
            params.put("operationnel", operationnel);
        } else if (!looksLikeBarcode) {
            // Par défaut, on affiche uniquement les produits actifs (operationnel = 1)
            whereClause.append(" AND st.operationnel = 1");
        }

        // Filtre sur nom commercial OU nom scientifique (LIKE) OU code-barres (= exact, si non null)
        if (nomcommercial != null && !nomcommercial.trim().isEmpty()) {
            String trimmedSearch = nomcommercial.trim();
            String searchTerm = "%" + trimmedSearch.toLowerCase() + "%";
            // Recherche exacte pour code-barres (si non null), recherche partielle pour nom commercial et nom scientifique
            whereClause.append(" AND (LOWER(p.nomcommercial) LIKE :searchTerm OR LOWER(p.nomscientifique) LIKE :searchTerm OR (p.codebarre IS NOT NULL AND p.codebarre = :codebarreExact))");
            params.put("searchTerm", searchTerm);
            params.put("codebarreExact", trimmedSearch);
        }

        String whereClauseStr = whereClause.toString();

        // Requête COUNT: utilise exactement la même base FROM + WHERE
        String countSql = "SELECT COUNT(p.id) " + baseFromClause + " " + whereClauseStr;

        Long total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
        if (total == null) {
            total = 0L;
        }

        // Requête SELECT principale: réutilise baseFromClause + WHERE - DISTINCT pour garantir l'unicité
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                p.id,
                p.codebarre,
                p.nomcommercial,
                p.nomscientifique,
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
            """);

        // Utilise la base FROM commune
        sqlBuilder.append(baseFromClause);

        // LEFT JOIN pour les dates de péremption (n'influe PAS sur le COUNT)
        sqlBuilder.append("""
            LEFT JOIN (
                SELECT
                    fkStock,
                    GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
                FROM perimable_alerte_stock
                WHERE notifactif = TRUE
                GROUP BY fkStock
            ) pa ON pa.fkStock = st.id AND st.qte > 0
            """);

        // Clauses WHERE communes
        sqlBuilder.append(" ").append(whereClauseStr);

        // Tri et pagination
        sqlBuilder.append(" ORDER BY p.nomcommercial ASC");
        sqlBuilder.append(" LIMIT :limit OFFSET :offset");

        // Paramètres de pagination depuis pageable
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        String sql = sqlBuilder.toString();

        // Exécution de la requête
        List<ProduitWithStockResponse> content = namedJdbcTemplate.query(
                sql,
                params,
                PRODUIT_WITH_STOCK_MAPPER
        );

        // Invariant: le nombre d'éléments renvoyés ne doit jamais dépasser pageSize
        if (content.size() > pageable.getPageSize()) {
            log.error("INVARIANT VIOLATED: contentSize={} > pageSize={} for page={}",
                    content.size(), pageable.getPageSize(), pageable.getPageNumber());
        }

        // Log debug pour vérifier comportement réel
        log.debug("PAGE DEBUG: page={}, size={}, contentSize={}, total={}",
                pageable.getPageNumber(), pageable.getPageSize(), content.size(), total);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Récupère une page de produits avec stock et péremption en utilisant la pagination cursor-based.
     * Cette méthode remplace la pagination offset-based par une pagination cursor-based plus performante.
     *
     * <p>
     * <strong>Avantages de la pagination cursor-based :</strong>
     * <ul>
     *   <li>Performance : pas de COUNT(*) coûteux, pas d'OFFSET sur grandes tables</li>
     *   <li>Stabilité : pas de duplication/saut d'éléments lors d'insertions pendant la pagination</li>
     *   <li>Scalabilité : fonctionne bien même avec des millions d'enregistrements</li>
     * </ul>
     *
     * <p>
     * <strong>Clé de tri :</strong> {@code id DESC} (simple, performant, toujours unique)
     *
     * @param pharmacieId ID de la pharmacie pour filtrer les droits
     * @param nomcommercial Recherche optionnelle par nom commercial OU nom scientifique OU code-barres (recherche partielle insensible à la casse)
     * @param operationnel Filtre optionnel sur le statut opérationnel (true = actif, false = inactif, null = tous)
     * @param cursor Cursor pour la pagination (ID du dernier élément, null pour première page)
     * @param limit Nombre d'éléments à retourner (défaut: 20, max: 100)
     * @param perime Filtre sur les produits périmés (true = périmés, false = non périmés, null = tous)
     * @param perimeDansXJours Filtre sur les produits qui vont périmer dans X jours (nombre de jours)
     * @param stockOperator Opérateur de comparaison pour le stock ("gt", "lt", "eq")
     * @param stockValue Valeur de comparaison pour le stock
     * @param prixOperator Opérateur de comparaison pour le prix d'achat ("gt", "lt", "eq")
     * @param prixValue Valeur de comparaison pour le prix d'achat
     * @param perimable Filtre sur les produits périssables (true = périssable, false = non périssable, null = tous)
     * @return CursorPageResponse avec items, nextCursor et hasMore
     */
    public CursorPageResponse<ProduitWithStockResponse> findProductsWithStockCursor(
            Long pharmacieId,
            String nomcommercial,
            Boolean operationnel,
            String cursor,
            Integer limit,
            Boolean perime,
            Integer perimeDansXJours,
            String stockOperator,
            Double stockValue,
            String prixOperator,
            Double prixValue,
            Boolean perimable) {

        // Limite par défaut et max
        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;

        log.debug("Récupération cursor-based des produits avec stock: pharmacieId={}, nomcommercial={}, operationnel={}, cursor={}, limit={}, perime={}, perimeDansXJours={}, stockOperator={}, stockValue={}, prixOperator={}, prixValue={}, perimable={}",
                pharmacieId, nomcommercial, operationnel, cursor, effectiveLimit, perime, perimeDansXJours, stockOperator, stockValue, prixOperator, prixValue, perimable);

        // Base FROM commune
        // Filtrage direct par pharmacie dans les JOINs pour garantir l'unicité
        String baseFromClause = """
            FROM produits p
            INNER JOIN formes f ON p.fkForme = f.id
            INNER JOIN dosages d ON p.fkDosage = d.id
            INNER JOIN conditionnements c ON p.fkConditionnement = c.id
            INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
            INNER JOIN stock_produits st ON p.id = st.fkProduits AND st.fkPharmacies = :pharmacieId
            INNER JOIN droits_categorie dt ON dt.fkCategorie = ct.id AND dt.fkPharmacie = :pharmacieId
            """;

        // Construction de la clause WHERE
        // Note: Le filtrage par pharmacie est maintenant dans les JOINs pour garantir l'unicité
        StringBuilder whereClause = new StringBuilder("WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        params.put("pharmacieId", pharmacieId);

        // Filtre sur operationnel (sauf recherche code-barres numérique)
        boolean looksLikeBarcode = nomcommercial != null && nomcommercial.trim().matches("\\d{4,}");
        if (operationnel != null) {
            whereClause.append(" AND st.operationnel = :operationnel");
            params.put("operationnel", operationnel);
        } else if (!looksLikeBarcode) {
            whereClause.append(" AND st.operationnel = true");
        }

        // Filtre sur nom commercial OU nom scientifique (LIKE) OU code-barres (= exact, si non null)
        if (nomcommercial != null && !nomcommercial.trim().isEmpty()) {
            String trimmedSearch = nomcommercial.trim();
            String searchTerm = "%" + trimmedSearch.toLowerCase() + "%";
            // Recherche exacte pour code-barres (si non null), recherche partielle pour nom commercial et nom scientifique
            whereClause.append(" AND (LOWER(p.nomcommercial) LIKE :searchTerm OR LOWER(p.nomscientifique) LIKE :searchTerm OR (p.codebarre IS NOT NULL AND p.codebarre = :codebarreExact))");
            params.put("searchTerm", searchTerm);
            params.put("codebarreExact", trimmedSearch);
        }

        // Filtre cursor-based : si cursor fourni, filtrer par id (plus simple et performant que dateCreate)
        if (cursor != null && !cursor.trim().isEmpty()) {
            try {
                // Parser le cursor comme ID (numérique)
                Long cursorId = Long.parseLong(cursor.trim());
                whereClause.append(" AND p.id < :cursorId");
                params.put("cursorId", cursorId);
            } catch (NumberFormatException e) {
                log.warn("Cursor invalide (doit être un ID numérique): {}, utilisation de la première page", cursor);
                // Si le cursor est invalide, on ignore et on retourne la première page
            }
        }

        // Filtre sur périssable
        if (perimable != null) {
            whereClause.append(" AND p.perimable = :perimable");
            params.put("perimable", perimable);
        }

        // Filtre sur le stock (supérieur, inférieur, égal)
        if (stockOperator != null && stockValue != null) {
            String op = stockOperator.toLowerCase();
            switch (op) {
                case "gt":
                    whereClause.append(" AND st.qte > :stockValue");
                    params.put("stockValue", stockValue);
                    break;
                case "lt":
                    whereClause.append(" AND st.qte < :stockValue");
                    params.put("stockValue", stockValue);
                    break;
                case "eq":
                    whereClause.append(" AND st.qte = :stockValue");
                    params.put("stockValue", stockValue);
                    break;
                default:
                    log.warn("Opérateur de stock invalide: {}, ignoré", stockOperator);
            }
        }

        // Filtre sur le prix d'achat (supérieur, inférieur, égal)
        if (prixOperator != null && prixValue != null) {
            String op = prixOperator.toLowerCase();
            switch (op) {
                case "gt":
                    whereClause.append(" AND p.prixachat > :prixValue");
                    params.put("prixValue", prixValue);
                    break;
                case "lt":
                    whereClause.append(" AND p.prixachat < :prixValue");
                    params.put("prixValue", prixValue);
                    break;
                case "eq":
                    whereClause.append(" AND p.prixachat = :prixValue");
                    params.put("prixValue", prixValue);
                    break;
                default:
                    log.warn("Opérateur de prix invalide: {}, ignoré", prixOperator);
            }
        }

        // Filtres sur péremption (utilisent des sous-requêtes EXISTS pour optimiser les performances)
        if (perime != null) {
            if (perime) {
                // Produits périmés : au moins une date de péremption <= aujourd'hui
                whereClause.append(" AND EXISTS (SELECT 1 FROM perimable_alerte_stock pas WHERE pas.fkStock = st.id AND pas.notifactif = TRUE AND pas.dateperemtion <= CURDATE())");
            } else {
                // Produits non périmés : pas de date de péremption <= aujourd'hui OU pas de date du tout
                whereClause.append(" AND NOT EXISTS (SELECT 1 FROM perimable_alerte_stock pas WHERE pas.fkStock = st.id AND pas.notifactif = TRUE AND pas.dateperemtion <= CURDATE())");
            }
        }

        if (perimeDansXJours != null && perimeDansXJours > 0) {
            // Produits qui vont périmer dans X jours : date de péremption entre aujourd'hui et aujourd'hui + X jours
            whereClause.append(" AND EXISTS (SELECT 1 FROM perimable_alerte_stock pas WHERE pas.fkStock = st.id AND pas.notifactif = TRUE AND pas.dateperemtion >= CURDATE() AND pas.dateperemtion <= DATE_ADD(CURDATE(), INTERVAL :perimeDansXJours DAY))");
            params.put("perimeDansXJours", perimeDansXJours);
        }

        String whereClauseStr = whereClause.toString();

        // Requête SELECT principale avec dateCreate
        // DISTINCT pour garantir l'unicité
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                p.id,
                p.codebarre,
                p.nomcommercial,
                p.nomscientifique,
                p.datecreate AS dateCreate,
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
            """);

        sqlBuilder.append(baseFromClause);

        // LEFT JOIN pour les dates de péremption
        sqlBuilder.append("""
            LEFT JOIN (
                SELECT
                    fkStock,
                    GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
                FROM perimable_alerte_stock
                WHERE notifactif = TRUE
                GROUP BY fkStock
            ) pa ON pa.fkStock = st.id AND st.qte > 0
            """);

        sqlBuilder.append(" ").append(whereClauseStr);

        // Tri cursor-based : id DESC (simple, performant, toujours unique)
        sqlBuilder.append(" ORDER BY p.id DESC");
        sqlBuilder.append(" LIMIT :limit");
        params.put("limit", effectiveLimit + 1); // Récupérer un élément de plus pour déterminer hasMore

        String sql = sqlBuilder.toString();

        // Exécution de la requête
        List<ProduitWithStockResponse> content = namedJdbcTemplate.query(
                sql,
                params,
                PRODUIT_WITH_STOCK_MAPPER
        );

        // Déterminer hasMore et nextCursor
        boolean hasMore = content.size() > effectiveLimit;
        List<ProduitWithStockResponse> items = hasMore
                ? content.subList(0, effectiveLimit)
                : content;

        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            // Le cursor est l'ID du dernier élément (simple et performant)
            ProduitWithStockResponse lastItem = items.get(items.size() - 1);
            nextCursor = String.valueOf(lastItem.getId());
        }

        log.debug("CURSOR DEBUG: items={}, hasMore={}, nextCursor={}", items.size(), hasMore, nextCursor);

        return CursorPageResponse.of(items, nextCursor, hasMore);
    }

    /**
     * Récupère une page de produits avec stock et péremption en utilisant la pagination page/size.
     * Cette méthode utilise Pageable pour une pagination classique avec support des filtres avancés.
     *
     * <p>
     * <strong>Avantages de la pagination page/size :</strong>
     * <ul>
     *   <li>Simplicité : navigation intuitive avec numéros de page</li>
     *   <li>Total : permet d'afficher le nombre total d'éléments</li>
     *   <li>Navigation : permet de sauter à une page spécifique</li>
     * </ul>
     *
     * @param pharmacieId ID de la pharmacie pour filtrer les droits
     * @param nomcommercial Recherche optionnelle par nom commercial OU nom scientifique OU code-barres
     * @param operationnel Filtre optionnel sur le statut opérationnel (true = actif, false = inactif, null = tous)
     * @param pageable Paramètres de pagination (page, size, sort)
     * @param perime Filtre sur les produits périmés (true = périmés, false = non périmés, null = tous)
     * @param perimeDansXJours Filtre sur les produits qui vont périmer dans X jours
     * @param stockOperator Opérateur de comparaison pour le stock ("gt", "lt", "eq")
     * @param stockValue Valeur de comparaison pour le stock
     * @param prixOperator Opérateur de comparaison pour le prix d'achat ("gt", "lt", "eq")
     * @param prixValue Valeur de comparaison pour le prix d'achat
     * @param perimable Filtre sur les produits périssables (true = périssable, false = non périssable, null = tous)
     * @return PageResponse avec content, page, size, totalElements, totalPages, hasNext, hasPrevious
     */
    public PageResponse<ProduitWithStockResponse> findProductsWithStockPage(
            Long pharmacieId,
            String nomcommercial,
            Boolean operationnel,
            Pageable pageable,
            Boolean perime,
            Integer perimeDansXJours,
            String stockOperator,
            Double stockValue,
            String prixOperator,
            Double prixValue,
            Boolean perimable) {

        log.debug("Récupération page/size des produits avec stock: pharmacieId={}, nomcommercial={}, operationnel={}, page={}, size={}, perime={}, perimeDansXJours={}, stockOperator={}, stockValue={}, prixOperator={}, prixValue={}, perimable={}",
                pharmacieId, nomcommercial, operationnel, pageable.getPageNumber(), pageable.getPageSize(), perime, perimeDansXJours, stockOperator, stockValue, prixOperator, prixValue, perimable);

        try {
            // Base FROM commune - Filtrage direct par pharmacie dans les JOINs pour garantir l'unicité
            String baseFromClause = """
                FROM produits p
                INNER JOIN formes f ON p.fkForme = f.id
                INNER JOIN dosages d ON p.fkDosage = d.id
                INNER JOIN conditionnements c ON p.fkConditionnement = c.id
                INNER JOIN categorie_produit ct ON p.fkCategorie = ct.id
                INNER JOIN stock_produits st ON p.id = st.fkProduits AND st.fkPharmacies = :pharmacieId
                INNER JOIN droits_categorie dt ON dt.fkCategorie = ct.id AND dt.fkPharmacie = :pharmacieId
                """;

            // Construction de la clause WHERE
            // Note: Le filtrage par pharmacie est maintenant dans les JOINs pour garantir l'unicité
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            Map<String, Object> params = new HashMap<>();
            params.put("pharmacieId", pharmacieId);

            // Filtre sur operationnel (sauf recherche code-barres numérique)
            boolean looksLikeBarcode = nomcommercial != null && nomcommercial.trim().matches("\\d{4,}");
            if (operationnel != null) {
                whereClause.append(" AND st.operationnel = :operationnel");
                params.put("operationnel", operationnel);
            } else if (!looksLikeBarcode) {
                whereClause.append(" AND st.operationnel = 1");
            }

            // Filtre sur nom commercial OU nom scientifique OU code-barres
            if (nomcommercial != null && !nomcommercial.trim().isEmpty()) {
                String trimmedSearch = nomcommercial.trim();
                String searchTerm = "%" + trimmedSearch.toLowerCase() + "%";
                whereClause.append(" AND (LOWER(p.nomcommercial) LIKE :searchTerm OR LOWER(p.nomscientifique) LIKE :searchTerm OR (p.codebarre IS NOT NULL AND p.codebarre = :codebarreExact))");
                params.put("searchTerm", searchTerm);
                params.put("codebarreExact", trimmedSearch);
            }

            // Filtre sur périssable
            if (perimable != null) {
                whereClause.append(" AND p.perimable = :perimable");
                params.put("perimable", perimable);
            }

            // Filtre sur le stock
            if (stockOperator != null && stockValue != null) {
                String op = stockOperator.toLowerCase();
                switch (op) {
                    case "gt":
                        whereClause.append(" AND st.qte > :stockValue");
                        params.put("stockValue", stockValue);
                        break;
                    case "lt":
                        whereClause.append(" AND st.qte < :stockValue");
                        params.put("stockValue", stockValue);
                        break;
                    case "eq":
                        whereClause.append(" AND st.qte = :stockValue");
                        params.put("stockValue", stockValue);
                        break;
                    default:
                        log.warn("Opérateur de stock invalide: {}, ignoré", stockOperator);
                }
            }

            // Filtre sur le prix d'achat
            if (prixOperator != null && prixValue != null) {
                String op = prixOperator.toLowerCase();
                switch (op) {
                    case "gt":
                        whereClause.append(" AND p.prixachat > :prixValue");
                        params.put("prixValue", prixValue);
                        break;
                    case "lt":
                        whereClause.append(" AND p.prixachat < :prixValue");
                        params.put("prixValue", prixValue);
                        break;
                    case "eq":
                        whereClause.append(" AND p.prixachat = :prixValue");
                        params.put("prixValue", prixValue);
                        break;
                    default:
                        log.warn("Opérateur de prix invalide: {}, ignoré", prixOperator);
                }
            }

            // Filtres sur péremption
            if (perime != null) {
                if (perime) {
                    whereClause.append(" AND EXISTS (SELECT 1 FROM perimable_alerte_stock pas WHERE pas.fkStock = st.id AND pas.notifactif = TRUE AND pas.dateperemtion <= CURDATE())");
                } else {
                    whereClause.append(" AND NOT EXISTS (SELECT 1 FROM perimable_alerte_stock pas WHERE pas.fkStock = st.id AND pas.notifactif = TRUE AND pas.dateperemtion <= CURDATE())");
                }
            }

            if (perimeDansXJours != null && perimeDansXJours > 0) {
                whereClause.append(" AND EXISTS (SELECT 1 FROM perimable_alerte_stock pas WHERE pas.fkStock = st.id AND pas.notifactif = TRUE AND pas.dateperemtion >= CURDATE() AND pas.dateperemtion <= DATE_ADD(CURDATE(), INTERVAL :perimeDansXJours DAY))");
                params.put("perimeDansXJours", perimeDansXJours);
            }

            String whereClauseStr = whereClause.toString();

            // Requête COUNT - Utilise COUNT(DISTINCT p.id) pour garantir l'unicité
            String countSql = "SELECT COUNT(DISTINCT p.id) " + baseFromClause + " " + whereClauseStr;
            Long total;
            try {
                log.debug("Exécution de la requête COUNT: SQL={}, params={}", countSql, params);
                total = namedJdbcTemplate.queryForObject(countSql, params, Long.class);
                if (total == null) {
                    total = 0L;
                }
                log.debug("Requête COUNT réussie: total={}", total);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                log.warn("COUNT query returned no result, defaulting to 0. SQL: {}", countSql);
                total = 0L;
            } catch (org.springframework.dao.DataAccessException e) {
                log.error("Erreur DataAccess lors de l'exécution de la requête COUNT: SQL={}, params={}, cause={}",
                        countSql, params, e.getCause() != null ? e.getCause().getMessage() : "N/A", e);
                throw new RuntimeException("Erreur lors du comptage des produits: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Erreur inattendue lors de l'exécution de la requête COUNT: SQL={}, params={}, type={}, message={}",
                        countSql, params, e.getClass().getName(), e.getMessage(), e);
                throw new RuntimeException("Erreur lors du comptage des produits: " + e.getMessage(), e);
            }

            // Requête SELECT principale - DISTINCT pour garantir l'unicité
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT DISTINCT
                    p.id,
                    p.codebarre,
                    p.nomcommercial,
                    p.nomscientifique,
                    p.datecreate AS dateCreate,
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
                """);

            sqlBuilder.append(baseFromClause);

            // LEFT JOIN pour les dates de péremption
            sqlBuilder.append("""
                LEFT JOIN (
                    SELECT
                        fkStock,
                        GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
                    FROM perimable_alerte_stock
                    WHERE notifactif = TRUE
                    GROUP BY fkStock
                ) pa ON pa.fkStock = st.id AND st.qte > 0
                """);

            sqlBuilder.append(" ").append(whereClauseStr);

            // Tri et pagination
            sqlBuilder.append(" ORDER BY p.nomcommercial ASC");
            sqlBuilder.append(" LIMIT :limit OFFSET :offset");

            params.put("limit", pageable.getPageSize());
            params.put("offset", pageable.getOffset());

            String sql = sqlBuilder.toString();

            // Exécution de la requête
            List<ProduitWithStockResponse> content;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Exécution de la requête SELECT: SQL={}, params={}", sql, params);
                }
                content = namedJdbcTemplate.query(
                        sql,
                        params,
                        PRODUIT_WITH_STOCK_MAPPER
                );
                if (log.isDebugEnabled()) {
                    log.debug("Requête SELECT réussie: {} produits récupérés", content.size());
                }
            } catch (org.springframework.dao.DataAccessException e) {
                String errorMessage = "Erreur DataAccess lors de l'exécution de la requête SELECT";
                String causeMessage = e.getCause() != null ? e.getCause().getMessage() : "N/A";

                log.error("{}: SQL={}, params={}, cause={}, exceptionType={}",
                        errorMessage, sql, params, causeMessage, e.getClass().getName(), e);

                // Si c'est une erreur SQL spécifique, l'inclure dans le message
                if (causeMessage != null && (causeMessage.contains("Unknown column") || causeMessage.contains("doesn't exist"))) {
                    throw new RuntimeException(
                            String.format("%s: %s. Vérifiez que la colonne existe dans la base de données.",
                                    errorMessage, causeMessage), e);
                }

                throw new RuntimeException(errorMessage + ": " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Erreur inattendue lors de l'exécution de la requête SELECT: SQL={}, params={}, type={}, message={}",
                        sql, params, e.getClass().getName(), e.getMessage(), e);
                throw new RuntimeException("Erreur lors de la récupération des produits: " + e.getMessage(), e);
            }

            // Validation: S'assurer que le nombre d'éléments retournés ne dépasse pas le total disponible
            long offset = pageable.getOffset();
            if (offset >= total && !content.isEmpty()) {
                log.warn("PAGE WARNING: offset={} >= total={} mais content.size()={}, vidage de la liste",
                        offset, total, content.size());
                content = List.of();
            }

            // Validation: Sur la dernière page uniquement, limiter le nombre d'éléments au reste disponible
            // Exemple: total=809, size=20, page=40 (dernière) → offset=800, reste=9 → on ne doit retourner que 9 éléments
            // Mais sur les pages intermédiaires, on doit retourner exactement 'size' éléments
            if (offset < total && !content.isEmpty()) {
                long remainingElements = total - offset;
                boolean isLastPage = remainingElements <= pageable.getPageSize();

                // Tronquer uniquement sur la dernière page si nécessaire
                if (isLastPage && content.size() > remainingElements) {
                    log.warn("PAGE WARNING: Dernière page - content.size()={} > remainingElements={}, limitation à {} éléments",
                            content.size(), remainingElements, remainingElements);
                    content = content.subList(0, (int) remainingElements);
                }

                // Si ce n'est pas la dernière page mais qu'on a moins d'éléments que prévu, logger un avertissement
                if (!isLastPage && content.size() < pageable.getPageSize()) {
                    log.warn("PAGE WARNING: Page intermédiaire - content.size()={} < pageSize={}, total={}, offset={}. " +
                            "La requête SQL n'a pas retourné assez d'éléments.",
                            content.size(), pageable.getPageSize(), total, offset);
                }
            }

            log.debug("PAGE DEBUG: page={}, size={}, offset={}, contentSize={}, total={}, remainingElements={}",
                    pageable.getPageNumber(), pageable.getPageSize(), offset, content.size(), total,
                    offset < total ? total - offset : 0);

            // Convertir en PageResponse
            return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
        } catch (RuntimeException e) {
            // Re-lancer les RuntimeException (déjà loggées)
            throw e;
        } catch (Exception e) {
            log.error("Erreur inattendue dans findProductsWithStockPage: pharmacieId={}, page={}, size={}",
                    pharmacieId, pageable.getPageNumber(), pageable.getPageSize(), e);
            throw new RuntimeException("Erreur lors de la récupération des produits avec stock: " + e.getMessage(), e);
        }
    }

    /**
     * Convertit une Produit (domain) en ProduitResponse (DTO).
     */
    private ProduitResponse produitToResponse(Produit produit) {
        if (produit == null) {
            return null;
        }

        return ProduitResponse.builder()
                .id(produit.getId())
                .codebarre(produit.getCodebarre())
                .nomcommercial(produit.getNomcommercial())
                .nomscientifique(produit.getNomscientifique())
                .fkForme(produit.getFkForme())
                .fkDosage(produit.getFkDosage())
                .fkConditionnement(produit.getFkConditionnement())
                .fkCategorie(produit.getFkCategorie())
                .prixachat(produit.getPrixachat())
                .prixachatcomptable(produit.getPrixachatcomptable())
                .qtealert(produit.getQtealert())
                .qtcritique(produit.getQtcritique())
                .perimable(produit.getPerimable())
                .dateCreate(produit.getDateCreate())
                .dateUpdate(produit.getDateUpdate())
                .build();
    }

}

