package cd.shad.erp.cmk.cmkerp.platform.notification.infrastructure.adapter;

import cd.shad.erp.cmk.cmkerp.platform.notification.application.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adapter NoOp pour NotificationPort utilisé quand aucun autre adapter n'est disponible.
 *
 * <p>
 * Cet adapter est créé uniquement si aucun autre bean NotificationPort n'est disponible.
 * Il permet de faire fonctionner l'application en environnement de test ou sans
 * configuration SMTP, en simulant l'envoi de notifications (juste logger).
 *
 * <p>
 * En production, EmailNotificationAdapter sera disponible et celui-ci ne sera pas créé.
 */
@Configuration
@Slf4j
public class NoOpNotificationAdapter {

    /**
     * Fournit un NotificationPort NoOp si aucun autre bean NotificationPort n'est disponible.
     *
     * <p>
     * Cet adapter ne fait rien (juste logger) et permet à l'application de fonctionner
     * sans configuration SMTP réelle (utile pour les tests et le développement local).
     *
     * @return un NotificationPort NoOp
     */
    @Bean
    @ConditionalOnMissingBean(NotificationPort.class)
    public NotificationPort noOpNotificationPort() {
        return new NotificationPort() {
            @Override
            public void sendEmail(String to, String subject, String body) {
                log.debug("[NoOp] Email simulé -> to: {}, subject: {}", to, subject);
            }

            @Override
            public void sendSms(String to, String message) {
                log.debug("[NoOp] SMS simulé -> to: {}, message: {}", to, message);
            }
        };
    }
}

