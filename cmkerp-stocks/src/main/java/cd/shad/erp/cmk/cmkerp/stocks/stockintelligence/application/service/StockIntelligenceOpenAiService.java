package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceAnalysisDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceReportType;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockIntelligenceMultiSnapshotDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.openai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockIntelligenceOpenAiService {

  private static final String JSON_SCHEMA = """
      Réponds UNIQUEMENT en JSON valide selon ce schéma :
      {
        "resume_direction": "string — message direction (achats / pharmacie centrale)",
        "niveau_risque": "faible|modere|eleve|critique",
        "synthese_executive": "string — 2 à 4 phrases style cabinet de conseil",
        "alertes": [{"produit":"string","risque":"string","urgence":"info|attention|critique","cause":"string","action":"string"}],
        "recommandations": ["string"],
        "commentaire_expert": "string",
        "perspectives": ["string — horizon 7j", "string — horizon 30j"],
        "anticipation_risques": ["string"],
        "actions_prioritaires_48h": ["string"],
        "commentaires_par_categorie": [
          {"categorie":"AVEC_MOUVEMENT|STOCK_SANS_MOUVEMENT|RUPTURE_SANS_MOUVEMENT","analyse":"string","points_cles":["string"]}
        ]
      }
      Règles : n'invente aucun produit absent du JSON ; cite des chiffres réels ;
      compare mois en cours vs mois précédent.
      """;

  private final OpenAiClient openAiClient;
  private final StockMovementAnalyticsService analyticsService;
  private final StockIntelligenceProperties properties;
  private final ObjectMapper objectMapper;

  public StockIntelligenceAnalysisDTO analyzeSnapshot(
      StockIntelligenceMultiSnapshotDTO snapshot,
      StockIntelligenceReportType reportType) {
    try {
      int maxPerCategory = Math.min(properties.getOpenai().getMaxProductsPerCategory(), 15);
      var compact = analyticsService.toCompactMap(snapshot, maxPerCategory);
      String snapshotJson = objectMapper.writeValueAsString(compact);
      String userPrompt = "Type de rapport: " + reportType.label() + "\n"
          + "Données 100 % issues du ERP CMK (stock réel, entrées/sorties SQL). "
          + "Analyse comme un cabinet expert en gestion de stocks hospitaliers.\n"
          + "Snapshot JSON:\n"
          + snapshotJson;
      log.info("OpenAI analyse — modèle={}, prompt utilisateur ~{} caractères, max {} produits/catégorie/pharmacie",
          properties.getOpenai().getModel(), userPrompt.length(), maxPerCategory);

      String json = openAiClient.chatCompletionJson(systemPromptFor(reportType), userPrompt);
      return objectMapper.readValue(json, StockIntelligenceAnalysisDTO.class);
    } catch (Exception e) {
      log.error("Analyse OpenAI échouée", e);
      return fallbackAnalysis(e.getMessage());
    }
  }

  public String answerWhatsAppQuestion(Map<String, Object> erpContext, String userQuestion) {
    try {
      String contextJson = objectMapper.writeValueAsString(erpContext);
      String userPrompt = """
          Question WhatsApp (utilisateur CMK ERP):
          %s

          Données ERP temps réel (JSON — ne rien inventer en dehors de ce JSON):
          %s
          """.formatted(userQuestion, contextJson);

      return openAiClient.chatCompletionText(
          """
              Tu es l'assistant expert stock du CMK ERP, accessible par WhatsApp.
              Réponds en français, ton humain et professionnel (pharmacien / supply chain hospitalier).
              Maximum 900 caractères. Utilise des puces courtes si plusieurs pharmacies ou produits.
              Cite les chiffres réels (stock, sorties du mois, rupture, jours de couverture).
              Si aucun produit trouvé, explique comment reformuler (nom commercial exact).
              Si plusieurs produits/pharmacies, résume les points clés par site.
              Ne mentionne pas OpenAI, JSON ou « IA » — parle comme un collègue expert.
              """,
          userPrompt);
    } catch (Exception e) {
      log.error("Réponse WhatsApp OpenAI échouée", e);
      return "Désolé, je ne peux pas analyser pour le moment. Réessayez dans une minute "
          + "ou précisez le nom du produit (ex. « stock paracétamol »).";
    }
  }

  /** @deprecated conservé pour compat — préférer {@link #answerWhatsAppQuestion(Map, String)} */
  @Deprecated
  public String answerWhatsAppQuestion(
      StockIntelligenceMultiSnapshotDTO snapshot,
      String userQuestion) {
    try {
      var compact = analyticsService.toCompactMap(snapshot, 10);
      return answerWhatsAppQuestion(compact, userQuestion);
    } catch (Exception e) {
      log.error("Réponse WhatsApp OpenAI échouée", e);
      return "Désolé, je ne peux pas analyser pour le moment. Réessayez plus tard.";
    }
  }

  private String systemPromptFor(StockIntelligenceReportType reportType) {
    String focus = switch (reportType) {
      case MORNING -> """
          Focus RAPPORT DU MATIN (pilotage & actions du jour) :
          - Lecture exécutive : situation globale et par pharmacie centrale
          - Produits actifs ce mois (entrées/sorties) vs inactifs / ruptures dormantes
          - Comparaison mois en cours vs mois précédent (tendances hausse/baisse)
          - Anticipation ruptures sous 7–30 jours (couverture stock, tendance sorties)
          - Actions prioritaires 48 h pour direction achats et pharmacie centrale
          """;
      case EVENING -> """
          Focus RAPPORT DU SOIR (bilan & préparation lendemain) :
          - Bilan de la journée et du mois en cours vs mois précédent
          - Écarts et anomalies (hausse consommation, ruptures silencieuses)
          - Surstock / stock dormant à surveiller
          - Préparation réapprovisionnements et commandes urgentes
          - Perspectives pour les 7 prochains jours ouvrés
          """;
      case ON_DEMAND -> """
          Analyse complète à la demande — même rigueur qu'un rapport cabinet :
          diagnostic, risques, perspectives et plan d'action.
          """;
    };
    return """
        Tu es un consultant senior d'un cabinet d'audit et de conseil en supply chain hospitalière \
        (15+ ans d'expérience stocks pharmaceutiques, labo, économat, dialyse).
        Tu reçois des données JSON extraites en temps réel du ERP CMK — tu ne fabriques pas de chiffres.
        Ton style : précis, orienté décision, prudent sur les limites des données, jamais alarmiste sans preuve.
        Structure ton raisonnement : constat → interprétation → risques anticipés → recommandations → perspectives.
        """ + focus + JSON_SCHEMA;
  }

  private StockIntelligenceAnalysisDTO fallbackAnalysis(String error) {
    return new StockIntelligenceAnalysisDTO(
        "Analyse IA indisponible — consultez la synthèse automatique et l'Excel joint.",
        "modere",
        null,
        java.util.List.of(),
        java.util.List.of("Vérifier les ruptures sans activité ce mois et les tendances de sortie."),
        "L'agent IA n'a pas pu analyser les données : " + error,
        java.util.List.of(),
        java.util.List.of(),
        java.util.List.of(),
        java.util.List.of());
  }
}
