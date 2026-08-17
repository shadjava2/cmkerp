package cd.shad.erp.cmk.cmkerp.platform.notification.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter.EmailService;
import cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter.SmsService;
import cd.shad.erp.cmk.cmkerp.platform.notification.application.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter pour l'envoi de notifications via email et SMS.
 *
 * <p>
 * Implémente le port NotificationPort en utilisant EmailService
 * (qui utilise JavaMailSender en interne) et SmsService (qui utilise Twilio).
 *
 * <p>
 * Cet adapter permet de découpler le domaine des détails d'implémentation
 * de l'envoi d'emails (SMTP, SendGrid, etc.) et de SMS (Twilio, etc.).
 *
 * <p>
 * <strong>Note :</strong> Ce bean est créé uniquement si EmailService est
 * disponible.
 * SmsService est optionnel : si non configuré, l'envoi SMS lèvera une
 * exception.
 * En environnement de test sans configuration SMTP, cet adapter ne sera pas
 * créé.
 */
@Component
@ConditionalOnBean(EmailService.class)
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements NotificationPort {

  private final EmailService emailService;

  @Autowired(required = false)
  private SmsService smsService;

  @Override
  public void sendEmail(String to, String subject, String body) {
    log.debug("Envoi d'email via EmailNotificationAdapter -> to: {}, subject: {}", to, subject);
    emailService.sendEmail(to, subject, body);
  }

  @Override
  public void sendSms(String to, String message) {
    if (smsService == null) {
      log.warn("SmsService non configuré, SMS non envoyé -> to: {}, message: {}", to, message);
      throw new UnsupportedOperationException(
          "Envoi SMS non configuré. Veuillez activer cmkerp.sms.enabled=true et configurer Twilio.");
    }
    log.debug("Envoi de SMS via EmailNotificationAdapter -> to: {}, message: {}", to, message);
    smsService.sendSms(to, message);
  }
}
