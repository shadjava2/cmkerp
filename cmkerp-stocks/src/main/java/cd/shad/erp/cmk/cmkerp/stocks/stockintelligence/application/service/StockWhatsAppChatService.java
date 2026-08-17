package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.StockProductInsightDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppChatTestResultDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto.WhatsAppSendTestResultDTO;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.ConditionalOnStockIntelligenceEnabled;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence.WhatsAppChatLogRepository;
import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.whatsapp.WhatsAppCloudApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chat expert stock via WhatsApp : recherche ERP ciblée + réponse OpenAI.
 */
@Service
@ConditionalOnStockIntelligenceEnabled
@RequiredArgsConstructor
@Slf4j
public class StockWhatsAppChatService {

  private static final int MAX_PRODUCTS = 12;

  private final StockIntelligenceProperties properties;
  private final StockMovementAnalyticsService analyticsService;
  private final StockIntelligenceOpenAiService openAiService;
  private final WhatsAppCloudApiClient whatsAppClient;
  private final WhatsAppChatLogRepository chatLogRepository;

  public String answerQuestion(String userQuestion) {
    List<String> searchTerms = WhatsAppQuestionParser.extractSearchTerms(userQuestion);
    List<StockProductInsightDTO> products = analyticsService.searchProductsForChat(userQuestion, MAX_PRODUCTS);

    Map<String, Object> context;
    String mode;
    if (!products.isEmpty()) {
      context = analyticsService.buildWhatsAppChatContext(userQuestion, products);
      mode = "PRODUIT";
    } else if (WhatsAppQuestionParser.isGeneralStockQuestion(userQuestion)) {
      context = analyticsService.buildWhatsAppOverviewContext(userQuestion);
      mode = "SYNTHESE";
    } else {
      context = analyticsService.buildWhatsAppChatContext(userQuestion, List.of());
      context.put("note", "Aucun produit correspondant aux termes: " + searchTerms);
      mode = "AUCUN_RESULTAT";
    }

    log.info("WhatsApp chat — mode={}, termes={}, produits={}", mode, searchTerms, products.size());

    if (properties.getOpenai().isEnabled()) {
      return openAiService.answerWhatsAppQuestion(context, userQuestion);
    }
    return buildOfflineAnswer(products, context, mode);
  }

  public WhatsAppChatTestResultDTO testQuestion(String question) {
    List<String> terms = WhatsAppQuestionParser.extractSearchTerms(question);
    int found = analyticsService.searchProductsForChat(question, MAX_PRODUCTS).size();
    String answer = answerQuestion(question);
    String mode = found > 0 ? "PRODUIT" : WhatsAppQuestionParser.isGeneralStockQuestion(question) ? "SYNTHESE" : "AUCUN_RESULTAT";
    return new WhatsAppChatTestResultDTO(question, answer, found, terms, mode);
  }

  /** Analyse + envoi réel via Meta Cloud API (comme un test email). */
  public WhatsAppSendTestResultDTO sendTestMessage(String phone, String question) {
    String normalized = WhatsAppSendService.normalizePhone(phone);
    WhatsAppChatTestResultDTO analysis = testQuestion(question);
    String status = "SENT";
    String error = null;
    try {
      if (!properties.getWhatsapp().isEnabled()) {
        throw new IllegalStateException("WhatsApp désactivé — whatsapp.enabled=false dans application-dev-secrets.yml");
      }
      if (!WhatsAppConfigHelper.isSecretConfigured(properties.getWhatsapp().getToken())
          || !WhatsAppConfigHelper.isSecretConfigured(properties.getWhatsapp().getPhoneNumberId())) {
        throw new IllegalStateException("Token Meta ou Phone Number ID non configuré");
      }
      whatsAppClient.sendTextMessage(normalized, analysis.answer());
    } catch (Exception e) {
      status = "FAILED";
      error = e.getMessage();
      log.error("Envoi test WhatsApp échoué -> {}", normalized, e);
    }
    chatLogRepository.logOutbound(normalized, question, analysis.answer(), null, status, error);
    return new WhatsAppSendTestResultDTO(
        normalized, question, analysis.answer(), analysis.productsFound(), status, error);
  }

  private String buildOfflineAnswer(
      List<StockProductInsightDTO> products,
      Map<String, Object> context,
      String mode) {

    if ("SYNTHESE".equals(mode)) {
      var r = analyticsService.buildOverview(null).resumeGlobal();
      return "CMK Stock — synthèse\n"
          + "Produits analysés: " + r.totalProduitsAnalyses() + "\n"
          + "Ruptures: " + r.totalRuptures() + "\n"
          + "Avec mouvement ce mois: " + r.totalAvecMouvement() + "\n"
          + "(Activez OpenAI pour une analyse experte.)";
    }

    if (products.isEmpty()) {
      return "Aucun produit trouvé pour votre recherche. Précisez le nom commercial "
          + "(ex. « stock paracétamol 500mg »). Activez OpenAI pour plus d'aide.";
    }

    StringBuilder sb = new StringBuilder("CMK Stock — ").append(products.size()).append(" ligne(s)\n");
    products.stream().limit(5).forEach(p -> sb.append("• ")
        .append(p.pharmacie()).append(" — ")
        .append(p.nomCommercial())
        .append(": stock ").append(p.stockActuel())
        .append(p.enRupture() ? " (rupture)" : "")
        .append("\n"));
    if (products.size() > 5) {
      sb.append("… et ").append(products.size() - 5).append(" autre(s) ligne(s).");
    }
    return sb.toString();
  }
}
