package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

/**
 * Garde-fous Hikari pour les bases externes (Mediline, CLINIQUE).
 *
 * <p>Objectif : ne jamais saturer ni bloquer les serveurs MySQL / SQL Server partagés.
 * Pool petit, idle à 0, timeouts courts, boot non bloquant.
 */
public final class ExternalHikariSupport {

  private static final Logger log = LoggerFactory.getLogger(ExternalHikariSupport.class);

  /** Plafond absolu : une instance cmkerp ne doit pas ouvrir plus de connexions externes. */
  public static final int HARD_CAP_MEDILINE = 8;
  public static final int HARD_CAP_CLINIQUE = 5;

  private ExternalHikariSupport() {}

  /**
   * Applique la politique anti-saturation pour un pool externe.
   *
   * @param config Hikari en cours de construction
   * @param poolLabel libellé logs (Mediline / CLINIQUE)
   * @param requestedMax taille demandée via YAML
   * @param hardCap plafond absolu pour cette base
   * @param connectionTimeoutMs timeout d'obtention (fail-fast)
   * @param leakDetectionMs seuil fuite (0 = off)
   * @param readOnly true pour CLINIQUE
   */
  public static void applyExternalPoolPolicy(
      HikariConfig config,
      String poolLabel,
      int requestedMax,
      int hardCap,
      long connectionTimeoutMs,
      long leakDetectionMs,
      boolean readOnly) {

    int maxPool = Math.max(1, Math.min(requestedMax, hardCap));
    if (requestedMax > hardCap) {
      log.warn(
          "{} : maximum-pool-size={} plafonné à {} pour protéger le serveur distant",
          poolLabel,
          requestedMax,
          hardCap);
    }

    config.setMaximumPoolSize(maxPool);
    // Ne garder aucune connexion idle : libérer dès que possible
    config.setMinimumIdle(0);
    // Fail-fast si le serveur distant est lent / saturé
    config.setConnectionTimeout(Math.max(3_000L, Math.min(connectionTimeoutMs, 15_000L)));
    // Libérer rapidement les connexions inutilisées
    config.setIdleTimeout(60_000L);
    // Recycler souvent (évite les sessions mortes côté SQL Server / MySQL)
    config.setMaxLifetime(600_000L);
    config.setKeepaliveTime(30_000L);
    config.setValidationTimeout(3_000L);
    if (leakDetectionMs > 0) {
      config.setLeakDetectionThreshold(Math.min(leakDetectionMs, 60_000L));
    }
    config.setRegisterMbeans(false);
    config.setAutoCommit(true);
    config.setReadOnly(readOnly);
    config.setConnectionTestQuery("SELECT 1");
    // Jamais bloquer le démarrage de cmkerp si la base externe est down
    config.setInitializationFailTimeout(-1);

    log.info(
        "{} pool externe -> max={}, minIdle=0, connTimeout={}ms, idle=60s, lifetime=10m, readOnly={}",
        poolLabel,
        maxPool,
        config.getConnectionTimeout(),
        readOnly);
  }
}
