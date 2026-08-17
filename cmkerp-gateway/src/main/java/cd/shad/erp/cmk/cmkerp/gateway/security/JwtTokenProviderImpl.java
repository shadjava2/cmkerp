package cd.shad.erp.cmk.cmkerp.gateway.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import cd.shad.erp.cmk.cmkerp.gateway.security.service.JwtBlacklistService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * Implémentation de JwtTokenProvider utilisant jjwt.
 *
 * <p>
 * Génère et valide les tokens JWT pour l'authentification.
 *
 * <p>
 * Configuration via application.properties :
 * <ul>
 * <li>jwt.secret : clé secrète pour signer les tokens (minimum 256 bits)</li>
 * <li>jwt.access-token-expiration : durée de validité du token d'accès (en millisecondes, défaut:
 * 3600000 = 1h)</li>
 * <li>jwt.refresh-token-expiration : durée de validité du token de rafraîchissement (en
 * millisecondes, défaut: 86400000 = 24h)</li>
 * <li>jwt.dev.relaxed-validation : en mode dev, permet une validation plus permissive (défaut: true)</li>
 * </ul>
 *
 * <p>
 * Intègre la vérification de la blacklist Redis pour invalider les tokens avant leur expiration.
 */
@Slf4j
@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

  @Value("${jwt.secret:your-256-bit-secret-key-must-be-at-least-256-bits-long-for-HS256-algorithm}")
  private String jwtSecret;

  @Value("${jwt.access-token-expiration:172800000}") // 48 heures par défaut (172800000 ms = 48h)
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration:172800000}") // 48 heures par défaut (172800000 ms = 48h)
  private long refreshTokenExpiration;

  @Value("${jwt.dev.relaxed-validation:true}") // Validation plus permissive en dev par défaut
  private boolean relaxedValidation;

  // Durées pour "Se souvenir de moi" (48h pour access et refresh - même durée que normale)
  // Note: Avec la persistance de 48h, rememberMe utilise la même durée
  private static final long REMEMBER_ME_ACCESS_TOKEN_EXPIRATION = 172800000L; // 48 heures
  private static final long REMEMBER_ME_REFRESH_TOKEN_EXPIRATION = 172800000L; // 48 heures

  private JwtBlacklistService jwtBlacklistService;
  private Environment environment;

  @Autowired(required = false)
  public void setJwtBlacklistService(JwtBlacklistService jwtBlacklistService) {
    this.jwtBlacklistService = jwtBlacklistService;
  }

  @Autowired
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /**
   * Vérifie si on est en mode développement.
   */
  private boolean isDevelopment() {
    if (environment == null) {
      return false;
    }
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles == null || activeProfiles.length == 0) {
      // Si aucun profil n'est actif, vérifier le profil par défaut
      return "dev".equals(environment.getDefaultProfiles()[0]);
    }
    for (String profile : activeProfiles) {
      if ("dev".equals(profile) || "development".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes());
  }

  @Override
  public String generateAccessToken(UserPermissions userPermissions) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userPermissions.getUserId());
    claims.put("roleId", userPermissions.getRoleId());
    claims.put("username", userPermissions.getUsername());
    claims.put("locked", userPermissions.isLocked());

    // Facebook-Grade : Inclure les permissions dans le token pour éviter les requêtes DB à chaque requête
    // Les permissions sont stockées comme une liste de strings dans le claim "permissions"
    if (userPermissions.getPermissions() != null && !userPermissions.getPermissions().isEmpty()) {
      claims.put("permissions", new java.util.ArrayList<>(userPermissions.getPermissions()));
    }

    return Jwts.builder().claims(claims).subject(userPermissions.getUsername()).issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(getSigningKey()).compact();
  }

  @Override
  public String generateRefreshToken(UserPermissions userPermissions) {
    return generateRefreshToken(userPermissions, false);
  }

  /**
   * Génère un token de rafraîchissement avec support pour "Se souvenir de moi".
   *
   * @param userPermissions les permissions de l'utilisateur
   * @param rememberMe true pour une durée prolongée (90 jours), false pour durée normale (7 jours)
   * @return le token JWT de rafraîchissement
   */
  public String generateRefreshToken(UserPermissions userPermissions, boolean rememberMe) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    claims.put("userId", userPermissions.getUserId());
    // Stocker rememberMe dans le claim pour pouvoir le vérifier plus tard
    claims.put("rememberMe", rememberMe);

    long expiration = rememberMe
        ? REMEMBER_ME_REFRESH_TOKEN_EXPIRATION
        : refreshTokenExpiration;

    return Jwts.builder().claims(claims).subject(userPermissions.getUsername()).issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey()).compact();
  }

  /**
   * Génère un token d'accès avec support pour "Se souvenir de moi".
   *
   * @param userPermissions les permissions de l'utilisateur
   * @param rememberMe true pour une durée prolongée (30 jours), false pour durée normale (24 heures)
   * @return le token JWT d'accès
   */
  public String generateAccessToken(UserPermissions userPermissions, boolean rememberMe) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userPermissions.getUserId());
    claims.put("roleId", userPermissions.getRoleId());
    claims.put("username", userPermissions.getUsername());
    claims.put("locked", userPermissions.isLocked());

    // Facebook-Grade : Inclure les permissions dans le token pour éviter les requêtes DB à chaque requête
    if (userPermissions.getPermissions() != null && !userPermissions.getPermissions().isEmpty()) {
      claims.put("permissions", new java.util.ArrayList<>(userPermissions.getPermissions()));
    }

    long expiration = rememberMe
        ? REMEMBER_ME_ACCESS_TOKEN_EXPIRATION
        : accessTokenExpiration;

    return Jwts.builder().claims(claims).subject(userPermissions.getUsername()).issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey()).compact();
  }

  @Override
  public boolean validateToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      if (isDevelopment() && relaxedValidation) {
        log.debug("[JWT] Token null ou vide - rejeté (mode dev avec validation permissive)");
      }
      return false;
    }

    try {
      // Vérifier d'abord la signature et l'expiration
      Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
          .getPayload();

      // Vérifier si le token est dans la blacklist (si le service est disponible)
      if (jwtBlacklistService != null && jwtBlacklistService.isTokenBlacklisted(token)) {
        if (isDevelopment() && relaxedValidation) {
          log.warn("[JWT] Token blacklisté - rejeté (mode dev avec validation permissive)");
        }
        return false;
      }

      // En mode dev avec validation permissive, logger les détails du token
      if (isDevelopment() && relaxedValidation) {
        Date expiration = claims.getExpiration();
        String username = claims.getSubject();
        Object userId = claims.get("userId");
        log.debug("[JWT] Token valide - username: {}, userId: {}, expiration: {}", username, userId,
            expiration);
      }

      return true;
    } catch (ExpiredJwtException e) {
      // Token expiré
      if (isDevelopment() && relaxedValidation) {
        log.warn("[JWT] Token expiré - username: {}, expiration: {} (mode dev avec validation permissive)",
            e.getClaims().getSubject(), e.getClaims().getExpiration());
        // En mode dev avec validation permissive, on peut accepter les tokens expirés pour le debug
        // Mais par défaut, on les rejette quand même pour la sécurité
        return false;
      }
      return false;
    } catch (MalformedJwtException e) {
      // Token malformé
      if (isDevelopment() && relaxedValidation) {
        log.error("[JWT] Token malformé: {}", e.getMessage());
      }
      return false;
    } catch (SignatureException e) {
      // Signature invalide
      if (isDevelopment() && relaxedValidation) {
        log.error("[JWT] Signature invalide: {}", e.getMessage());
      }
      return false;
    } catch (Exception e) {
      // Autre erreur
      if (isDevelopment() && relaxedValidation) {
        log.error("[JWT] Erreur lors de la validation du token: {} - {}", e.getClass().getSimpleName(),
            e.getMessage());
      }
      return false;
    }
  }

  @Override
  public String getUsernameFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    return claims.getSubject();
  }

  @Override
  public Long getUserIdFromToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
      Object userIdObj = claims.get("userId");
      if (userIdObj == null) {
        return null;
      }
      // Gérer différents types numériques (Long, Integer, etc.)
      if (userIdObj instanceof Long) {
        return (Long) userIdObj;
      } else if (userIdObj instanceof Integer) {
        return ((Integer) userIdObj).longValue();
      } else if (userIdObj instanceof Number) {
        return ((Number) userIdObj).longValue();
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Extrait les permissions depuis un token JWT.
   *
   * @param token le token JWT
   * @return la liste des permissions (codes de permissions), ou une liste vide si non présentes
   */
  public java.util.Set<String> getPermissionsFromToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
      Object permissionsObj = claims.get("permissions");
      if (permissionsObj == null) {
        return java.util.Collections.emptySet();
      }

      // Les permissions sont stockées comme une liste dans le claim
      if (permissionsObj instanceof java.util.List) {
        @SuppressWarnings("unchecked")
        java.util.List<String> permissionsList = (java.util.List<String>) permissionsObj;
        return new java.util.HashSet<>(permissionsList);
      }

      return java.util.Collections.emptySet();
    } catch (Exception e) {
      return java.util.Collections.emptySet();
    }
  }

  /**
   * Extrait la date d'expiration depuis un token JWT.
   *
   * @param token le token JWT
   * @return la date d'expiration, ou null si le token est invalide
   */
  public Date getExpirationDateFromToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
      return claims.getExpiration();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Détermine si un refresh token a été créé avec "Se souvenir de moi" activé.
   * Vérifie d'abord le claim "rememberMe" dans le token, puis la durée de vie restante en fallback.
   *
   * @param refreshToken le refresh token à vérifier
   * @return true si rememberMe était activé lors de la création du token
   */
  public boolean isRememberMeToken(String refreshToken) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(refreshToken).getPayload();

      // Vérifier d'abord le claim "rememberMe" (plus fiable)
      Object rememberMeObj = claims.get("rememberMe");
      if (rememberMeObj instanceof Boolean) {
        return (Boolean) rememberMeObj;
      }

      // Fallback : vérifier la durée de vie restante
      Date expiration = claims.getExpiration();
      if (expiration == null) {
        return false;
      }

      // Calculer la durée de vie restante en millisecondes
      long remainingTime = expiration.getTime() - System.currentTimeMillis();
      // Si la durée restante est supérieure à 60 jours, c'est qu'il a été créé avec rememberMe (90 jours)
      long sixtyDaysInMs = 60L * 24 * 3600 * 1000;
      return remainingTime > sixtyDaysInMs;
    } catch (Exception e) {
      return false;
    }
  }
}

