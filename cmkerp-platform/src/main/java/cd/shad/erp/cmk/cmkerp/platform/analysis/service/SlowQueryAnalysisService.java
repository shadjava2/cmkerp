package cd.shad.erp.cmk.cmkerp.platform.analysis.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.SlowQueryAnalysisResponse;

/**
 * Service d'analyse du slow query log pour identifier les requêtes lentes et recommander des index
 * composites.
 *
 * <p>
 * Ce service analyse les requêtes depuis performance_schema (MySQL 5.7+) ou peut parser le slow
 * query log pour identifier :
 * <ul>
 * <li>Les top 20 requêtes les plus lentes</li>
 * <li>Les requêtes avec table scan complet</li>
 * <li>Les requêtes sans index approprié</li>
 * <li>Les recommandations d'index composites</li>
 * </ul>
 */
@Service
public class SlowQueryAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(SlowQueryAnalysisService.class);

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  @Value("${cmkerp.analysis.slow-query.limit:20}")
  private int queryLimit;

  public SlowQueryAnalysisService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
      @Qualifier("primaryDataSource") DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  /**
   * Analyse le top 20 des requêtes lentes et génère des recommandations d'index.
   */
  public SlowQueryAnalysisResponse analyzeTopSlowQueries() {
    log.info("Début de l'analyse du top {} requêtes lentes", queryLimit);

    try {
      List<SlowQueryAnalysisResponse.SlowQueryInfo> slowQueries = getTopSlowQueries();
      List<SlowQueryAnalysisResponse.IndexRecommendation> recommendations =
          generateIndexRecommendations(slowQueries);

      SlowQueryAnalysisResponse.AnalysisSummary summary =
          calculateSummary(slowQueries, recommendations);

      return SlowQueryAnalysisResponse.builder().topSlowQueries(slowQueries)
          .indexRecommendations(recommendations).summary(summary).build();

    } catch (Exception e) {
      log.error("Erreur lors de l'analyse des requêtes lentes : {}", e.getMessage(), e);
      throw new RuntimeException("Erreur lors de l'analyse des requêtes lentes", e);
    }
  }

  /**
   * Récupère le top N des requêtes lentes depuis performance_schema.
   */
  private List<SlowQueryAnalysisResponse.SlowQueryInfo> getTopSlowQueries() {
    List<SlowQueryAnalysisResponse.SlowQueryInfo> queries = new ArrayList<>();

    try {
      // Vérifier si performance_schema est disponible
      String sql = """
          SELECT
            digest_text AS query,
            count_star AS execution_count,
            ROUND(sum_timer_wait / 1000000000000, 2) AS total_execution_time_sec,
            ROUND(avg_timer_wait / 1000000000000, 2) AS avg_execution_time_sec,
            ROUND(max_timer_wait / 1000000000000, 2) AS max_execution_time_sec,
            sum_rows_examined AS rows_examined,
            sum_rows_sent AS rows_sent
          FROM performance_schema.events_statements_summary_by_digest
          WHERE digest_text IS NOT NULL
            AND digest_text NOT LIKE 'SHOW%'
            AND digest_text NOT LIKE 'SELECT%performance_schema%'
            AND sum_timer_wait > 0
          ORDER BY sum_timer_wait DESC
          LIMIT ?
          """;

      List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, queryLimit);

      for (Map<String, Object> row : results) {
        String query = (String) row.get("query");
        Long executionCount = ((Number) row.get("execution_count")).longValue();
        Double totalTimeSec = ((Number) row.get("total_execution_time_sec")).doubleValue();
        Double avgTimeSec = ((Number) row.get("avg_execution_time_sec")).doubleValue();
        Double maxTimeSec = ((Number) row.get("max_execution_time_sec")).doubleValue();
        Long rowsExamined = ((Number) row.get("rows_examined")).longValue();
        Long rowsSent = ((Number) row.get("rows_sent")).longValue();

        // Normaliser la requête pour l'analyse
        String normalizedQuery = normalizeQuery(query);

        // Exécuter EXPLAIN
        String explainPlan = explainQuery(normalizedQuery);

        // Extraire le nom de la table
        String tableName = extractTableName(normalizedQuery);

        SlowQueryAnalysisResponse.SlowQueryInfo queryInfo = SlowQueryAnalysisResponse.SlowQueryInfo
            .builder().query(query).executionCount(executionCount)
            .avgExecutionTimeMs(avgTimeSec * 1000).maxExecutionTimeMs(maxTimeSec * 1000)
            .totalExecutionTimeMs(totalTimeSec * 1000).rowsExamined(rowsExamined).rowsSent(rowsSent)
            .explainPlan(explainPlan).tableName(tableName).build();

        queries.add(queryInfo);
      }

      log.info("Récupération de {} requêtes lentes depuis performance_schema", queries.size());
      return queries;

    } catch (Exception e) {
      log.warn("Impossible d'utiliser performance_schema, tentative avec information_schema : {}",
          e.getMessage());
      // Fallback : utiliser information_schema.processlist (limité)
      return getTopSlowQueriesFallback();
    }
  }

  /**
   * Méthode fallback si performance_schema n'est pas disponible.
   */
  private List<SlowQueryAnalysisResponse.SlowQueryInfo> getTopSlowQueriesFallback() {
    log.warn("performance_schema non disponible, analyse limitée");
    return new ArrayList<>();
  }

  /**
   * Exécute EXPLAIN sur une requête normalisée.
   */
  private String explainQuery(String query) {
    try {
      // Ne pas exécuter EXPLAIN sur des requêtes non-SELECT (INSERT, UPDATE, DELETE)
      String trimmedQuery = query.trim().toUpperCase();
      if (!trimmedQuery.startsWith("SELECT")) {
        return "EXPLAIN non applicable pour les requêtes non-SELECT";
      }

      // Limiter la longueur de la requête pour EXPLAIN (éviter les requêtes trop complexes)
      if (query.length() > 5000) {
        return "Requête trop longue pour EXPLAIN (>" + query.length() + " caractères)";
      }

      String explainQuery = "EXPLAIN " + query;

      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement();
          ResultSet rs = statement.executeQuery(explainQuery)) {

        StringBuilder explain = new StringBuilder();
        explain.append(
            "id\tselect_type\ttable\ttype\tpossible_keys\tkey\tkey_len\tref\trows\tExtra\n");

        while (rs.next()) {
          explain.append(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
              getStringOrNull(rs, "id"), getStringOrNull(rs, "select_type"),
              getStringOrNull(rs, "table"), getStringOrNull(rs, "type"),
              getStringOrNull(rs, "possible_keys"), getStringOrNull(rs, "key"),
              getStringOrNull(rs, "key_len"), getStringOrNull(rs, "ref"),
              getStringOrNull(rs, "rows"), getStringOrNull(rs, "Extra")));
        }

        return explain.toString();
      }
    } catch (Exception e) {
      log.warn("Impossible d'exécuter EXPLAIN pour la requête : {}", e.getMessage());
      return "EXPLAIN erreur : " + e.getMessage();
    }
  }

  /**
   * Récupère une valeur String depuis ResultSet ou retourne null si absente.
   */
  private String getStringOrNull(ResultSet rs, String columnName) {
    try {
      return rs.getString(columnName);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Normalise une requête SQL (enlève les valeurs, standardise les espaces).
   */
  private String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return query;
    }

    // Remplacer les valeurs numériques par ?
    query = query.replaceAll("\\b\\d+\\b", "?");
    // Remplacer les chaînes entre quotes par ?
    query = query.replaceAll("'[^']*'", "?");
    query = query.replaceAll("\"[^\"]*\"", "?");
    // Standardiser les espaces
    query = query.replaceAll("\\s+", " ").trim();

    return query;
  }

  /**
   * Extrait le nom de la table principale d'une requête SELECT.
   */
  private String extractTableName(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }

    // Pattern pour trouver FROM table_name
    Pattern pattern = Pattern.compile("FROM\\s+([`]?)(\\w+)\\1", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      return matcher.group(2);
    }

    return null;
  }

  /**
   * Génère des recommandations d'index composites basées sur l'analyse EXPLAIN.
   */
  private List<SlowQueryAnalysisResponse.IndexRecommendation> generateIndexRecommendations(
      List<SlowQueryAnalysisResponse.SlowQueryInfo> queries) {
    Map<String, IndexAnalysis> tableAnalyses = new HashMap<>();

    // Analyser chaque requête
    for (SlowQueryAnalysisResponse.SlowQueryInfo query : queries) {
      if (query.getTableName() == null) {
        continue;
      }

      String tableName = query.getTableName();
      IndexAnalysis analysis = tableAnalyses.computeIfAbsent(tableName, k -> new IndexAnalysis());

      // Analyser le plan d'exécution
      if (query.getExplainPlan() != null) {
        String explainPlan = query.getExplainPlan().toUpperCase();
        if (explainPlan.contains("ALL") || explainPlan.contains("FULL TABLE SCAN")
            || explainPlan.contains("USING WHERE") && !explainPlan.contains("USING INDEX")) {
          analysis.fullTableScans++;
        }
      }

      // Extraire les colonnes WHERE de la requête
      List<String> whereColumns = extractWhereColumns(query.getQuery());
      if (!whereColumns.isEmpty()) {
        analysis.whereColumns.addAll(whereColumns);
        // Compter la fréquence des colonnes
        for (String col : whereColumns) {
          analysis.columnFrequency.put(col, analysis.columnFrequency.getOrDefault(col, 0) + 1);
        }
        analysis.queryCount++;
      }
    }

    // Générer les recommandations
    List<SlowQueryAnalysisResponse.IndexRecommendation> recommendations = new ArrayList<>();

    for (Map.Entry<String, IndexAnalysis> entry : tableAnalyses.entrySet()) {
      String tableName = entry.getKey();
      IndexAnalysis analysis = entry.getValue();

      if (analysis.fullTableScans > 0 && !analysis.whereColumns.isEmpty()) {
        // Créer un index composite recommandé
        List<String> recommendedColumns =
            getRecommendedColumnOrder(analysis.whereColumns, tableName, analysis.columnFrequency);

        if (!recommendedColumns.isEmpty()) {
          String indexName = generateIndexName(tableName, recommendedColumns);
          String sqlStatement = generateIndexSQL(tableName, indexName, recommendedColumns);

          SlowQueryAnalysisResponse.IndexRecommendation recommendation =
              SlowQueryAnalysisResponse.IndexRecommendation.builder().tableName(tableName)
                  .indexName(indexName).columns(recommendedColumns).sqlStatement(sqlStatement)
                  .reason(
                      String.format("Table scan détecté (%d fois). Colonnes WHERE fréquentes : %s",
                          analysis.fullTableScans, String.join(", ", recommendedColumns)))
                  .estimatedImpact(calculateImpact(analysis.fullTableScans, analysis.queryCount))
                  .affectedQueries(findAffectedQueries(queries, tableName)).build();

          recommendations.add(recommendation);
        }
      }
    }

    log.info("Génération de {} recommandations d'index", recommendations.size());
    return recommendations;
  }

  /**
   * Classe interne pour analyser une table.
   */
  private static class IndexAnalysis {
    int fullTableScans = 0;
    int queryCount = 0;
    Set<String> whereColumns = new HashSet<>();
    Map<String, Integer> columnFrequency = new HashMap<>();
  }

  /**
   * Extrait les colonnes utilisées dans la clause WHERE.
   */
  private List<String> extractWhereColumns(String query) {
    List<String> columns = new ArrayList<>();
    if (query == null || query.isBlank()) {
      return columns;
    }

    // Pattern amélioré pour trouver WHERE column_name =, >, <, LIKE, IN, etc.
    // Gère : WHERE col = ?, WHERE col > ?, WHERE col LIKE ?, WHERE col IN (?), etc.
    Pattern pattern = Pattern.compile(
        "(?:WHERE|AND|OR)\\s+([`]?)(\\w+)\\1\\s*(?:[=<>!]+|LIKE|IN)\\s*", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);

    while (matcher.find()) {
      String column = matcher.group(2).trim();
      // Exclure les mots-clés SQL communs
      if (!column.isEmpty() && !column.equalsIgnoreCase("SELECT")
          && !column.equalsIgnoreCase("FROM") && !column.equalsIgnoreCase("WHERE")
          && !column.equalsIgnoreCase("AND") && !column.equalsIgnoreCase("OR")
          && !column.equals("?")) {
        columns.add(column);
      }
    }

    return columns;
  }

  /**
   * Détermine l'ordre recommandé des colonnes pour un index composite.
   */
  private List<String> getRecommendedColumnOrder(Set<String> columns, String tableName,
      Map<String, Integer> columnFrequency) {
    // Prioriser les colonnes par fréquence d'utilisation (plus fréquentes en premier)
    // Vérifier les index existants pour éviter les doublons
    Set<String> existingColumns = getExistingIndexColumns(tableName);

    List<String> recommended = new ArrayList<>(columns);
    recommended.removeAll(existingColumns); // Exclure les colonnes déjà indexées

    if (recommended.isEmpty()) {
      return recommended;
    }

    // Trier par fréquence décroissante
    recommended.sort((a, b) -> {
      int freqA = columnFrequency.getOrDefault(a, 0);
      int freqB = columnFrequency.getOrDefault(b, 0);
      return Integer.compare(freqB, freqA); // Décroissant
    });

    // Limiter à 3-4 colonnes max pour un index composite efficace
    if (recommended.size() > 4) {
      recommended = recommended.subList(0, 4);
    }

    return recommended;
  }

  /**
   * Récupère les colonnes déjà indexées pour une table.
   */
  private Set<String> getExistingIndexColumns(String tableName) {
    Set<String> columns = new HashSet<>();
    try {
      String sql = """
          SELECT DISTINCT column_name
          FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = ?
            AND index_name != 'PRIMARY'
          """;

      List<String> existing = jdbcTemplate.queryForList(sql, String.class, tableName);
      columns.addAll(existing);
    } catch (Exception e) {
      log.warn("Impossible de récupérer les index existants pour {} : {}", tableName,
          e.getMessage());
    }
    return columns;
  }

  /**
   * Génère un nom d'index basé sur la table et les colonnes.
   */
  private String generateIndexName(String tableName, List<String> columns) {
    String columnPrefix = String.join("_", columns.subList(0, Math.min(3, columns.size())));
    return String.format("idx_%s_%s", tableName, columnPrefix).replaceAll("[^a-zA-Z0-9_]", "_");
  }

  /**
   * Génère la déclaration SQL CREATE INDEX.
   */
  private String generateIndexSQL(String tableName, String indexName, List<String> columns) {
    String columnsList = String.join(", ", columns);
    return String.format("CREATE INDEX `%s` ON `%s` (%s);", indexName, tableName, columnsList);
  }

  /**
   * Calcule l'impact estimé (1-5).
   */
  private Integer calculateImpact(int fullTableScans, int queryCount) {
    if (fullTableScans > 10 && queryCount > 50) {
      return 5; // Très élevé
    } else if (fullTableScans > 5 && queryCount > 20) {
      return 4; // Élevé
    } else if (fullTableScans > 2) {
      return 3; // Modéré
    } else {
      return 2; // Faible
    }
  }

  /**
   * Trouve les requêtes affectées par cette recommandation.
   */
  private List<String> findAffectedQueries(List<SlowQueryAnalysisResponse.SlowQueryInfo> queries,
      String tableName) {
    List<String> affected = new ArrayList<>();
    for (SlowQueryAnalysisResponse.SlowQueryInfo query : queries) {
      if (tableName.equals(query.getTableName()) && query.getQuery() != null) {
        String normalized = normalizeQuery(query.getQuery());
        if (normalized.length() > 100) {
          normalized = normalized.substring(0, 100) + "...";
        }
        affected.add(normalized);
      }
    }
    return affected;
  }

  /**
   * Calcule le résumé de l'analyse.
   */
  private SlowQueryAnalysisResponse.AnalysisSummary calculateSummary(
      List<SlowQueryAnalysisResponse.SlowQueryInfo> queries,
      List<SlowQueryAnalysisResponse.IndexRecommendation> recommendations) {

    int queriesWithoutIndex = 0;
    int queriesWithFullTableScan = 0;
    double totalTime = 0;
    double maxTime = 0;

    for (SlowQueryAnalysisResponse.SlowQueryInfo query : queries) {
      if (query.getExplainPlan() != null) {
        if (query.getExplainPlan().contains("ALL")
            || query.getExplainPlan().contains("full table scan")) {
          queriesWithFullTableScan++;
          queriesWithoutIndex++;
        } else if (query.getExplainPlan().contains("NULL")
            || query.getExplainPlan().contains("Using where")) {
          queriesWithoutIndex++;
        }
      }

      if (query.getAvgExecutionTimeMs() != null) {
        totalTime += query.getAvgExecutionTimeMs();
        if (query.getMaxExecutionTimeMs() != null && query.getMaxExecutionTimeMs() > maxTime) {
          maxTime = query.getMaxExecutionTimeMs();
        }
      }
    }

    double avgTime = queries.isEmpty() ? 0 : totalTime / queries.size();

    return SlowQueryAnalysisResponse.AnalysisSummary.builder().totalQueriesAnalyzed(queries.size())
        .queriesWithoutIndex(queriesWithoutIndex).queriesWithFullTableScan(queriesWithFullTableScan)
        .recommendedIndexes(recommendations.size()).avgExecutionTimeMs(avgTime)
        .maxExecutionTimeMs(maxTime).build();
  }
}
