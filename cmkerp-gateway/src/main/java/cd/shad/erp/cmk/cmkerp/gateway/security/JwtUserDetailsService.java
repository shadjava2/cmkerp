package cd.shad.erp.cmk.cmkerp.gateway.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Spring Security pour charger les détails d'un utilisateur depuis la base de données.
 *
 * <p>
 * Implémente UserDetailsService pour l'intégration avec Spring Security.
 * Charge les informations de l'utilisateur depuis UtilisateurRepository.
 *
 * <p>
 * L'identité vient du JWT ; l'autorisation métier est appliquée par
 * {@code PermissionAuthorizationPort} / {@code RbacEnforcementFilter} (cache),
 * pas par le claim permissions du token (volontairement absent pour limiter la taille cookie).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

  private final UtilisateurRepository utilisateurRepository;
  private final JwtTokenProviderImpl jwtTokenProvider;

  /**
   * Charge les détails d'un utilisateur par son username.
   *
   * <p>
   * Note: Cette méthode est appelée par Spring Security mais ne reçoit pas le token.
   * Les permissions doivent être chargées depuis le token dans le filtre JWT.
   *
   * @param username le nom d'utilisateur
   * @return UserDetails avec les informations de base (sans permissions)
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + username));

    // Créer un UserDetails avec les informations de base
    // Les permissions seront ajoutées dans le filtre JWT depuis le token
    return User.builder()
        .username(utilisateur.getUsername())
        .password(utilisateur.getMotDePasse() != null ? utilisateur.getMotDePasse() : "")
        .authorities(Collections.emptyList()) // Permissions chargées depuis le token dans le filtre
        .accountLocked(Boolean.TRUE.equals(utilisateur.getLocked()))
        .build();
  }

  /**
   * Charge les détails d'un utilisateur authentifié par JWT.
   *
   * <p>
   * Les authorities Spring peuvent être vides : le RBAC runtime lit le catalogue
   * via {@code PermissionAuthorizationPort} (userId du token).
   *
   * @param username le nom d'utilisateur
   * @param token le token JWT d'identité
   * @return UserDetails pour le SecurityContext
   */
  public UserDetails loadUserByUsernameWithToken(String username, String token) {
    Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + username));

    // Compatibilité tokens legacy qui embarquaient encore le claim "permissions"
    Set<String> permissions = jwtTokenProvider.getPermissionsFromToken(token);
    Collection<GrantedAuthority> authorities = permissions.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    if (log.isDebugEnabled()) {
      log.debug("JWT identité OK pour {} (authorities legacy={})", username, authorities.size());
    }

    return User.builder()
        .username(utilisateur.getUsername())
        .password(utilisateur.getMotDePasse() != null ? utilisateur.getMotDePasse() : "")
        .authorities(authorities)
        .accountLocked(Boolean.TRUE.equals(utilisateur.getLocked()))
        .build();
  }
}

