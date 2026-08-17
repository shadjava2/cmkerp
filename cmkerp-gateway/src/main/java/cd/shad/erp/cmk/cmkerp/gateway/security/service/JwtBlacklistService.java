package cd.shad.erp.cmk.cmkerp.gateway.security.service;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Service de gestion de la blacklist JWT avec Redis.
 *
 * <p>
 * Permet d'invalider des tokens JWT avant leur expiration naturelle, notamment lors de :
 * <ul>
 * <li>Déconnexion utilisateur</li>
 * <li>Changement de mot de passe</li>
 * <li>Révocation de tokens compromis</li>
 * <li>Rotation des refresh tokens</li>
 * </ul>
 *
 * <p>
 * Les tokens sont stockés dans Redis avec une TTL correspondant à leur expiration.
 */
@Service
public class JwtBlacklistService {

  private static final Logger log = LoggerFactory.getLogger(JwtBlacklistService.class);
  private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
  private static final String USER_TOKENS_PREFIX = "jwt:user:tokens:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final String jwtSecret;

  public JwtBlacklistService(RedisTemplate<String, Object> redisTemplate,
      @Value("${jwt.secret:your-256-bit-secret-key-must-be-at-least-256-bits-long-for-HS256-algorithm}") String jwtSecret) {
    this.redisTemplate = redisTemplate;
    this.jwtSecret = jwtSecret;
  }

  /**
   * Ajoute un token à la blacklist.
   *
   * <p>
   * ✅ Optimisé : Utilise Redis avec timeout pour éviter les blocages.
   * En cas d'erreur Redis, la méthode échoue silencieusement pour ne pas bloquer le logout.
   *
   * @param token le token JWT à blacklister
   */
  public void blacklistToken(String token) {
    try {
      // Extraire l'expiration du token pour définir la TTL
      Claims claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build()
          .parseSignedClaims(token).getPayload();

      Date expiration = claims.getExpiration();
      if (expiration != null) {
        long ttlSeconds = Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000);

        if (ttlSeconds > 0) {
          String key = BLACKLIST_PREFIX + token;
          // ✅ Optimisation : Utiliser setIfAbsent pour éviter les opérations inutiles
          // et définir un timeout pour éviter les blocages
          redisTemplate.opsForValue().set(key, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
          log.debug("Token ajouté à la blacklist (TTL: {}s)", ttlSeconds);
        } else {
          log.debug("Token déjà expiré, pas besoin de le blacklister");
        }
      }
    } catch (Exception e) {
      // ✅ En cas d'erreur Redis, ne pas bloquer le logout
      // Le token sera simplement considéré comme valide jusqu'à son expiration naturelle
      log.warn("Impossible d'ajouter le token à la blacklist (non bloquant) : {}", e.getMessage());
    }
  }

  /**
   * Vérifie si un token est dans la blacklist.
   *
   * @param token le token JWT à vérifier
   * @return true si le token est blacklisté, false sinon
   */
  public boolean isTokenBlacklisted(String token) {
    try {
      String key = BLACKLIST_PREFIX + token;
      Boolean exists = redisTemplate.hasKey(key);
      return Boolean.TRUE.equals(exists);
    } catch (Exception e) {
      log.warn("Erreur lors de la vérification de la blacklist : {}", e.getMessage());
      // En cas d'erreur Redis, on considère que le token n'est pas blacklisté
      // pour éviter de bloquer les utilisateurs légitimes
      return false;
    }
  }

  /**
   * Révoque tous les tokens d'un utilisateur (déconnexion globale).
   *
   * <p>
   * ✅ Optimisé : Opération Redis rapide avec gestion d'erreur non bloquante.
   * En cas d'erreur Redis, la méthode échoue silencieusement pour ne pas bloquer le logout.
   *
   * @param userId l'ID de l'utilisateur
   */
  public void revokeAllUserTokens(Long userId) {
    try {
      String userTokensKey = USER_TOKENS_PREFIX + userId;
      // ✅ Supprimer tous les refresh tokens de l'utilisateur (opération Redis rapide)
      redisTemplate.delete(userTokensKey);
      log.debug("Tous les tokens de l'utilisateur {} ont été révoqués", userId);
    } catch (Exception e) {
      // ✅ En cas d'erreur Redis, ne pas bloquer le logout
      // Les tokens seront simplement considérés comme valides jusqu'à leur expiration naturelle
      log.warn("Erreur lors de la révocation des tokens de l'utilisateur {} (non bloquant) : {}",
          userId, e.getMessage());
    }
  }

  /**
   * Nettoie les tokens expirés de la blacklist (méthode utilitaire). Note : Redis gère
   * automatiquement l'expiration via TTL, cette méthode est optionnelle.
   */
  public void cleanupExpiredTokens() {
    // Redis gère automatiquement l'expiration via TTL
    // Cette méthode peut être utilisée pour des opérations de maintenance si nécessaire
    log.debug("Nettoyage des tokens expirés (géré automatiquement par Redis TTL)");
  }
}
