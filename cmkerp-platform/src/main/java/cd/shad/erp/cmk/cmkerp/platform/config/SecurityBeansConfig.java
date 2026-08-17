package cd.shad.erp.cmk.cmkerp.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration des beans de sécurité pour le module platform.
 *
 * <p>
 * Fournit les beans utilitaires nécessaires à la gestion de la sécurité au niveau métier, sans
 * dépendre directement de la configuration Spring Security (qui sera gérée dans le module gateway).
 *
 * <p>
 * Beans fournis :
 * <ul>
 * <li>{@link PasswordEncoder} : pour le hachage des mots de passe avec BCrypt</li>
 * </ul>
 *
 * <p>
 * Pour la gestion des permissions utilisateur, utilisez directement :
 * <ul>
 * <li>{@link cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions} : classe du
 * shared-kernel pour vérifier les permissions</li>
 * <li>{@link cd.shad.erp.cmk.cmkerp.platform.security.application.service.SecurityMapper} : mapper pour construire
 * UserPermissions à partir des entités</li>
 * <li>{@link cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionCode} : énumération des codes
 * de permissions disponibles</li>
 * </ul>
 *
 * <p>
 * Exemple d'utilisation dans les services :
 *
 * <pre>{@code
 * @Service
 * public class MonService {
 *   private final PasswordEncoder passwordEncoder;
 *
 *   public void operationMetier(UserPermissions userPerms) {
 *     // Vérification directe via UserPermissions (gère les nulls)
 *     if (userPerms == null || !userPerms.hasPermission(PermissionCode.MANAGE_STOCK.getCode())) {
 *       throw new BusinessException("Permission insuffisante");
 *     }
 *
 *     // Vérification d'accès à une pharmacie
 *     if (!userPerms.canAccessPharmacy(pharmacieId)) {
 *       throw new BusinessException("Accès refusé à cette pharmacie");
 *     }
 *
 *     // Vérification du verrouillage
 *     if (userPerms.isLocked()) {
 *       throw new BusinessException("Compte verrouillé");
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Note: La configuration complète de Spring Security (filtres, règles d'autorisation, JWT, etc.)
 * doit être effectuée dans le module gateway. Cette classe fournit uniquement les composants métier
 * nécessaires à la platform.
 *

 * @see cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions
 * @see cd.shad.erp.cmk.cmkerp.sharedkernel.security.SecurityMapper
 * @see cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionCode
 */
@Configuration
public class SecurityBeansConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityBeansConfig.class);

  /**
   * Coût (strength) du BCrypt. Configurable via la propriété
   * {@code cmkerp.security.bcrypt.strength}. Valeur par défaut: 10 (bon équilibre
   * sécurité/performance). Augmenter à 12-14 pour une sécurité renforcée (plus lent).
   */
  @Value("${cmkerp.security.bcrypt.strength:10}")
  private int bcryptStrength;

  /**
   * Crée un bean {@link PasswordEncoder} utilisant BCrypt.
   *
   * <p>
   * BCrypt est un algorithme de hachage adapté pour les mots de passe :
   * <ul>
   * <li>Inclut un salt automatique (unique par mot de passe)</li>
   * <li>Coût configurable via la propriété {@code cmkerp.security.bcrypt.strength}</li>
   * <li>Résistant aux attaques par force brute</li>
   * </ul>
   *
   * <p>
   * Utilisation typique dans les services :
   *
   * <pre>{@code
   * @Service
   * public class UtilisateurService {
   *   private final PasswordEncoder passwordEncoder;
   *
   *   public void changePassword(Long userId, String newPassword) {
   *     String encoded = passwordEncoder.encode(newPassword);
   *     // Sauvegarder encoded dans la base de données
   *   }
   *
   *   public boolean verifyPassword(String rawPassword, String encodedPassword) {
   *     return passwordEncoder.matches(rawPassword, encodedPassword);
   *   }
   * }
   * }</pre>
   *
   * <p>
   * Le coût (strength) peut être configuré via la propriété {@code cmkerp.security.bcrypt.strength}
   * dans les fichiers application.properties. Valeur par défaut: 10 (équilibre
   * sécurité/performance).
   *
   * @return un PasswordEncoder utilisant BCrypt avec le coût configuré
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    log.info("Initialisation du BCryptPasswordEncoder avec strength={}", bcryptStrength);
    return new BCryptPasswordEncoder(bcryptStrength);
  }
}

