package cd.shad.erp.cmk.cmkerp.platform.config;

/**
 * Configuration de référence pour la gestion des transactions (dépréciée).
 *
 * <p>
 * <strong>NOTE IMPORTANTE</strong> : Cette classe est conservée uniquement pour documentation.
 * Le bean `transactionManager` a été déplacé dans {@link JpaConfig} pour éviter les conflits
 * de beans dupliqués.
 *
 * <p>
 * Le {@link org.springframework.orm.jpa.JpaTransactionManager} dans {@link JpaConfig} gère
 * maintenant à la fois :
 * <ul>
 * <li>Les transactions JPA (repositories JPA)</li>
 * <li>Les transactions JDBC (JdbcTemplate, NamedParameterJdbcTemplate)</li>
 * </ul>
 *
 * <p>
 * Utilisation dans les services :
 *
 * <pre>
 * {@code
 * &#64;Service
 * &#64;Transactional  // Utilise le transactionManager de JpaConfig
 * public class MonService {
 *     &#64;Transactional(readOnly = true)  // Lecture seule (optimisation)
 *     public List<Entity> findAll() { ... }
 *
 *     &#64;Transactional(rollbackFor = Exception.class)  // Rollback sur toute exception
 *     public void save(Entity entity) { ... }
 * }
 * }
 * </pre>
 *
 * <p>
 * Bonnes pratiques :
 * <ul>
 * <li>Utiliser {@code @Transactional} au niveau service, pas repository</li>
 * <li>Marquer les méthodes de lecture avec {@code readOnly = true}</li>
 * <li>Spécifier {@code rollbackFor} si nécessaire (par défaut : RuntimeException et Error)</li>
 * <li>Éviter les transactions trop longues (timeout configuré à 30s par défaut)</li>
 * </ul>
 *
 * @deprecated Le bean transactionManager a été déplacé dans JpaConfig pour éviter les conflits.
 *             Cette classe est conservée uniquement pour documentation.
 */
@Deprecated
public class TransactionConfig {

  /**
   * Le bean transactionManager a été déplacé dans {@link JpaConfig}.
   *
   * <p>Le {@link org.springframework.orm.jpa.JpaTransactionManager} dans JpaConfig gère
   * maintenant à la fois les transactions JPA et JDBC sur la datasource primaire.
   *
   * <p>Cette classe est conservée uniquement pour documentation des bonnes pratiques
   * de gestion des transactions.
   */
  private TransactionConfig() {
    // Classe de documentation uniquement - pas d'instanciation
  }
}
