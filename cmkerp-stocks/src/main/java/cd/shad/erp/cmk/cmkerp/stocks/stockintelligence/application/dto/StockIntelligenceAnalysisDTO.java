package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Réponse structurée attendue de l'IA (expert gestion stock).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StockIntelligenceAnalysisDTO(
    String resumeDirection,
    String niveauRisque,
    /** Synthèse exécutive style cabinet (2–4 phrases). */
    String syntheseExecutive,
    List<AlerteIaDTO> alertes,
    List<String> recommandations,
    String commentaireExpert,
    /** Perspectives 7–30 jours basées sur tendances observées. */
    List<String> perspectives,
    /** Risques anticipés si inaction (rupture, surstock, obsolescence). */
    List<String> anticipationRisques,
    /** Actions concrètes sous 48 h pour la direction achats / pharmacie. */
    List<String> actionsPrioritaires48h,
    List<CategorieCommentaireDTO> commentairesParCategorie
) {
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record AlerteIaDTO(
      String produit,
      String risque,
      String urgence,
      String cause,
      String action
  ) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record CategorieCommentaireDTO(
      String categorie,
      String analyse,
      List<String> points_cles
  ) {}
}
