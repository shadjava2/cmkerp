package cd.shad.erp.cmk.cmkerp.platform.config.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du pool de connexions Redis (Lettuce).
 *
 * <p>
 * Cette classe centralise la configuration du pool Redis pour permettre un tuning fin selon
 * l'environnement (dev/prod) et le trafic attendu.
 *
 * <p>
 * Configuration dans application-*.yml :
 * 
 * <pre>{@code
 * cmkerp:
 *   redis:
 *     pool:
 *       max-active: 64
 *       max-idle: 32
 *       min-idle: 8
 *       time-between-eviction-runs-ms: 30000
 *       number-of-instances: 3  # Optionnel : active la vérification maxclients
 * }</pre>
 *
 * <p>
 * Recommandations pour la production :
 * <ul>
 * <li>max-active : 64-128 selon le trafic (plusieurs instances partagent le même Redis)</li>
 * <li>max-idle : environ 50% de max-active pour maintenir des connexions chaudes</li>
 * <li>min-idle : environ 10-15% de max-active pour éviter les créations fréquentes</li>
 * <li>time-between-eviction-runs : 30 secondes pour nettoyer les connexions idle</li>
 * </ul>
 *
 * <p>
 * Ajuster ces valeurs selon :
 * <ul>
 * <li>Le nombre d'instances de l'application (ex: 3 instances × 64 max-active = 192 connexions
 * Redis)</li>
 * <li>La capacité Redis (maxclients) : vérifier avec CONFIG GET maxclients</li>
 * <li>Le trafic attendu : plus d'instances = plus de connexions nécessaires</li>
 * </ul>
 *
 * 
 */
@ConfigurationProperties(prefix = "cmkerp.redis.pool")
public class RedisPoolProperties {

  /**
   * Nombre maximum de connexions actives dans le pool.
   *
   * <p>
   * Ajuster selon :
   * <ul>
   * <li>Nombre d'instances : max-active × nombre_instances ≤ maxclients Redis</li>
   * <li>Exemple : 3 instances × 64 max-active = 192 connexions (vérifier maxclients ≥ 200)</li>
   * </ul>
   */
  private int maxActive = 8;

  /**
   * Nombre maximum de connexions idle dans le pool.
   *
   * <p>
   * Recommandation : environ 50% de max-active.
   */
  private int maxIdle = 8;

  /**
   * Nombre minimum de connexions idle maintenues dans le pool.
   *
   * <p>
   * Recommandation : environ 10-15% de max-active pour éviter les créations fréquentes.
   */
  private int minIdle = 0;

  /**
   * Intervalle entre les runs d'éviction des connexions idle (millisecondes).
   *
   * <p>
   * Recommandation : 30 secondes (30000ms) pour nettoyer les connexions inactives.
   */
  private long timeBetweenEvictionRunsMs = 30000;

  /**
   * Nombre d'instances de l'application (pour vérification maxclients Redis).
   *
   * <p>
   * Utilisé pour calculer le nombre total de connexions : max-active × nombre_instances
   * <ul>
   * <li>Si non spécifié, la vérification maxclients est ignorée</li>
   * <li>Exemple : 3 instances × 64 max-active = 192 connexions max</li>
   * <li>Vérification : 192 ≤ maxclients Redis (avec marge de sécurité)</li>
   * </ul>
   */
  private Integer numberOfInstances;

  // Getters et Setters

  public int getMaxActive() {
    return maxActive;
  }

  public void setMaxActive(int maxActive) {
    this.maxActive = maxActive;
  }

  public int getMaxIdle() {
    return maxIdle;
  }

  public void setMaxIdle(int maxIdle) {
    this.maxIdle = maxIdle;
  }

  public int getMinIdle() {
    return minIdle;
  }

  public void setMinIdle(int minIdle) {
    this.minIdle = minIdle;
  }

  public long getTimeBetweenEvictionRunsMs() {
    return timeBetweenEvictionRunsMs;
  }

  public void setTimeBetweenEvictionRunsMs(long timeBetweenEvictionRunsMs) {
    this.timeBetweenEvictionRunsMs = timeBetweenEvictionRunsMs;
  }

  public Integer getNumberOfInstances() {
    return numberOfInstances;
  }

  public void setNumberOfInstances(Integer numberOfInstances) {
    this.numberOfInstances = numberOfInstances;
  }
}

