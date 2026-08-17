package cd.shad.erp.cmk.cmkerp.gateway.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.gateway.dto.response.AuthResponse;
import cd.shad.erp.cmk.cmkerp.gateway.security.service.JwtBlacklistService;
import cd.shad.erp.cmk.cmkerp.gateway.security.service.RefreshTokenService;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.ChangePasswordRequest;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.SecurityMapper;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.UtilisateurCommandService;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.gateway.exception.RefreshTokenExpiredException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;
import lombok.RequiredArgsConstructor;

/**
 * Service d'authentification pour la gestion de l'authentification JWT.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UtilisateurRepository utilisateurRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final JwtBlacklistService jwtBlacklistService;
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final UtilisateurCommandService utilisateurCommandService;

  /**
   * Authentifie un utilisateur et génère les tokens JWT.
   *
   * @param username le nom d'utilisateur
   * @param password le mot de passe en clair
   * @param rememberMe true pour activer "Se souvenir de moi" (durée prolongée)
   * @return AuthResponse contenant les tokens et les permissions de l'utilisateur
   * @throws BusinessException si l'authentification échoue (utilisateur non trouvé, compte
   *         verrouillé, mot de passe incorrect)
   */
  @Transactional
  public AuthResponse login(String username, String password, boolean rememberMe) {
    log.debug("Tentative de connexion pour l'utilisateur: {}", username);

    // Facebook-Grade : Charger l'utilisateur ET le nom du rôle en une seule requête (jointure LEFT
    // JOIN)
    Optional<Map.Entry<Utilisateur, String>> utilisateurWithRoleOpt =
        utilisateurRepository.findByUsernameWithRole(username);

    if (utilisateurWithRoleOpt.isEmpty()) {
      log.warn("Utilisateur non trouvé: {}", username);
      throw new BusinessException("Nom d'utilisateur ou mot de passe incorrect");
    }

    Map.Entry<Utilisateur, String> entry = utilisateurWithRoleOpt.get();
    Utilisateur utilisateur = entry.getKey();
    String roleName = entry.getValue();

    log.debug("Utilisateur trouvé: id={}, username={}, locked={}, roleName={}", utilisateur.getId(),
        utilisateur.getUsername(), utilisateur.getLocked(), roleName);

    // Vérifier que le compte n'est pas verrouillé
    if (Boolean.TRUE.equals(utilisateur.getLocked())) {
      log.warn("Tentative de connexion sur un compte verrouillé: {}", username);
      throw new BusinessException("Le compte est verrouillé");
    }

    // Vérifier le mot de passe
    String motDePasseEnBase = utilisateur.getMotDePasse();
    if (motDePasseEnBase == null) {
      log.warn("Mot de passe null pour l'utilisateur: {}", username);
      throw new BusinessException("Nom d'utilisateur ou mot de passe incorrect");
    }

    log.debug("Vérification du mot de passe. Format en base: {} (début)",
        motDePasseEnBase.length() > 20 ? motDePasseEnBase.substring(0, 20) + "..."
            : motDePasseEnBase);

    boolean passwordMatches = passwordEncoder.matches(password, motDePasseEnBase);
    log.debug("Résultat de la vérification du mot de passe: {}", passwordMatches);

    if (!passwordMatches) {
      log.warn("Mot de passe incorrect pour l'utilisateur: {}", username);
      throw new BusinessException("Nom d'utilisateur ou mot de passe incorrect");
    }

    // Charger le rôle complet (nécessaire pour les permissions)
    // Note : roleName est déjà récupéré via la jointure, mais on a besoin de l'objet Role pour les
    // permissions
    Role role = null;
    if (utilisateur.getFkRole() != null) {
      role = roleRepository.findById(utilisateur.getFkRole()).orElse(null);
    }

    // Charger les permissions du rôle
    List<Permission> permissions = Collections.emptyList();
    if (role != null) {
      List<RolePermission> rolePermissions = rolePermissionRepository.findByRole(role.getId());
      List<Long> permissionIds = rolePermissions.stream().map(RolePermission::getFkPermission)
          .collect(Collectors.toList());
      permissions = permissionIds.stream().map(permissionRepository::findById)
          .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
          .collect(Collectors.toList());
    }

    // Construire UserPermissions avec les permissions chargées
    UserPermissions userPermissions =
        SecurityMapper.mapToUserPermissions(utilisateur, role, permissions, Collections.emptyList() // droitsPharmacies
                                                                                                    // :
                                                                                                    // vide
                                                                                                    // pour
                                                                                                    // l'instant
        );

    // Facebook-Grade : Utiliser le roleName récupéré via la jointure (plus performant)
    if (roleName != null && !roleName.isEmpty()) {
      userPermissions.setRoleName(roleName);
    }

    log.debug("Permissions chargées pour l'utilisateur {}: {}, roleName={}", username,
        userPermissions.getPermissions(), userPermissions.getRoleName());

    // Générer les tokens JWT avec support pour "Se souvenir de moi"
    // Utiliser les méthodes avec rememberMe si JwtTokenProviderImpl est disponible
    String accessToken;
    String refreshToken;

    if (jwtTokenProvider instanceof cd.shad.erp.cmk.cmkerp.gateway.security.JwtTokenProviderImpl providerImpl) {
      accessToken = providerImpl.generateAccessToken(userPermissions, rememberMe);
      refreshToken = providerImpl.generateRefreshToken(userPermissions, rememberMe);
    } else {
      // Fallback : utiliser les méthodes standard si l'implémentation ne supporte pas rememberMe
      accessToken = jwtTokenProvider.generateAccessToken(userPermissions);
      refreshToken = jwtTokenProvider.generateRefreshToken(userPermissions);
    }

    // Stocker le refresh token dans Redis pour la rotation
    refreshTokenService.storeRefreshToken(utilisateur.getId(), refreshToken);

    log.debug("Tokens générés pour l'utilisateur {} (rememberMe={})", username, rememberMe);

    return new AuthResponse(accessToken, refreshToken, userPermissions);
  }

  /**
   * Rafraîchit les tokens JWT avec persistance de 48h.
   *
   * <p>
   * STRATÉGIE : Persistance de 48h sans rotation du refresh token.
   * L'ancien refresh token reste valide jusqu'à son expiration naturelle (48h).
   * Le token n'est blacklisté que lors de la déconnexion explicite ou à l'expiration.
   *
   * @param refreshToken le refresh token actuel
   * @return AuthResponse contenant les nouveaux tokens
   * @throws BusinessException si le refresh token est invalide ou expiré
   */
  @Transactional
  public AuthResponse refreshToken(String refreshToken) {
    log.debug("Tentative de rafraîchissement de token");

    // Valider le refresh token (vérifie Redis et blacklist)
    Long userId = refreshTokenService.validateRefreshToken(refreshToken);
    if (userId == null) {
      // Utiliser l'exception dédiée pour une gestion spécifique côté handler
      throw new RefreshTokenExpiredException();
    }

    // Charger l'utilisateur
    var utilisateurOpt = utilisateurRepository.findById(userId);
    if (utilisateurOpt.isEmpty()) {
      log.warn("Utilisateur non trouvé pour le refresh token: userId={}", userId);
      throw new BusinessException("Utilisateur non trouvé");
    }

    Utilisateur utilisateur = utilisateurOpt.get();

    // Vérifier que le compte n'est pas verrouillé
    if (Boolean.TRUE.equals(utilisateur.getLocked())) {
      log.warn("Tentative de refresh sur un compte verrouillé: userId={}", userId);
      throw new BusinessException("Le compte est verrouillé");
    }

    // Charger le rôle et les permissions
    Role role = null;
    if (utilisateur.getFkRole() != null) {
      role = roleRepository.findById(utilisateur.getFkRole()).orElse(null);
    }

    List<Permission> permissions = Collections.emptyList();
    if (role != null) {
      List<RolePermission> rolePermissions = rolePermissionRepository.findByRole(role.getId());
      List<Long> permissionIds = rolePermissions.stream().map(RolePermission::getFkPermission)
          .collect(Collectors.toList());
      permissions = permissionIds.stream().map(permissionRepository::findById)
          .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
          .collect(Collectors.toList());
    }

    UserPermissions userPermissions = SecurityMapper.mapToUserPermissions(utilisateur, role,
        permissions, Collections.emptyList());

    // Générer de nouveaux tokens en préservant le paramètre rememberMe
    // Déterminer si le refresh token original avait rememberMe activé
    boolean rememberMe = false;
    if (jwtTokenProvider instanceof cd.shad.erp.cmk.cmkerp.gateway.security.JwtTokenProviderImpl providerImpl) {
      rememberMe = providerImpl.isRememberMeToken(refreshToken);
    }

    String newAccessToken;
    String newRefreshToken;

    if (jwtTokenProvider instanceof cd.shad.erp.cmk.cmkerp.gateway.security.JwtTokenProviderImpl providerImpl) {
      newAccessToken = providerImpl.generateAccessToken(userPermissions, rememberMe);
      newRefreshToken = providerImpl.generateRefreshToken(userPermissions, rememberMe);
    } else {
      // Fallback : utiliser les méthodes standard
      newAccessToken = jwtTokenProvider.generateAccessToken(userPermissions);
      newRefreshToken = jwtTokenProvider.generateRefreshToken(userPermissions);
    }

    // ✅ OPTIMISATION : Stocker le nouveau refresh token SANS révoquer l'ancien
    //
    // STRATÉGIE : Avec une persistance de 48h, on ne fait PAS de rotation du refresh token.
    // L'ancien token reste valide jusqu'à son expiration naturelle (48h).
    //
    // Avantages :
    // - Le token reste valide pendant 48h sans être blacklisté prématurément
    // - Les requêtes parallèles continuent de fonctionner avec l'ancien token
    // - Le token n'est blacklisté que lors de l'expiration (48h) ou de la déconnexion
    //
    // Sécurité : Le token expire naturellement après 48h, ce qui limite la fenêtre d'attaque
    refreshTokenService.storeRefreshToken(userId, newRefreshToken);

    // ⚠️ IMPORTANT : Ne PAS révoquer l'ancien refresh token
    // L'ancien token reste valide jusqu'à son expiration (48h)
    // Il sera automatiquement supprimé de Redis à l'expiration
    // Seule la déconnexion explicite blacklistera le token

    log.info("Tokens rafraîchis avec succès pour l'utilisateur {} (rememberMe={})", userId, rememberMe);

    return new AuthResponse(newAccessToken, newRefreshToken, userPermissions);
  }

  /**
   * Initialise le mot de passe lors de la première connexion (initPassword = false). Ne vérifie PAS
   * le mot de passe actuel et met initPassword à true après le changement.
   *
   * @param userId l'ID de l'utilisateur (extrait du JWT)
   * @param newPassword le nouveau mot de passe
   * @throws BusinessException si l'utilisateur n'existe pas ou si initPassword n'est pas false
   */
  @Transactional
  public void initPassword(Long userId, String newPassword) {
    log.debug("Initialisation du mot de passe pour l'utilisateur ID: {}", userId);

    // Charger l'utilisateur
    var utilisateurOpt = utilisateurRepository.findById(userId);
    if (utilisateurOpt.isEmpty()) {
      log.warn("Utilisateur non trouvé: ID={}", userId);
      throw new BusinessException("Utilisateur non trouvé");
    }

    var utilisateur = utilisateurOpt.get();

    // Vérifier que c'est bien un changement initial (initPassword doit être false)
    if (!Boolean.FALSE.equals(utilisateur.getInitPassword())) {
      log.warn(
          "Tentative d'initialisation de mot de passe pour un utilisateur avec initPassword=true: ID={}",
          userId);
      throw new BusinessException(
          "Cette opération n'est autorisée que lors de la première connexion");
    }

    // Convertir le DTO gateway en DTO platform
    ChangePasswordRequest platformRequest = new ChangePasswordRequest();
    platformRequest.setNewPassword(newPassword);

    // Changer le mot de passe via le service
    utilisateurCommandService.changePassword(userId, platformRequest, userId);

    // Mettre à jour initPassword à true après le changement réussi
    // Cela indique que l'utilisateur a initialisé son mot de passe et n'a plus besoin de le changer
    var utilisateurUpdatedOpt = utilisateurRepository.findById(userId);
    if (utilisateurUpdatedOpt.isPresent()) {
      var utilisateurUpdated = utilisateurUpdatedOpt.get();
      // Mettre à jour initPassword à true (l'utilisateur a initialisé son mot de passe)
      utilisateurUpdated.setInitPassword(true);
      utilisateurUpdated.setDateUpdate(LocalDateTime.now());
      utilisateurUpdated.setUserUpdatedId(userId);
      int rowsUpdated = utilisateurRepository.update(utilisateurUpdated);
      if (rowsUpdated == 0) {
        log.warn("Aucune ligne mise à jour pour initPassword pour l'utilisateur ID: {}", userId);
      } else {
        log.debug("initPassword mis à jour à true pour l'utilisateur ID: {}", userId);
      }
    } else {
      log.warn("Impossible de recharger l'utilisateur ID: {} pour mettre à jour initPassword",
          userId);
    }

    log.info("Mot de passe initialisé avec succès pour l'utilisateur ID: {}", userId);
  }

  /**
   * Change le mot de passe de l'utilisateur connecté depuis le profil. Vérifie STRICTEMENT le mot
   * de passe actuel et ne modifie PAS initPassword.
   *
   * @param userId l'ID de l'utilisateur (extrait du JWT)
   * @param request la requête contenant l'ancien et le nouveau mot de passe
   * @throws BusinessException si l'ancien mot de passe est incorrect, manquant ou si l'utilisateur
   *         n'existe pas
   */
  @Transactional
  public void changePassword(Long userId,
      cd.shad.erp.cmk.cmkerp.gateway.dto.request.ChangePasswordRequest request) {
    log.debug("Changement de mot de passe depuis le profil pour l'utilisateur ID: {}", userId);

    // Charger l'utilisateur
    var utilisateurOpt = utilisateurRepository.findById(userId);
    if (utilisateurOpt.isEmpty()) {
      log.warn("Utilisateur non trouvé: ID={}", userId);
      throw new BusinessException("Utilisateur non trouvé");
    }

    var utilisateur = utilisateurOpt.get();

    // Vérification STRICTE : l'ancien mot de passe est OBLIGATOIRE
    if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
      log.warn("Ancien mot de passe manquant pour un changement depuis le profil: ID={}", userId);
      throw new BusinessException("Le mot de passe actuel est obligatoire");
    }

    // Vérification STRICTE : l'ancien mot de passe doit être correct
    String motDePasseEnBase = utilisateur.getMotDePasse();
    if (motDePasseEnBase == null
        || !passwordEncoder.matches(request.getOldPassword(), motDePasseEnBase)) {
      log.warn("Ancien mot de passe incorrect pour l'utilisateur: ID={}", userId);
      throw new BusinessException("Le mot de passe actuel est incorrect");
    }

    // Convertir le DTO gateway en DTO platform
    ChangePasswordRequest platformRequest = new ChangePasswordRequest();
    platformRequest.setNewPassword(request.getNewPassword());

    // Changer le mot de passe via le service
    utilisateurCommandService.changePassword(userId, platformRequest, userId);

    // Révoquer tous les tokens de l'utilisateur après changement de mot de passe (sécurité)
    refreshTokenService.revokeAllUserRefreshTokens(userId);
    jwtBlacklistService.revokeAllUserTokens(userId);

    // IMPORTANT : Ne PAS modifier initPassword lors d'un changement depuis le profil
    // initPassword reste inchangé

    log.info("Mot de passe changé avec succès depuis le profil pour l'utilisateur ID: {}", userId);
  }
}
