package cd.shad.erp.cmk.cmkerp.gateway.security.service;

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
 * Service de gestion des refresh tokens avec persistance de 48h.
 *
 * <p>
 * STRATÉGIE : Les refresh tokens ont une durée de vie de 48h et ne sont PAS révoqués lors du refresh.
 * L'ancien token reste valide jusqu'à son expiration naturelle (48h).
 *
 * <p>
 * Comportement :
 * <ul>
 * <li>Lors du refresh, un nouveau refresh token est généré et stocké</li>
 * <li>L'ancien refresh token reste valide jusqu'à son expiration (48h)</li>
 * <li>Les refresh tokens sont stockés dans Redis avec TTL de 48h</li>
 * <li>Le token n'est blacklisté que lors de la déconnexion explicite ou à l'expiration</li>
 * </ul>
 *
 * <p>
 * Avantages :
 * <ul>
 * <li>Persistance de 48h sans blacklistage prématuré</li>
 * <li>Les requêtes parallèles continuent de fonctionner avec l'ancien token</li>
 * <li>Expiration automatique après 48h (géré par Redis TTL)</li>
 * <li>Blacklistage uniquement lors de la déconnexion ou de l'expiration</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
  private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";
  private static final String USER_REFRESH_TOKEN_KEY = "jwt:user:refresh:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final JwtBlacklistService jwtBlacklistService;
  private final String jwtSecret;

  @Value("${jwt.refresh-token-expiration:86400000}") // 24 heures par défaut
  private long refreshTokenExpiration;

  public RefreshTokenService(RedisTemplate<String, Object> redisTemplate,
      JwtBlacklistService jwtBlacklistService,
      @Value("${jwt.secret:your-256-bit-secret-key-must-be-at-least-256-bits-long-for-HS256-algorithm}") String jwtSecret) {
    this.redisTemplate = redisTemplate;
    this.jwtBlacklistService = jwtBlacklistService;
    this.jwtSecret = jwtSecret;
  }

  /**
   * Stocke un refresh token pour un utilisateur.
   *
   * @param userId l'ID de l'utilisateur
   * @param refreshToken le refresh token à stocker
   */
  public void storeRefreshToken(Long userId, String refreshToken) {
    try {
      // Stocker le refresh token avec userId comme clé
      String key = USER_REFRESH_TOKEN_KEY + userId;
      long ttlSeconds = refreshTokenExpiration / 1000;

      redisTemplate.opsForValue().set(key, refreshToken, ttlSeconds, TimeUnit.SECONDS);

      // Stocker également le mapping inverse (refreshToken -> userId) pour validation rapide
      String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
      redisTemplate.opsForValue().set(tokenKey, userId.toString(), ttlSeconds, TimeUnit.SECONDS);

      log.debug("Refresh token stocké pour l'utilisateur {} (TTL: {}s)", userId, ttlSeconds);
    } catch (Exception e) {
      log.warn("Erreur lors du stockage du refresh token pour l'utilisateur {} : {}", userId,
          e.getMessage());
    }
  }

  /**
   * Vérifie si un refresh token est valide (existe et n'est pas blacklisté).
   *
   * <p>
   * IMPORTANT : Cette méthode vérifie la validité SANS révoquer le token.
   * Le token n'est blacklisté que lors de la déconnexion explicite ou à l'expiration (48h).
   *
   * @param refreshToken le refresh token à vérifier
   * @return l'ID de l'utilisateur si le token est valide, null sinon
   */
  public Long validateRefreshToken(String refreshToken) {
    try {
      // Vérifier d'abord si le token est blacklisté
      // Note: Un token peut être blacklisté lors de la déconnexion explicite ou à l'expiration
      if (jwtBlacklistService.isTokenBlacklisted(refreshToken)) {
        log.warn("Refresh token blacklisté détecté - token révoqué lors de la déconnexion ou expiré");
        return null;
      }

      // Vérifier si le token existe dans Redis
      String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
      Object userIdObj = redisTemplate.opsForValue().get(tokenKey);

      if (userIdObj == null) {
        log.debug("Refresh token non trouvé dans Redis - peut être expiré ou déjà révoqué");
        return null;
      }

      Long userId = Long.parseLong(userIdObj.toString());
      log.debug("Refresh token valide pour l'utilisateur {}", userId);
      return userId;

    } catch (Exception e) {
      log.warn("Erreur lors de la validation du refresh token : {}", e.getMessage());
      return null;
    }
  }

  /**
   * Révoque un refresh token (rotation). Invalide l'ancien token et le supprime de Redis.
   *
   * @param refreshToken l'ancien refresh token à révoquer
   * @param userId l'ID de l'utilisateur
   */
  public void revokeRefreshToken(String refreshToken, Long userId) {
    try {
      // Ajouter l'ancien token à la blacklist
      jwtBlacklistService.blacklistToken(refreshToken);

      // Supprimer le mapping refreshToken -> userId
      String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
      redisTemplate.delete(tokenKey);

      // Supprimer également le mapping userId -> refreshToken
      String userKey = USER_REFRESH_TOKEN_KEY + userId;
      redisTemplate.delete(userKey);

      log.debug("Refresh token révoqué pour l'utilisateur {}", userId);
    } catch (Exception e) {
      log.warn("Erreur lors de la révocation du refresh token : {}", e.getMessage());
    }
  }

  /**
   * Révoque tous les refresh tokens d'un utilisateur.
   *
   * @param userId l'ID de l'utilisateur
   */
  public void revokeAllUserRefreshTokens(Long userId) {
    try {
      // Récupérer le refresh token actuel de l'utilisateur
      String userKey = USER_REFRESH_TOKEN_KEY + userId;
      Object refreshTokenObj = redisTemplate.opsForValue().get(userKey);

      if (refreshTokenObj != null) {
        String refreshToken = refreshTokenObj.toString();
        revokeRefreshToken(refreshToken, userId);
      }

      log.info("Tous les refresh tokens de l'utilisateur {} ont été révoqués", userId);
    } catch (Exception e) {
      log.warn("Erreur lors de la révocation de tous les refresh tokens de l'utilisateur {} : {}",
          userId, e.getMessage());
    }
  }

  /**
   * Extrait l'ID utilisateur depuis un refresh token (sans validation Redis). Utile pour extraire
   * l'userId avant la validation complète.
   *
   * @param refreshToken le refresh token
   * @return l'ID de l'utilisateur ou null si le token est invalide
   */
  public Long extractUserIdFromToken(String refreshToken) {
    try {
      Claims claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build()
          .parseSignedClaims(refreshToken).getPayload();

      Object userIdObj = claims.get("userId");
      if (userIdObj == null) {
        return null;
      }

      if (userIdObj instanceof Long) {
        return (Long) userIdObj;
      } else if (userIdObj instanceof Integer) {
        return ((Integer) userIdObj).longValue();
      } else if (userIdObj instanceof Number) {
        return ((Number) userIdObj).longValue();
      }

      return null;
    } catch (Exception e) {
      log.debug("Impossible d'extraire userId du refresh token : {}", e.getMessage());
      return null;
    }
  }
}
