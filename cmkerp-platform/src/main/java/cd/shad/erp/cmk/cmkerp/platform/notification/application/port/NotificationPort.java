package cd.shad.erp.cmk.cmkerp.platform.notification.application.port;

/**
 * Port pour l'envoi de notifications (email, SMS, etc.).
 *
 * <p>Ce port définit le contrat d'envoi de notifications sans dépendre
 * des implémentations techniques (SMTP, API SMS, etc.).
 *
 * <p>Les implémentations de ce port (adapters) seront dans la couche infrastructure.
 *
 * <p>Ce port permet de :
 * <ul>
 *   <li>Découpler le domaine des services d'envoi techniques</li>
 *   <li>Faciliter les tests (mocks/stubs)</li>
 *   <li>Permettre le changement d'implémentation (SMTP → SendGrid, SMS → Twilio)</li>
 * </ul>
 */
public interface NotificationPort {

    /**
     * Envoie un email.
     *
     * @param to l'adresse email du destinataire
     * @param subject le sujet de l'email
     * @param body le corps de l'email (texte)
     */
    void sendEmail(String to, String subject, String body);

    /**
     * Envoie un SMS.
     *
     * @param to le numéro de téléphone du destinataire
     * @param message le message SMS
     */
    void sendSms(String to, String message);

    /**
     * Envoie une notification selon son type (email ou SMS).
     *
     * @param type le type de notification ("email" ou "sms")
     * @param to l'adresse/numéro du destinataire
     * @param subject le sujet (pour email) ou null (pour SMS)
     * @param content le contenu du message
     */
    default void sendNotification(String type, String to, String subject, String content) {
        if ("email".equalsIgnoreCase(type)) {
            sendEmail(to, subject != null ? subject : "Notification", content);
        } else if ("sms".equalsIgnoreCase(type)) {
            sendSms(to, content);
        } else {
            throw new IllegalArgumentException("Type de notification non supporté: " + type);
        }
    }
}

