package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.openai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.config.StockIntelligenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

  private final StockIntelligenceProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();

  public String chatCompletionJson(String systemPrompt, String userPrompt) {
    return chatCompletion(systemPrompt, userPrompt, true);
  }

  public String chatCompletionText(String systemPrompt, String userPrompt) {
    return chatCompletion(systemPrompt, userPrompt, false);
  }

  private String chatCompletion(String systemPrompt, String userPrompt, boolean jsonMode) {
    StockIntelligenceProperties.OpenAi cfg = properties.getOpenai();
    if (!cfg.isEnabled() || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
      throw new IllegalStateException("OpenAI non configuré (cmkerp.stock-intelligence.openai.enabled/api-key)");
    }

    int maxAttempts = 5;
    Exception lastError = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return doChatCompletion(cfg, systemPrompt, userPrompt, jsonMode);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Appel OpenAI interrompu", e);
      } catch (Exception e) {
        lastError = e;
        if (attempt < maxAttempts && isRetryable(e)) {
          long backoffMs = retryBackoffMs(e, attempt);
          log.warn("OpenAI tentative {}/{} échouée — retry dans {}ms ({})",
              attempt, maxAttempts, backoffMs, e.getMessage());
          try {
            Thread.sleep(backoffMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Appel OpenAI interrompu", ie);
          }
        } else {
          break;
        }
      }
    }
    throw new IllegalStateException("Échec appel OpenAI: " + lastError.getMessage(), lastError);
  }

  private static long retryBackoffMs(Exception e, int attempt) {
    if (e instanceof OpenAiHttpException http && http.statusCode == 429) {
      long fromHeader = http.retryAfterMs;
      long exponential = 5000L * (1L << (attempt - 1));
      return Math.max(fromHeader, exponential);
    }
    return 1000L * (1L << (attempt - 1));
  }

  private String doChatCompletion(
      StockIntelligenceProperties.OpenAi cfg,
      String systemPrompt,
      String userPrompt,
      boolean jsonMode) throws Exception {

    Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("model", cfg.getModel());
    body.put("max_tokens", cfg.getMaxTokens());
    if (jsonMode) {
      body.put("response_format", Map.of("type", "json_object"));
    }
    body.put("messages", List.of(
        Map.of("role", "system", "content", systemPrompt),
        Map.of("role", "user", "content", userPrompt)));

    String jsonBody = objectMapper.writeValueAsString(body);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(cfg.getBaseUrl() + "/chat/completions"))
        .timeout(Duration.ofSeconds(120))
        .header("Authorization", "Bearer " + cfg.getApiKey())
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 429) {
      log.warn("OpenAI HTTP 429 (quota / TPM) — modèle={}, corps={}",
          cfg.getModel(), truncate(response.body(), 400));
      throw new OpenAiHttpException(429, parseRetryAfterMs(response), "Erreur OpenAI HTTP 429");
    }
    if (response.statusCode() >= 500) {
      throw new OpenAiHttpException(response.statusCode(), 0, "Erreur OpenAI HTTP " + response.statusCode());
    }
    if (response.statusCode() >= 400) {
      log.error("OpenAI HTTP {}: {}", response.statusCode(), response.body());
      throw new IllegalStateException("Erreur OpenAI HTTP " + response.statusCode());
    }

    JsonNode root = objectMapper.readTree(response.body());
    return root.path("choices").path(0).path("message").path("content").asText();
  }

  private boolean isRetryable(Exception e) {
    if (e instanceof OpenAiHttpException http) {
      return http.statusCode == 429 || http.statusCode >= 500;
    }
    if (e instanceof java.net.http.HttpTimeoutException) {
      return true;
    }
    String msg = e.getMessage();
    return msg != null && (msg.contains("HTTP 429") || msg.contains("HTTP 5"));
  }

  private static long parseRetryAfterMs(HttpResponse<String> response) {
    return response.headers().firstValue("retry-after")
        .map(value -> {
          try {
            return Long.parseLong(value.trim()) * 1000L;
          } catch (NumberFormatException ignored) {
            return 0L;
          }
        })
        .orElse(0L);
  }

  private static String truncate(String text, int max) {
    if (text == null || text.length() <= max) {
      return text;
    }
    return text.substring(0, max) + "...";
  }

  private static final class OpenAiHttpException extends RuntimeException {
    private final int statusCode;
    private final long retryAfterMs;

    private OpenAiHttpException(int statusCode, long retryAfterMs, String message) {
      super(message);
      this.statusCode = statusCode;
      this.retryAfterMs = retryAfterMs;
    }
  }
}
