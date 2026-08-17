package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Configuration pour l'optimisation des requêtes SQL.
 *
 * <p>
 * Fournit des méthodes utilitaires pour analyser et optimiser les requêtes SQL.
 * Utilise EXPLAIN pour analyser les plans d'exécution et suggérer des optimisations.
 *
 * <p>
 * Facebook-Grade: Monitoring et optimisation proactive des requêtes.
 *
 */
@Configuration
@ConditionalOnProperty(name = "cmkerp.query-optimization.enabled", havingValue = "true", matchIfMissing = true)
public class QueryOptimizationConfig {

    private static final Logger log = LoggerFactory.getLogger(QueryOptimizationConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public QueryOptimizationConfig(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Détecte le type de base de données (MySQL, etc.).
     *
     * @return le nom du driver ou "unknown"
     */
    private String detectDatabaseType() {
        try {
            String jdbcUrl = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
            if (jdbcUrl != null) {
                if (jdbcUrl.startsWith("jdbc:mysql:") || jdbcUrl.startsWith("jdbc:tc:mysql:")) {
                    return "MySQL";
                }
            }
        } catch (Exception e) {
            log.debug("Impossible de détecter le type de base de données", e);
        }
        return "unknown";
    }

    /**
     * Analyse une requête SQL avec EXPLAIN.
     *
     * <p>
     * Utile pour identifier les requêtes lentes et optimiser les index.
     *
     * @param sql la requête SQL à analyser
     * @param args les paramètres de la requête
     * @return le résultat de EXPLAIN
     */
    public String explainQuery(String sql, Object... args) {
        String explainSql = "EXPLAIN " + sql;
        try {
            return jdbcTemplate.queryForObject(explainSql, String.class, args);
        } catch (Exception e) {
            log.error("Erreur lors de l'analyse EXPLAIN pour: {}", sql, e);
            return null;
        }
    }

    /**
     * Vérifie si un index existe sur une table.
     *
     * @param tableName le nom de la table
     * @param indexName le nom de l'index
     * @return true si l'index existe
     */
    public boolean indexExists(String tableName, String indexName) {
        String dbType = detectDatabaseType();
        if (!"MySQL".equals(dbType)) {
            log.debug("Type de base de données non supporté pour la vérification d'index: {}", dbType);
            return false;
        }
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.statistics " +
                    "WHERE table_schema = DATABASE() " +
                    "AND table_name = ? " +
                    "AND index_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, indexName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification d'index pour {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Log les index existants pour une table (utile pour debugging).
     *
     * @param tableName le nom de la table
     */
    public void logTableIndexes(String tableName) {
        String dbType = detectDatabaseType();
        if (!"MySQL".equals(dbType)) {
            log.debug("Type de base de données non supporté pour le log des index: {}", dbType);
            return;
        }
        try {
            String sql = "SELECT index_name, column_name, seq_in_index " +
                    "FROM information_schema.statistics " +
                    "WHERE table_schema = DATABASE() " +
                    "AND table_name = ? " +
                    "ORDER BY index_name, seq_in_index";
            jdbcTemplate.query(sql, (rs) -> {
                log.info("Index: {} - Column: {} - Position: {}",
                        rs.getString("index_name"),
                        rs.getString("column_name"),
                        rs.getInt("seq_in_index"));
            }, tableName);
        } catch (Exception e) {
            log.debug("Impossible de lister les index pour {}: {}", tableName, e.getMessage());
        }
    }

    /**
     * Initialisation: Log les index critiques au démarrage.
     */
    @PostConstruct
    public void logCriticalIndexes() {
        if (log.isDebugEnabled()) {
            log.debug("Vérification des index critiques...");
            logTableIndexes("utilisateurs");
            logTableIndexes("roles_permissions");
            logTableIndexes("utilisateurs_permissions");
        }
    }
}

