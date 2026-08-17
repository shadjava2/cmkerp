package cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter;

import cd.shad.erp.cmk.cmkerp.platform.common.application.port.AuditPort;
import cd.shad.erp.cmk.cmkerp.platform.events.AuditEvent;
import cd.shad.erp.cmk.cmkerp.platform.events.IDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter pour l'audit via Kafka (via IDomainEventPublisher).
 *
 * <p>Implémente le port AuditPort en utilisant IDomainEventPublisher
 * pour publier des AuditEvent vers Kafka.
 *
 * <p>Cet adapter permet de découpler le domaine des détails d'implémentation
 * de l'audit (Kafka, base de données, fichiers, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaAuditAdapter implements AuditPort {

    private final IDomainEventPublisher eventPublisher;

    @Override
    public void audit(String action, Long userId, Map<String, Object> metadata) {
        log.debug("Enregistrement d'audit via KafkaAuditAdapter -> action: {}, userId: {}", action, userId);

        // Construire les détails depuis les métadonnées
        String details = buildDetailsFromMetadata(metadata);

        // Créer l'événement d'audit
        // Note: Pour récupérer le username, on devrait idéalement le passer en paramètre
        // ou le récupérer depuis un service. Pour l'instant, on met null.
        AuditEvent auditEvent = new AuditEvent(
            userId,
            null, // username - à récupérer si nécessaire
            action,
            extractResourceType(metadata),
            extractResourceId(metadata),
            details
        );

        // Publier l'événement via le publisher
        eventPublisher.publishAuditEvent(auditEvent);
    }

    /**
     * Construit une chaîne de détails depuis les métadonnées.
     */
    private String buildDetailsFromMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        StringBuilder details = new StringBuilder();
        metadata.forEach((key, value) -> {
            if (!"resourceType".equals(key) && !"resourceId".equals(key) && !"message".equals(key)) {
                if (details.length() > 0) {
                    details.append(", ");
                }
                details.append(key).append("=").append(value);
            }
        });

        // Ajouter le message s'il existe
        if (metadata.containsKey("message")) {
            if (details.length() > 0) {
                details.append(" - ");
            }
            details.append(metadata.get("message"));
        }

        return details.length() > 0 ? details.toString() : null;
    }

    /**
     * Extrait le type de ressource depuis les métadonnées.
     */
    private String extractResourceType(Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("resourceType")) {
            return metadata.get("resourceType").toString();
        }
        return null;
    }

    /**
     * Extrait l'ID de ressource depuis les métadonnées.
     */
    private Long extractResourceId(Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("resourceId")) {
            Object resourceId = metadata.get("resourceId");
            if (resourceId instanceof Long) {
                return (Long) resourceId;
            } else if (resourceId instanceof Number) {
                return ((Number) resourceId).longValue();
            } else if (resourceId != null) {
                try {
                    return Long.parseLong(resourceId.toString());
                } catch (NumberFormatException e) {
                    log.warn("Impossible de parser resourceId en Long: {}", resourceId);
                }
            }
        }
        return null;
    }
}

