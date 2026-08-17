package cd.shad.erp.cmk.cmkerp.platform.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Consumer Kafka pour les événements d'audit.
 *
 * <p>
 * Écoute le topic {@code cmkerp-audit-events} et traite les événements d'audit.
 * En production, cet
 * événement pourrait être persisté dans une base d'audit, envoyé à un système
 * de logging centralisé
 * (ELK, Splunk), etc.
 *
 * <p>
 * L'événement est :
 * <ul>
 * <li>Loggé pour le suivi immédiat</li>
 * <li>Persisté dans la table d'audit MySQL (audit_events) pour un historique
 * complet</li>
 * <li>Enregistré dans des métriques pour le monitoring
 * (Prometheus/Grafana)</li>
 * <li>Indexé dans Elasticsearch pour recherche avancée (si activé via
 * {@code cmkerp.elasticsearch.enabled=true})</li>
 * </ul>
 *
 * <p>
 * Ce consumer est conditionnel : il ne s'active que si
 * {@code cmkerp.kafka.enabled=true}. Quand
 * Kafka est désactivé, ce bean ne sera pas créé.
 *

 */
@Component
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AuditEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

  private final MeterRegistry meterRegistry;
  private final AuditEventRepository auditEventRepository;
  private final ElasticsearchAuditService elasticsearchAuditService;
  private Counter auditEventCounter;

  public AuditEventConsumer(
      @Autowired(required = false) MeterRegistry meterRegistry,
      @Autowired(required = false) AuditEventRepository auditEventRepository,
      @Autowired(required = false) ElasticsearchAuditService elasticsearchAuditService) {
    this.meterRegistry = meterRegistry;
    this.auditEventRepository = auditEventRepository;
    this.elasticsearchAuditService = elasticsearchAuditService;
    if (meterRegistry != null) {
      this.auditEventCounter = Counter.builder("cmkerp.audit.events.total")
          .description("Nombre total d'événements d'audit traités")
          .tag("component", "audit-consumer").register(meterRegistry);
    }
  }

  /**
   * Consomme les événements d'audit depuis Kafka.
   *
   * @param event l'événement d'audit
   */
  @KafkaListener(topics = KafkaTopics.TOPIC_AUDIT_EVENTS, groupId = "${spring.kafka.consumer.group-id:cmkerp-api-group}")
  public void consumeAuditEvent(AuditEvent event) {
    log.info("AUDIT: User={} (ID:{}) | Action={} | Resource={}#{} | Details={}",
        event.getUsername(), event.getUserId(), event.getAction(), event.getResourceType(),
        event.getResourceId(), event.getDetails());

    // Enregistrer la métrique pour le monitoring (dashboard Prometheus/Grafana)
    if (meterRegistry != null) {
      // Compteur total d'événements d'audit
      if (auditEventCounter != null) {
        auditEventCounter.increment();
      }

      // Compteur détaillé par action et type de ressource (pour dashboard)
      Counter.builder("cmkerp.audit.events")
          .description("Événements d'audit par action et type de ressource")
          .tag("action", event.getAction() != null ? event.getAction() : "unknown")
          .tag("resource_type",
              event.getResourceType() != null ? event.getResourceType() : "unknown")
          .register(meterRegistry).increment();
    }

    // Persister l'événement dans la table d'audit MySQL
    if (auditEventRepository != null) {
      try {
        auditEventRepository.save(event);
        log.debug("Événement d'audit persisté avec succès dans la base de données");
      } catch (Exception e) {
        // Log l'erreur mais ne bloque pas le traitement (événement déjà loggé et
        // métriques enregistrées)
        log.error("Erreur lors de la persistance de l'événement d'audit dans la base de données", e);
      }
    } else {
      log.warn("AuditEventRepository non disponible, l'événement n'a pas été persisté");
    }

    // Indexer l'événement dans Elasticsearch pour recherche avancée
    if (elasticsearchAuditService != null) {
      try {
        elasticsearchAuditService.indexAuditEvent(event);
        log.debug("Événement d'audit indexé avec succès dans Elasticsearch");
      } catch (Exception e) {
        // Log l'erreur mais ne bloque pas le traitement (événement déjà loggé,
        // persisté en DB et métriques enregistrées)
        log.error("Erreur lors de l'indexation de l'événement d'audit dans Elasticsearch", e);
      }
    } else {
      log.debug("ElasticsearchAuditService non disponible, l'événement n'a pas été indexé");
    }
  }
}
