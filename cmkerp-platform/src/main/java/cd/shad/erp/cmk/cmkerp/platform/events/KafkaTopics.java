package cd.shad.erp.cmk.cmkerp.platform.events;

/**
 * Constantes pour les topics Kafka utilisés par le système d'événements.
 *
 * <p>
 * Ces constantes centralisent les noms des topics Kafka pour éviter
 * la duplication et faciliter la maintenance.
 *

 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Classe utilitaire, pas d'instanciation
    }

    /**
     * Topic pour les événements utilisateur.
     */
    public static final String TOPIC_USER_EVENTS = "cmkerp-user-events";

    /**
     * Topic pour les événements d'audit.
     */
    public static final String TOPIC_AUDIT_EVENTS = "cmkerp-audit-events";

    /**
     * Topic pour les événements de notification.
     */
    public static final String TOPIC_NOTIFICATION_EVENTS = "cmkerp-notification-events";
}

