package cd.shad.erp.cmk.cmkerp.platform.config.cache;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * Vérificateur automatique de la configuration Redis maxclients au démarrage.
 *
 * <p>
 * Cette classe vérifie que le nombre total de connexions Redis nécessaires (max-active ×
 * nombre-instances) ne dépasse pas la limite maxclients configurée sur le serveur Redis.
 *
 * <p>
 * Activation conditionnelle :
 * <ul>
 * <li>Nécessite que {@code cmkerp.redis.pool.number-of-instances} soit configuré</li>
 * <li>Nécessite que Redis soit disponible</li>
 * </ul>
 *
 * <p>
 * Formule de vérification :
 *
 * <pre>
 * (max-active × nombre-instances) + 20% ≤ maxclients Redis
 * </pre>
 *
 * <p>
 * Exemple :
 * <ul>
 * <li>3 instances × 64 max-active = 192 connexions</li>
 * <li>192 + 20% = 230 connexions nécessaires</li>
 * <li>maxclients Redis doit être ≥ 230</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cmkerp.redis.pool.number-of-instances")
public class RedisMaxClientsChecker implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(RedisMaxClientsChecker.class);

  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisPoolProperties poolProperties;

  @Override
  public void run(String... args) {
    try {
      checkMaxClients();
    } catch (Exception e) {
      log.warn(
          "⚠️ [RedisMaxClientsChecker] Impossible de vérifier maxclients Redis : {}. Vérifiez manuellement avec : CONFIG GET maxclients",
          e.getMessage());
    }
  }

  /**
   * Vérifie que la configuration maxclients Redis est suffisante.
   */
  private void checkMaxClients() {
    if (poolProperties.getNumberOfInstances() == null
        || poolProperties.getNumberOfInstances() <= 0) {
      log.debug(
          "[RedisMaxClientsChecker] Vérification désactivée : number-of-instances non configuré");
      return;
    }

    // Calculer le nombre total de connexions nécessaires
    int maxActive = poolProperties.getMaxActive();
    int numberOfInstances = poolProperties.getNumberOfInstances();
    int totalConnections = maxActive * numberOfInstances;

    // Ajouter 20% de marge de sécurité
    int requiredMaxClients = (int) (totalConnections * 1.20);

    try {
      // Récupérer maxclients depuis Redis via INFO CLIENTS (plus fiable que CONFIG GET)
      String info = redisTemplate.execute(connection -> {
        Properties infoProps = connection.serverCommands().info("clients");
        if (infoProps == null) {
          return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : infoProps.stringPropertyNames()) {
          sb.append(key).append(":").append(infoProps.getProperty(key)).append("\n");
        }
        return sb.toString();
      }, true);

      // Parser la ligne "maxclients:XXXX" depuis INFO CLIENTS
      int maxClients = 10000; // Valeur par défaut si non trouvée
      if (info != null && !info.isEmpty()) {
        String[] lines = info.split("\r?\n");
        for (String line : lines) {
          if (line.startsWith("maxclients:")) {
            String value = line.substring("maxclients:".length()).trim();
            maxClients = Integer.parseInt(value);
            break;
          }
        }
      }

      // Vérifier si maxclients est suffisant
      if (maxClients >= requiredMaxClients) {
        log.info(
            "✅ [RedisMaxClientsChecker] Configuration Redis valide : maxclients={} ≥ connexions_requises={} ({} max-active × {} instances + 20%)",
            maxClients, requiredMaxClients, maxActive, numberOfInstances);
      } else {
        log.error(
            "❌ [RedisMaxClientsChecker] Configuration Redis INSUFFISANTE : maxclients={} < connexions_requises={} ({} max-active × {} instances + 20%). "
                + "Augmentez maxclients Redis avec : CONFIG SET maxclients {}",
            maxClients, requiredMaxClients, maxActive, numberOfInstances, requiredMaxClients);
      }
    } catch (Exception e) {
      log.warn(
          "⚠️ [RedisMaxClientsChecker] Erreur lors de la récupération de maxclients : {}. Vérifiez manuellement avec : CONFIG GET maxclients",
          e.getMessage());
    }
  }
}
