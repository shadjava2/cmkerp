package cd.shad.erp.cmk.cmkerp.platform.events;

import cd.shad.erp.cmk.cmkerp.platform.config.elasticsearch.ElasticsearchProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service pour l'indexation des événements d'audit dans Elasticsearch.
 * S'active uniquement si cmkerp.elasticsearch.enabled=true.
 */
@Service
@ConditionalOnClass(name = "co.elastic.clients.elasticsearch.ElasticsearchClient")
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchAuditService {

  private static final Logger log = LoggerFactory.getLogger(ElasticsearchAuditService.class);

  private final ElasticsearchClient elasticsearchClient;
  private final String indexName;
  private Counter elasticsearchSuccessCounter;
  private Counter elasticsearchFailureCounter;

  public ElasticsearchAuditService(
      @Autowired ElasticsearchClient elasticsearchClient,
      @Autowired(required = false) ElasticsearchProperties elasticsearchProperties,
      @Autowired(required = false) MeterRegistry meterRegistry) {
    this.elasticsearchClient = elasticsearchClient;
    this.indexName = elasticsearchProperties != null ? elasticsearchProperties.getIndexName()
        : "cmkerp-audit-events";

    if (meterRegistry != null) {
      this.elasticsearchSuccessCounter = Counter.builder("cmkerp.elasticsearch.audit.events.success")
          .description("Nombre d'événements d'audit indexés avec succès dans Elasticsearch")
          .tag("component", "elasticsearch-audit-service")
          .register(meterRegistry);

      this.elasticsearchFailureCounter = Counter.builder("cmkerp.elasticsearch.audit.events.failure")
          .description("Nombre d'échecs d'indexation d'événements d'audit dans Elasticsearch")
          .tag("component", "elasticsearch-audit-service")
          .register(meterRegistry);
    }

    log.info("ElasticsearchAuditService initialisé avec l'index: {}", indexName);
  }

  /**
   * Indexe un événement d'audit dans Elasticsearch avec retry automatique.
   */
  public void indexAuditEvent(AuditEvent event) {
    int maxRetries = 3;
    long initialDelayMs = 1000;
    long maxDelayMs = 5000;
    long delayMs = initialDelayMs;

    Exception lastException = null;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        Map<String, Object> document = buildDocument(event);

        IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
            .index(indexName)
            .id(event.getEventId())
            .document(document));

        IndexResponse response = elasticsearchClient.index(request);

        if (response.result() != null) {
          log.debug("Événement d'audit indexé dans Elasticsearch: eventId={}, index={}, result={}, attempt={}",
              event.getEventId(), indexName, response.result(), attempt);
          if (elasticsearchSuccessCounter != null) {
            elasticsearchSuccessCounter.increment();
          }
          return; // Succès, sortir de la boucle
        } else {
          log.warn("Indexation Elasticsearch retournée sans résultat: eventId={}, attempt={}",
              event.getEventId(), attempt);
          // Considérer comme un succès partiel mais logguer
          if (elasticsearchSuccessCounter != null) {
            elasticsearchSuccessCounter.increment();
          }
          return;
        }
      } catch (Exception e) {
        lastException = e;
        if (attempt < maxRetries) {
          log.warn("Tentative {} échouée pour l'indexation Elasticsearch: eventId={}, retry dans {}ms",
              attempt, event.getEventId(), delayMs, e);
          try {
            Thread.sleep(delayMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interruption pendant le retry Elasticsearch: eventId={}", event.getEventId(), ie);
            break;
          }
          // Backoff exponentiel: 1s, 2s, 4s (max 5s)
          delayMs = Math.min(delayMs * 2, maxDelayMs);
        } else {
          // Dernière tentative échouée
          log.error("Échec définitif de l'indexation Elasticsearch après {} tentatives: eventId={}",
              maxRetries, event.getEventId(), e);
          if (elasticsearchFailureCounter != null) {
            elasticsearchFailureCounter.increment();
          }
        }
      }
    }

    // Si on arrive ici, toutes les tentatives ont échoué
    // On log l'erreur mais on ne bloque pas le traitement principal
    if (lastException != null) {
      log.error("Impossible d'indexer l'événement d'audit dans Elasticsearch après {} tentatives: eventId={}",
          maxRetries, event.getEventId(), lastException);
    }
  }

  private Map<String, Object> buildDocument(AuditEvent event) {
    Map<String, Object> doc = new HashMap<>();
    doc.put("eventId", event.getEventId());
    doc.put("timestamp", event.getTimestamp().toString());
    doc.put("eventType", event.getEventType());
    doc.put("userId", event.getUserId());
    doc.put("username", event.getUsername());
    doc.put("action", event.getAction());
    doc.put("resourceType", event.getResourceType());
    doc.put("resourceId", event.getResourceId());
    doc.put("details", event.getDetails());
    // Ajouter un champ @timestamp pour faciliter les recherches par date dans
    // Kibana
    doc.put("@timestamp", event.getTimestamp());
    return doc;
  }
}
