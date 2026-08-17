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
 * Facebook-Grade : Les permissions sont chargées depuis le token JWT pour éviter
 * les requêtes DB à chaque requête. Si les permissions ne sont pas dans le token,
 * elles sont chargées depuis la base de données.
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
   * Charge les détails d'un utilisateur avec les permissions depuis le token JWT.
   *
   * <p>
   * Facebook-Grade : Cette méthode extrait les permissions directement du token JWT,
   * évitant ainsi une requête DB supplémentaire à chaque requête HTTP.
   *
   * @param username le nom d'utilisateur
   * @param token le token JWT contenant les permissions
   * @return UserDetails avec les permissions extraites du token
   */
  public UserDetails loadUserByUsernameWithToken(String username, String token) {
    Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + username));

    // Extraire les permissions depuis le token JWT
    Set<String> permissions = jwtTokenProvider.getPermissionsFromToken(token);

    // ✅ SECURITY SCOPE REDUCTION: Les permissions du token sont conservées mais ne sont plus utilisées pour l'autorisation
    // On garde les permissions dans les authorities pour compatibilité (ne casse pas la génération du token)
    // mais elles ne sont plus utilisées pour bloquer l'accès (AUTH uniquement, pas de RBAC/ACL)
    Collection<GrantedAuthority> authorities = permissions.stream()
        .map(permission -> new SimpleGrantedAuthority(permission))
        .collect(Collectors.toList());

    if (log.isDebugEnabled()) {
      log.debug("Permissions chargées depuis le token pour {}: {}", username, permissions);
    }

    return User.builder()
        .username(utilisateur.getUsername())
        .password(utilisateur.getMotDePasse() != null ? utilisateur.getMotDePasse() : "")
        .authorities(authorities)
        .accountLocked(Boolean.TRUE.equals(utilisateur.getLocked()))
        .build();
  }
}

