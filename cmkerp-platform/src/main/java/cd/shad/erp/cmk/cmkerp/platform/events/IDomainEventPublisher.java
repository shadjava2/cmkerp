package cd.shad.erp.cmk.cmkerp.platform.events;

/**
 * Interface pour la publication d'événements de domaine.
 *
 * <p>
 * Permet de publier des événements de manière asynchrone pour découpler
 * les traitements lourds (notifications, audit, reporting) des traitements immédiats.
 *
 * <p>
 * Deux implémentations sont disponibles :
 * <ul>
 * <li>{@code KafkaDomainEventPublisher} : publie via Kafka (quand {@code cmkerp.kafka.enabled=true})</li>
 * <li>{@code NoOpDomainEventPublisher} : implémentation no-op (quand {@code cmkerp.kafka.enabled=false})</li>
 * </ul>
 *
 * <p>
 * Topics Kafka configurés :
 * <ul>
 * <li>{@code cmkerp-user-events} : événements utilisateur</li>
 * <li>{@code cmkerp-audit-events} : événements d'audit</li>
 * </ul>
 *
 * <p>
 * Exemple d'utilisation :
 * <pre>{@code
 * @Autowired
 * private IDomainEventPublisher eventPublisher;
 *
 * public void createUser(User user) {
 *     // ... création utilisateur ...
 *     eventPublisher.publishUserEvent(new UserEvent(user.getId(), user.getUsername(), UserEventType.USER_CREATED));
 * }
 * }</pre>
 *

 */
public interface IDomainEventPublisher {

    /**
     * Publie un événement utilisateur.
     *
     * @param event l'événement utilisateur
     */
    void publishUserEvent(UserEvent event);

    /**
     * Publie un événement d'audit.
     *
     * @param event l'événement d'audit
     */
    void publishAuditEvent(AuditEvent event);

    /**
     * Publie un événement générique (pour extension future).
     *
     * @param topic le topic Kafka (ou identifiant de canal)
     * @param key la clé de partition
     * @param event l'événement
     */
    void publishEvent(String topic, String key, DomainEvent event);
}

