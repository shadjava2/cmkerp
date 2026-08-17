package cd.shad.erp.cmk.cmkerp.platform.config.db.routing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aspect AOP pour gérer l'annotation {@link ReadOnly} et router automatiquement
 * les méthodes annotées vers le read replica.
 *
 * <p>
 * Cet aspect intercepte les méthodes annotées avec {@code @ReadOnly} et configure
 * le contexte pour utiliser le read replica via {@link ReadWriteRoutingContext}.
 *
 * <p>
 * L'aspect est activé uniquement si les read replicas sont activés :
 * {@code cmkerp.db.read-replica.enabled=true}
 *
 * <p>
 * Exemple d'utilisation :
 * <pre>{@code
 * @Service
 * public class UserService {
 *
 *     @ReadOnly
 *     public List<User> findAllUsers() {
 *         // Cette méthode utilisera automatiquement le read replica
 *         return userRepository.findAll();
 *     }
 * }
 * }</pre>
 *

 */
@Aspect
@Component
@Order(1) // Exécuter avant les aspects de transaction
@ConditionalOnProperty(name = "cmkerp.db.read-replica.enabled", havingValue = "true", matchIfMissing = false)
public class ReadOnlyAspect {

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyAspect.class);

    /**
     * Intercepte les méthodes annotées avec {@code @ReadOnly} et configure
     * le contexte pour utiliser le read replica.
     *
     * @param joinPoint le point de jointure (méthode interceptée)
     * @param readOnly l'annotation @ReadOnly
     * @return le résultat de la méthode interceptée
     * @throws Throwable si la méthode interceptée lève une exception
     */
    @Around("@annotation(readOnly)")
    public Object routeToReadReplica(ProceedingJoinPoint joinPoint, ReadOnly readOnly) throws Throwable {
        // Vérifier si l'annotation est active
        if (!readOnly.value()) {
            return joinPoint.proceed();
        }

        // Configurer le contexte pour utiliser le read replica
        boolean wasReadOnly = ReadWriteRoutingContext.isReadOnly();
        try {
            ReadWriteRoutingContext.setReadOnly(true);
            log.debug("Routing to read replica for method: {}", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        } finally {
            // Restaurer le contexte précédent
            if (wasReadOnly) {
                ReadWriteRoutingContext.setReadOnly(true);
            } else {
                ReadWriteRoutingContext.clear();
            }
        }
    }

    /**
     * Intercepte les méthodes dans les classes annotées avec {@code @ReadOnly}
     * (au niveau de la classe).
     *
     * @param joinPoint le point de jointure (méthode interceptée)
     * @param readOnly l'annotation @ReadOnly au niveau de la classe
     * @return le résultat de la méthode interceptée
     * @throws Throwable si la méthode interceptée lève une exception
     */
    @Around("@within(readOnly) && execution(public * *(..))")
    public Object routeClassToReadReplica(ProceedingJoinPoint joinPoint, ReadOnly readOnly) throws Throwable {
        // Vérifier si l'annotation est active
        if (!readOnly.value()) {
            return joinPoint.proceed();
        }

        // Configurer le contexte pour utiliser le read replica
        boolean wasReadOnly = ReadWriteRoutingContext.isReadOnly();
        try {
            ReadWriteRoutingContext.setReadOnly(true);
            log.debug("Routing to read replica for class method: {}", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        } finally {
            // Restaurer le contexte précédent
            if (wasReadOnly) {
                ReadWriteRoutingContext.setReadOnly(true);
            } else {
                ReadWriteRoutingContext.clear();
            }
        }
    }
}

