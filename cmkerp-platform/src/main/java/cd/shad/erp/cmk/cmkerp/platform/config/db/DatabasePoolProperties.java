package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du pool de connexions HikariCP.
 *
 * <p>
 * Cette classe centralise la configuration du pool de connexions pour permettre un tuning fin selon
 * l'environnement (dev/prod) et le nombre d'instances.
 *
 * <p>
 * Configuration dans application-*.yml :
 *
 * <pre>{@code
 * cmkerp:
 *   db:
 *     pool:
 *       max-size: 60
 *       min-idle: 20
 *       connection-timeout-ms: 30000
 *       idle-timeout-ms: 600000
 *       max-lifetime-ms: 1800000
 *       keepalive-time-ms: 120000
 * }</pre>
 *
 * <p>
 * Recommandations pour la production :
 * <ul>
 * <li>max-size : calculer selon (max_connections MySQL / nombre d'instances) - marge de
 * sécurité</li>
 * <li>min-idle : environ 30-40% de max-size pour maintenir des connexions chaudes</li>
 * <li>max-lifetime : doit être inférieur à wait_timeout MySQL (généralement 28800s = 8h)</li>
 * <li>idle-timeout : environ 10 minutes pour libérer les connexions inactives</li>
 * </ul>
 *
 * <p>
 * Note : Ajuster ces valeurs selon :
 * <ul>
 * <li>Le nombre d'instances de l'application (ex: 3 instances → max-size = 60 par instance)</li>
 * <li>Le max_connections MySQL (ex: 200 → 200 / 3 instances = ~66 par instance, avec marge =
 * 60)</li>
 * <li>Les paramètres MySQL wait_timeout et interactive_timeout</li>
 * </ul>
 *
 *
 */
@ConfigurationProperties(prefix = "cmkerp.db.pool")
public class DatabasePoolProperties {

  /**
   * Taille maximale du pool de connexions.
   *
   * <p>
   * Ajuster selon :
   * <ul>
   * <li>Nombre d'instances : max-size = (max_connections MySQL / nombre_instances) - marge</li>
   * <li>Exemple : MySQL max_connections=200, 3 instances → max-size = (200/3) - 10 = ~60</li>
   * </ul>
   */
  private int maxSize = 40;

  /**
   * Nombre minimum de connexions idle maintenues dans le pool.
   *
   * <p>
   * Recommandation : 30-40% de max-size pour maintenir des connexions chaudes.
   */
  private int minIdle = 10;

  /**
   * Timeout pour obtenir une connexion depuis le pool (millisecondes).
   *
   * <p>
   * Si aucune connexion n'est disponible dans ce délai, une exception est levée.
   */
  private long connectionTimeoutMs = 30000;

  /**
   * Timeout avant qu'une connexion idle soit retirée du pool (millisecondes).
   *
   * <p>
   * Recommandation : 10 minutes (600000ms) pour libérer les connexions inactives.
   */
  private long idleTimeoutMs = 600000;

  /**
   * Durée maximale de vie d'une connexion (millisecondes).
   *
   * <p>
   * IMPORTANT : Doit être inférieur à wait_timeout MySQL (généralement 28800s = 8h = 28800000ms).
   *
   * <p>
   * Vérifier les paramètres MySQL :
   *
   * <pre>{@code
   * SHOW VARIABLES LIKE 'wait_timeout';
   * SHOW VARIABLES LIKE 'interactive_timeout';
   * }</pre>
   *
   * <p>
   * Recommandation : max-lifetime = wait_timeout - 30 minutes (marge de sécurité). Exemple :
   * wait_timeout = 8h → max-lifetime = 7h30 = 27000000ms.
   */
  private long maxLifetimeMs = 1800000; // 30 minutes par défaut (dev)

  /**
   * Intervalle entre les keepalive probes (millisecondes).
   *
   * <p>
   * Permet de détecter les connexions mortes avant qu'elles ne soient utilisées. Recommandation : 2
   * minutes (120000ms).
   */
  private long keepaliveTimeMs = 120000;

  /**
   * Timeout pour la validation d'une connexion (millisecondes).
   *
   * <p>
   * Temps maximum pour valider qu'une connexion est encore valide avant de l'utiliser. Doit être
   * inférieur à connectionTimeout. Recommandation : 3 secondes (3000ms).
   *
   * <p>
   * Si non configuré, HikariCP utilise 5 secondes par défaut. Réduire à 3s améliore la réactivité
   * lors de la détection de connexions mortes.
   */
  private long validationTimeoutMs = 3000;

  // Getters et Setters

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  public int getMinIdle() {
    return minIdle;
  }

  public void setMinIdle(int minIdle) {
    this.minIdle = minIdle;
  }

  public long getConnectionTimeoutMs() {
    return connectionTimeoutMs;
  }

  public void setConnectionTimeoutMs(long connectionTimeoutMs) {
    this.connectionTimeoutMs = connectionTimeoutMs;
  }

  public long getIdleTimeoutMs() {
    return idleTimeoutMs;
  }

  public void setIdleTimeoutMs(long idleTimeoutMs) {
    this.idleTimeoutMs = idleTimeoutMs;
  }

  public long getMaxLifetimeMs() {
    return maxLifetimeMs;
  }

  public void setMaxLifetimeMs(long maxLifetimeMs) {
    this.maxLifetimeMs = maxLifetimeMs;
  }

  public long getKeepaliveTimeMs() {
    return keepaliveTimeMs;
  }

  public void setKeepaliveTimeMs(long keepaliveTimeMs) {
    this.keepaliveTimeMs = keepaliveTimeMs;
  }

  public long getValidationTimeoutMs() {
    return validationTimeoutMs;
  }

  public void setValidationTimeoutMs(long validationTimeoutMs) {
    this.validationTimeoutMs = validationTimeoutMs;
  }
}

