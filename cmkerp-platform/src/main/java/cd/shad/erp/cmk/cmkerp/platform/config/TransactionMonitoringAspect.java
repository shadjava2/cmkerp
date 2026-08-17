package cd.shad.erp.cmk.cmkerp.platform.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect AOP pour surveiller les transactions longues.
 *
 * <p>
 * Cet aspect log un avertissement si une transaction prend plus de temps que le seuil configuré.
 * Utile pour détecter les transactions qui pourraient bloquer des connexions trop longtemps.
 *
 * <p>
 * Configuration dans application.yml :
 *
 * <pre>{@code
 * cmkerp:
 *   monitoring:
 *     transaction:
 *       enabled: true
 *       warning-threshold-ms: 1000  # Avertir si > 1 seconde
 * }</pre>
 *
 * <p>
 * Pour désactiver le monitoring :
 *
 * <pre>{@code
 * cmkerp:
 *   monitoring:
 *     transaction:
 *       enabled: false
 * }</pre>
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(prefix = "cmkerp.monitoring.transaction", name = "enabled",
    havingValue = "true", matchIfMissing = false)
public class TransactionMonitoringAspect {

  /**
   * Seuil en millisecondes au-delà duquel une transaction est considérée comme longue. Par défaut :
   * 1000ms (1 seconde).
   */
  @Value("${cmkerp.monitoring.transaction.warning-threshold-ms:1000}")
  private long warningThresholdMs;

  /**
   * Intercepte toutes les méthodes annotées avec @Transactional.
   *
   * @param joinPoint le point de jointure (méthode interceptée)
   * @return le résultat de la méthode
   * @throws Throwable si une exception est levée
   */
  @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
  public Object monitorTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!log.isWarnEnabled()) {
      // Si les warnings ne sont pas activés, ne pas calculer la durée
      return joinPoint.proceed();
    }

    long startTime = System.currentTimeMillis();
    String methodName = joinPoint.getSignature().toShortString();
    String className = joinPoint.getTarget().getClass().getSimpleName();

    try {
      Object result = joinPoint.proceed();
      long duration = System.currentTimeMillis() - startTime;

      if (duration >= warningThresholdMs) {
        log.warn(
            "⚠️ Transaction longue détectée: {}ms - {}.{}() - Considérer optimiser cette transaction",
            duration, className, methodName);
      } else if (log.isDebugEnabled() && duration > 100) {
        // Logger en DEBUG les transactions > 100ms pour debugging
        log.debug("Transaction: {}ms - {}.{}()", duration, className, methodName);
      }

      return result;
    } catch (Throwable throwable) {
      long duration = System.currentTimeMillis() - startTime;

      if (duration >= warningThresholdMs) {
        log.warn("⚠️ Transaction longue avec exception: {}ms - {}.{}() - Exception: {}", duration,
            className, methodName, throwable.getClass().getSimpleName());
      }

      throw throwable;
    }
  }
}



