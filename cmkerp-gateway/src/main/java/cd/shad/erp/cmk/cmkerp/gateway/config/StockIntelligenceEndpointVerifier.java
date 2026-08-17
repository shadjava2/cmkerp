package cd.shad.erp.cmk.cmkerp.gateway.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;

/**
 * Échec au démarrage si les routes REST stock-intelligence ne sont pas enregistrées.
 */
@Component
@Order(100)
public class StockIntelligenceEndpointVerifier implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StockIntelligenceEndpointVerifier.class);
  private static final String PLACEHOLDER_API_KEY = "CHANGEZ-MOI";

  private static final Set<String> REQUIRED_PATH_FRAGMENTS = Set.of(
      "/stock-intelligence/status",
      "/stock-intelligence/email-history",
      "/stock-intelligence/mailingsend",
      "/stock-intelligence/pharmacies-central",
      "/stock-intelligence/snapshot");

  private final RequestMappingHandlerMapping handlerMapping;
  private final StockIntelligenceProperties stockIntelligenceProperties;

  public StockIntelligenceEndpointVerifier(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
      StockIntelligenceProperties stockIntelligenceProperties) {
    this.handlerMapping = handlerMapping;
    this.stockIntelligenceProperties = stockIntelligenceProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    Set<String> registered = handlerMapping.getHandlerMethods().keySet().stream()
        .map(this::pathPattern)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

    boolean ok = REQUIRED_PATH_FRAGMENTS.stream().allMatch(required ->
        registered.stream().anyMatch(p -> p.contains(required)));

    if (ok) {
      log.info("API stock-intelligence détectée ({} routes)", countStockIntelligenceRoutes(registered));
      logOpenAiConfig();
      return;
    }

    String missing = REQUIRED_PATH_FRAGMENTS.stream()
        .filter(required -> registered.stream().noneMatch(p -> p.contains(required)))
        .collect(Collectors.joining(", "));

    String message = """
        API stock-intelligence absente du gateway (%s manquant).
        Vérifiez que cmkerp-stocks (stock-intelligence) est compilé et que le gateway scanne ce module.
        Rebuild : mvn -pl cmkerp-gateway -am install -DskipTests
        Puis redémarrez complètement le gateway (pas un simple reload DevTools).
        """.formatted(missing);

    log.error(message);
    throw new IllegalStateException(message.trim());
  }

  private Set<String> pathPattern(RequestMappingInfo info) {
    if (info.getPathPatternsCondition() == null) {
      return Set.of();
    }
    return info.getPathPatternsCondition().getPatterns().stream()
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  private long countStockIntelligenceRoutes(Set<String> registered) {
    return registered.stream().filter(p -> p.contains("stock-intelligence")).count();
  }

  private void logOpenAiConfig() {
    StockIntelligenceProperties.OpenAi openAi = stockIntelligenceProperties.getOpenai();
    if (!openAi.isEnabled()) {
      log.info("OpenAI stock-intelligence : désactivé (cmkerp.stock-intelligence.openai.enabled=false)");
      return;
    }
    String apiKey = openAi.getApiKey();
    if (apiKey == null || apiKey.isBlank() || PLACEHOLDER_API_KEY.equals(apiKey.trim())) {
      log.warn(
          "OpenAI stock-intelligence : activé mais clé absente ou placeholder — "
              + "renseignez application-dev-secrets.yml puis redémarrez complètement le gateway");
      return;
    }
    log.info(
        "OpenAI stock-intelligence : activé, modèle={}, clé={}",
        openAi.getModel(),
        maskApiKey(apiKey));
  }

  private static String maskApiKey(String apiKey) {
    if (apiKey.length() <= 12) {
      return "sk-***";
    }
    return apiKey.substring(0, 7) + "..." + apiKey.substring(apiKey.length() - 4);
  }
}
