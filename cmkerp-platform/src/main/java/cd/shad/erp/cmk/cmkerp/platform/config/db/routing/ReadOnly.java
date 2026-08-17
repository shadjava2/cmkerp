package cd.shad.erp.cmk.cmkerp.platform.config.db.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les méthodes qui doivent utiliser le read replica.
 *
 * <p>
 * Cette annotation peut être utilisée sur les méthodes de service ou repository
 * pour indiquer qu'elles effectuent uniquement des lectures et peuvent donc
 * utiliser le read replica au lieu du primary.
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
 *
 *     public User createUser(User user) {
 *         // Cette méthode utilisera le primary (écriture)
 *         return userRepository.save(user);
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Note : Cette annotation nécessite un aspect ou un interceptor pour être effective.
 * Voir {@link ReadOnlyAspect} pour l'implémentation.
 *

 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {
    /**
     * Indique si l'annotation est active (par défaut : true).
     * Permet de désactiver temporairement le routing vers le replica.
     */
    boolean value() default true;
}

