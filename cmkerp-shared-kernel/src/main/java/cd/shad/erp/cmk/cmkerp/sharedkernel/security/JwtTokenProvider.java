package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

/**
 * Provider pour la génération et la validation des tokens JWT.
 *
 * <p>
 * Cette classe doit être implémentée pour générer les tokens d'accès et de rafraîchissement
 * à partir des informations de l'utilisateur.
 */
public interface JwtTokenProvider {

  /**
   * Génère un token d'accès pour l'utilisateur.
   *
   * @param userPermissions les permissions de l'utilisateur
   * @return le token JWT d'accès
   */
  String generateAccessToken(UserPermissions userPermissions);

  /**
   * Génère un token de rafraîchissement pour l'utilisateur.
   *
   * @param userPermissions les permissions de l'utilisateur
   * @return le token JWT de rafraîchissement
   */
  String generateRefreshToken(UserPermissions userPermissions);

  /**
   * Valide un token JWT.
   *
   * @param token le token à valider
   * @return true si le token est valide, false sinon
   */
  boolean validateToken(String token);

  /**
   * Extrait le username depuis un token JWT.
   *
   * @param token le token JWT
   * @return le username extrait du token
   */
  String getUsernameFromToken(String token);

  /**
   * Extrait le userId depuis un token JWT.
   *
   * @param token le token JWT
   * @return le userId extrait du token, ou null si non présent
   */
  Long getUserIdFromToken(String token);
}

