package cd.shad.erp.cmk.cmkerp.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration AOP pour activer le support des aspects (notamment pour les read replicas).
 *
 * <p>
 * Cette configuration active le support AspectJ pour permettre l'utilisation
 * de l'annotation {@code @ReadOnly} et du routing automatique vers les read replicas.
 *
 * <p>
 * Note : Spring Boot active automatiquement AOP si des aspects sont détectés,
 * mais cette configuration explicite garantit le bon fonctionnement même
 * si aucun aspect n'est encore chargé au démarrage.
 *
 * <p>
 * L'activation est conditionnelle : uniquement si les read replicas sont activés.
 * Si les read replicas sont désactivés, cette configuration n'a pas d'impact.
 *

 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnProperty(name = "cmkerp.db.read-replica.enabled", havingValue = "true", matchIfMissing = false)
public class AopConfig {
    // Configuration AOP activée automatiquement par Spring Boot si des aspects sont détectés
    // Cette classe garantit l'activation même si les aspects sont chargés tardivement
}

