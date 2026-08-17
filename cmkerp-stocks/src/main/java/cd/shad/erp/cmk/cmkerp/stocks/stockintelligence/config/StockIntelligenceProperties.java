package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cmkerp.stock-intelligence")
public class StockIntelligenceProperties {

  private boolean enabled = false;

  /** Pharmacie centrale par défaut (null = toutes Centrale) */
  private Long defaultPharmacieId;

  /** Cron rapport email matin — désactivé si morning-report-enabled=false */
  private String morningCron = "0 0 7 * * ?";

  private boolean morningReportEnabled = false;

  private String eveningCron = "0 0 18 * * ?";

  private boolean eveningReportEnabled = false;

  /** true = lance un rapport matin immédiatement au démarrage (dev/test) */
  private boolean runOnStartup = false;

  private OpenAi openai = new OpenAi();

  private WhatsApp whatsapp = new WhatsApp();

  private Email email = new Email();

  @Data
  public static class OpenAi {
    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private int maxTokens = 4096;
    /** Nombre max de produits par catégorie envoyés à l'IA (évite tokens excessifs) */
    private int maxProductsPerCategory = 40;
  }

  @Data
  public static class WhatsApp {
    private boolean enabled = false;
    private String token = "";
    private String phoneNumberId = "";
    private String verifyToken = "";
    private String graphApiVersion = "v21.0";
    /** Numéros autorisés (sans +), vide = tous si webhook actif */
    private java.util.List<String> allowedNumbers = java.util.List.of();
  }

  @Data
  public static class Email {
    private String subjectPrefix = "[CMK Stock]";
    private String defaultRecipient = "";
  }
}
