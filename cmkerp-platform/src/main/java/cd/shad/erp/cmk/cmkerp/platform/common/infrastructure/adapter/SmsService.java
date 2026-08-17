package cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Service d'envoi de SMS via Twilio API.
 */
@Service
@ConditionalOnProperty(name = "cmkerp.sms.enabled", havingValue = "true", matchIfMissing = false)
public class SmsService {

  private static final Logger log = LoggerFactory.getLogger(SmsService.class);

  @Value("${cmkerp.sms.twilio.account-sid:}")
  private String accountSid;

  @Value("${cmkerp.sms.twilio.auth-token:}")
  private String authToken;

  @Value("${cmkerp.sms.twilio.from-number:}")
  private String fromNumber;

  @Value("${cmkerp.sms.max-retries:3}")
  private int maxRetries;

  @Value("${cmkerp.sms.retry-backoff-ms:1000}")
  private long retryBackoffMs;

  private final MeterRegistry meterRegistry;
  private Counter smsSentCounter;
  private Counter smsFailedCounter;
  private Timer smsSendTimer;
  private volatile boolean twilioInitialized = false;

  public SmsService(@Autowired(required = false) MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    if (meterRegistry != null) {
      this.smsSentCounter = Counter.builder("cmkerp.sms.sent")
          .description("Nombre de SMS envoyés avec succès")
          .register(meterRegistry);
      this.smsFailedCounter = Counter.builder("cmkerp.sms.failed")
          .description("Nombre de SMS en échec")
          .register(meterRegistry);
      this.smsSendTimer = Timer.builder("cmkerp.sms.send.duration")
          .description("Durée d'envoi des SMS")
          .register(meterRegistry);
    }
  }

  private void initializeTwilio() {
    if (!twilioInitialized) {
      synchronized (this) {
        if (!twilioInitialized) {
          if (accountSid == null || accountSid.isEmpty() ||
              authToken == null || authToken.isEmpty()) {
            log.warn("Configuration Twilio incomplète (account-sid ou auth-token manquant)");
            return;
          }
          try {
            com.twilio.Twilio.init(accountSid, authToken);
            twilioInitialized = true;
            log.info("Twilio initialisé avec succès (account-sid: {})",
                accountSid.substring(0, Math.min(8, accountSid.length())) + "...");
          } catch (Exception e) {
            log.error("Erreur lors de l'initialisation de Twilio", e);
          }
        }
      }
    }
  }

  /**
   * Envoie un SMS avec retry automatique.
   */
  public void sendSms(String to, String message) {
    Timer.Sample sample = meterRegistry != null ? Timer.start(meterRegistry) : null;
    int attempt = 0;
    Exception lastException = null;

    // Validation de la configuration
    if (accountSid == null || accountSid.isEmpty() ||
        authToken == null || authToken.isEmpty() ||
        fromNumber == null || fromNumber.isEmpty()) {
      log.warn("Configuration Twilio incomplète, SMS non envoyé -> to: {}", to);
      if (smsFailedCounter != null) {
        smsFailedCounter.increment();
      }
      if (sample != null && smsSendTimer != null) {
        sample.stop(smsSendTimer);
      }
      return;
    }

    // Initialisation de Twilio (une seule fois, thread-safe)
    initializeTwilio();

    if (!twilioInitialized) {
      log.warn("Twilio non initialisé, SMS non envoyé -> to: {}", to);
      if (smsFailedCounter != null) {
        smsFailedCounter.increment();
      }
      if (sample != null && smsSendTimer != null) {
        sample.stop(smsSendTimer);
      }
      return;
    }

    while (attempt <= maxRetries) {
      try {
        // Envoi du SMS via Twilio
        com.twilio.rest.api.v2010.account.Message twilioMessage = com.twilio.rest.api.v2010.account.Message.creator(
            new com.twilio.type.PhoneNumber(to),
            new com.twilio.type.PhoneNumber(fromNumber),
            message).create();

        if (sample != null && smsSendTimer != null) {
          sample.stop(smsSendTimer);
        }
        if (smsSentCounter != null) {
          smsSentCounter.increment();
        }
        log.info("SMS envoyé avec succès -> to: {}, sid: {}, attempt: {}",
            to, twilioMessage.getSid(), attempt + 1);
        return;
      } catch (com.twilio.exception.ApiException e) {
        lastException = e;
        attempt++;
        if (attempt <= maxRetries) {
          long backoffMs = retryBackoffMs * (long) Math.pow(2, attempt - 1); // Backoff exponentiel
          log.warn("Tentative {} échouée pour l'envoi de SMS -> to: {}, retry dans {}ms, code: {}, message: {}",
              attempt, to, backoffMs, e.getCode(), e.getMessage());
          try {
            Thread.sleep(backoffMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interruption lors du retry d'envoi de SMS", ie);
            break;
          }
        }
      } catch (Exception e) {
        lastException = e;
        attempt++;
        if (attempt <= maxRetries) {
          long backoffMs = retryBackoffMs * (long) Math.pow(2, attempt - 1);
          log.warn("Tentative {} échouée pour l'envoi de SMS -> to: {}, retry dans {}ms",
              attempt, to, backoffMs, e);
          try {
            Thread.sleep(backoffMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interruption lors du retry d'envoi de SMS", ie);
            break;
          }
        }
      }
    }

    if (sample != null && smsSendTimer != null) {
      sample.stop(smsSendTimer);
    }
    if (smsFailedCounter != null) {
      smsFailedCounter.increment();
    }
    log.error("Échec définitif de l'envoi de SMS après {} tentatives -> to: {}",
        maxRetries + 1, to, lastException);
    // Ne pas propager l'exception pour ne pas interrompre le flux métier
  }
}
