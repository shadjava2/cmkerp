package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse d'analyse du slow query log avec recommandations d'index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowQueryAnalysisResponse {

  private List<SlowQueryInfo> topSlowQueries;
  private List<IndexRecommendation> indexRecommendations;
  private AnalysisSummary summary;

  /**
   * Informations sur une requête lente.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SlowQueryInfo {
    private String query;
    private Long executionCount;
    private Double avgExecutionTimeMs;
    private Double maxExecutionTimeMs;
    private Double totalExecutionTimeMs;
    private Long rowsExamined;
    private Long rowsSent;
    private String explainPlan;
    private String tableName;
    private String indexRecommendation;
  }

  /**
   * Recommandation d'index composite.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class IndexRecommendation {
    private String tableName;
    private String indexName;
    private List<String> columns;
    private String sqlStatement;
    private String reason;
    private Integer estimatedImpact; // 1-5 (5 = très élevé)
    private List<String> affectedQueries; // Requêtes qui bénéficieraient de cet index
  }

  /**
   * Résumé de l'analyse.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AnalysisSummary {
    private Integer totalQueriesAnalyzed;
    private Integer queriesWithoutIndex;
    private Integer queriesWithFullTableScan;
    private Integer recommendedIndexes;
    private Double avgExecutionTimeMs;
    private Double maxExecutionTimeMs;
  }
}
