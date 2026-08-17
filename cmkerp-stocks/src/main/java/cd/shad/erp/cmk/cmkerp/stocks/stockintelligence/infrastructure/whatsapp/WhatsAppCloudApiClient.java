package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.whatsapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppCloudApiClient {

  private final StockIntelligenceProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(20))
      .build();

  public void sendTextMessage(String toNumber, String text) {
    var wa = properties.getWhatsapp();
    if (!wa.isEnabled()) {
      throw new IllegalStateException("WhatsApp non activé");
    }
    String normalizedTo = toNumber.replaceAll("[^0-9]", "");
    try {
      Map<String, Object> body = Map.of(
          "messaging_product", "whatsapp",
          "to", normalizedTo,
          "type", "text",
          "text", Map.of("preview_url", false, "body", truncate(text, 4090)));

      String url = "https://graph.facebook.com/" + wa.getGraphApiVersion() + "/" + wa.getPhoneNumberId() + "/messages";
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(30))
          .header("Authorization", "Bearer " + wa.getToken())
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        log.error("WhatsApp API {}: {}", response.statusCode(), response.body());
        throw new IllegalStateException("WhatsApp HTTP " + response.statusCode());
      }
      log.info("WhatsApp message envoyé -> {}", normalizedTo);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Envoi WhatsApp interrompu", e);
    } catch (Exception e) {
      throw new IllegalStateException("Échec envoi WhatsApp: " + e.getMessage(), e);
    }
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max - 3) + "...";
  }
}
