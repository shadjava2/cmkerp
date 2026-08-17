package cd.shad.erp.cmk.cmkerp.platform.common.application.port;

import java.util.Map;

/**
 * Port pour l'audit et la traçabilité des actions.
 *
 * <p>Ce port définit le contrat d'audit sans dépendre des implémentations
 * techniques (Kafka, base de données, fichiers, etc.).
 *
 * <p>Les implémentations de ce port (adapters) seront dans la couche infrastructure.
 *
 * <p>Ce port permet de :
 * <ul>
 *   <li>Découpler le domaine des systèmes d'audit</li>
 *   <li>Faciliter les tests (mocks/stubs)</li>
 *   <li>Permettre le changement d'implémentation (DB → Kafka → ELK)</li>
 * </ul>
 */
public interface AuditPort {

    /**
     * Enregistre un événement d'audit.
     *
     * @param action l'action effectuée (ex: "USER_CREATED", "ROLE_UPDATED")
     * @param userId l'ID de l'utilisateur qui a effectué l'action
     * @param metadata métadonnées supplémentaires (clé-valeur)
     */
    void audit(String action, Long userId, Map<String, Object> metadata);

    /**
     * Enregistre un événement d'audit avec un message personnalisé.
     *
     * @param action l'action effectuée
     * @param userId l'ID de l'utilisateur
     * @param message le message d'audit
     * @param metadata métadonnées supplémentaires
     */
    default void audit(String action, Long userId, String message, Map<String, Object> metadata) {
        if (metadata != null && message != null) {
            metadata.put("message", message);
        }
        audit(action, userId, metadata != null ? metadata : Map.of("message", message));
    }

    /**
     * Enregistre un événement d'audit simple (sans métadonnées).
     *
     * @param action l'action effectuée
     * @param userId l'ID de l'utilisateur
     */
    default void audit(String action, Long userId) {
        audit(action, userId, Map.of());
    }
}

