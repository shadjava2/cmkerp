package cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Service d'envoi d'emails (adapter infrastructure).
 *
 * <p>
 * Encapsule la logique d'envoi d'emails via JavaMailSender (SMTP).
 * Les méthodes de ce service sont conçues pour être appelées de manière asynchrone
 * (via {@code @Async}) pour ne pas bloquer le thread HTTP.
 *
 * <p>
 * Configuration SMTP via application.yml :
 *
 * <pre>{@code
 * spring:
 *   mail:
 *     host: ${MAIL_HOST}
 *     port: 465
 *     username: ${MAIL_USERNAME}
 *     password: ${MAIL_PASSWORD}
 * }</pre>
 *
 * <p>
 * Exemple d'utilisation :
 *
 * <pre>{@code
 * @Async("cmkerpAsyncExecutor")
 * public void sendWelcomeEmail(Long userId, String email) {
 *     emailService.sendEmail(email, "Bienvenue", "Bienvenue dans CMK-ERP !");
 * }
 * }</pre>
 *

 */
@Service
@ConditionalOnBean(JavaMailSender.class)
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  @Value("${spring.mail.username:}")
  private String fromEmail;

  @Value("${cmkerp.email.max-retries:3}")
  private int maxRetries;

  @Value("${cmkerp.email.retry-backoff-ms:1000}")
  private long retryBackoffMs;

  private final JavaMailSender mailSender;
  private final MeterRegistry meterRegistry;
  private Counter emailSentCounter;
  private Counter emailFailedCounter;
  private Timer emailSendTimer;

  public EmailService(JavaMailSender mailSender, @Autowired(required = false) MeterRegistry meterRegistry) {
    this.mailSender = mailSender;
    this.meterRegistry = meterRegistry;
    if (meterRegistry != null) {
      this.emailSentCounter = Counter.builder("cmkerp.email.sent")
          .description("Nombre d'emails envoyés avec succès")
          .register(meterRegistry);
      this.emailFailedCounter = Counter.builder("cmkerp.email.failed")
          .description("Nombre d'emails en échec")
          .register(meterRegistry);
      this.emailSendTimer = Timer.builder("cmkerp.email.send.duration")
          .description("Durée d'envoi des emails")
          .register(meterRegistry);
    }
  }

  /**
   * Envoie un email simple (texte) avec retry et backoff exponentiel.
   *
   * @param to destinataire
   * @param subject sujet
   * @param body corps du message
   */
  public void sendEmail(String to, String subject, String body) {
    Timer.Sample sample = meterRegistry != null ? Timer.start(meterRegistry) : null;
    int attempt = 0;
    Exception lastException = null;

    while (attempt <= maxRetries) {
      try {
        if (mailSender == null) {
          log.warn("JavaMailSender non configuré, email non envoyé -> to: {}, subject: {}", to, subject);
          if (emailFailedCounter != null) {
            emailFailedCounter.increment();
          }
          return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        if (sample != null && emailSendTimer != null) {
          sample.stop(emailSendTimer);
        }
        if (emailSentCounter != null) {
          emailSentCounter.increment();
        }
        log.info("Email envoyé avec succès -> to: {}, subject: {}, attempt: {}", to, subject, attempt + 1);
        return;
      } catch (Exception e) {
        lastException = e;
        attempt++;
        if (attempt <= maxRetries) {
          long backoffMs = retryBackoffMs * (long) Math.pow(2, attempt - 1); // Backoff exponentiel
          log.warn("Tentative {} échouée pour l'envoi d'email -> to: {}, subject: {}, retry dans {}ms",
              attempt, to, subject, backoffMs, e);
          try {
            Thread.sleep(backoffMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interruption lors du retry d'envoi d'email", ie);
            break;
          }
        }
      }
    }

    if (sample != null && emailSendTimer != null) {
      sample.stop(emailSendTimer);
    }
    if (emailFailedCounter != null) {
      emailFailedCounter.increment();
    }
    log.error("Échec définitif de l'envoi d'email après {} tentatives -> to: {}, subject: {}",
        maxRetries + 1, to, subject, lastException);
    // Ne pas propager l'exception pour ne pas interrompre le flux métier
  }

  /**
   * Envoie un email de bienvenue à un nouvel utilisateur.
   *
   * @param email adresse email de l'utilisateur
   * @param username nom d'utilisateur
   */
  public void sendWelcomeEmail(String email, String username) {
    String subject = "Bienvenue dans CMK-ERP";
    String body = String.format(
        "Bonjour %s,\n\n"
            + "Votre compte a été créé avec succès dans CMK-ERP.\n"
            + "Vous pouvez maintenant vous connecter avec votre nom d'utilisateur : %s\n\n"
            + "Cordialement,\n"
            + "L'équipe CMK-ERP",
        username, username);

    sendEmail(email, subject, body);
  }

  /**
   * Envoie un email de notification.
   *
   * @param email adresse email du destinataire
   * @param subject sujet de la notification
   * @param content contenu de la notification
   */
  public void sendNotificationEmail(String email, String subject, String content) {
    String body = String.format(
        "Bonjour,\n\n"
            + "Vous avez reçu une nouvelle notification :\n\n"
            + "%s\n\n"
            + "Cordialement,\n"
            + "L'équipe CMK-ERP",
        content);

    sendEmail(email, subject, body);
  }
}


