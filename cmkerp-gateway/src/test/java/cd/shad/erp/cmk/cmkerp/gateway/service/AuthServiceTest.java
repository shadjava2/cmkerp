package cd.shad.erp.cmk.cmkerp.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import cd.shad.erp.cmk.cmkerp.gateway.dto.response.AuthResponse;
import cd.shad.erp.cmk.cmkerp.gateway.security.service.JwtBlacklistService;
import cd.shad.erp.cmk.cmkerp.gateway.security.service.RefreshTokenService;
import cd.shad.erp.cmk.cmkerp.platform.security.application.service.UtilisateurCommandService;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.PermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;

/**
 * Tests unitaires pour AuthService.
 *
 * <p>
 * Tests de la logique métier d'authentification sans dépendances externes (DB, réseau, etc.).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - AuthService")
class AuthServiceTest {

  @Mock
  private UtilisateurRepository utilisateurRepository;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private PermissionRepository permissionRepository;

  @Mock
  private RolePermissionRepository rolePermissionRepository;

  @Mock
  private RefreshTokenService refreshTokenService;

  @Mock
  private JwtBlacklistService jwtBlacklistService;

  @Mock
  private UtilisateurCommandService utilisateurCommandService;

  @InjectMocks
  private AuthService authService;

  private Utilisateur utilisateurValide;
  private Utilisateur utilisateurVerrouille;
  private String passwordHash;

  @BeforeEach
  void setUp() {
    passwordHash = "$2a$10$abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUV";

    utilisateurValide = Utilisateur.builder().id(1L).username("testuser").motDePasse(passwordHash)
        .locked(false).fkRole(1L).build();

    utilisateurVerrouille = Utilisateur.builder().id(2L).username("lockeduser")
        .motDePasse(passwordHash).locked(true).fkRole(1L).build();
  }

  @Test
  @DisplayName("Login réussi avec credentials valides")
  void testLoginSuccess() {
    // Given
    String username = "testuser";
    String password = "password123";
    String accessToken = "access-token";
    String refreshToken = "refresh-token";
    String roleName = "ADMIN";

    Map.Entry<Utilisateur, String> utilisateurWithRole =
        new AbstractMap.SimpleEntry<>(utilisateurValide, roleName);

    Role role = Role.builder().id(1L).nom("ADMIN").build();

    when(utilisateurRepository.findByUsernameWithRole(username))
        .thenReturn(Optional.of(utilisateurWithRole));
    when(passwordEncoder.matches(password, passwordHash)).thenReturn(true);
    when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
    when(rolePermissionRepository.findByRole(1L)).thenReturn(Collections.emptyList());
    when(jwtTokenProvider.generateAccessToken(any(UserPermissions.class))).thenReturn(accessToken);
    when(jwtTokenProvider.generateRefreshToken(any(UserPermissions.class)))
        .thenReturn(refreshToken);

    // When
    AuthResponse response = authService.login(username, password, false);

    // Then
    assertNotNull(response);
    assertEquals(accessToken, response.accessToken());
    assertEquals(refreshToken, response.refreshToken());
    assertNotNull(response.user());
    verify(utilisateurRepository).findByUsernameWithRole(username);
    verify(passwordEncoder).matches(password, passwordHash);
    verify(jwtTokenProvider).generateAccessToken(any(UserPermissions.class));
    verify(jwtTokenProvider).generateRefreshToken(any(UserPermissions.class));
  }

  @Test
  @DisplayName("Login échoue si utilisateur n'existe pas")
  void testLoginUserNotFound() {
    // Given
    String username = "nonexistent";
    String password = "password123";

    when(utilisateurRepository.findByUsernameWithRole(username)).thenReturn(Optional.empty());

    // When / Then
    BusinessException exception = assertThrows(BusinessException.class, () -> {
      authService.login(username, password, false);
    });

    assertEquals("Nom d'utilisateur ou mot de passe incorrect", exception.getMessage());
    verify(utilisateurRepository).findByUsernameWithRole(username);
    verify(passwordEncoder, never()).matches(any(), any());
    verify(jwtTokenProvider, never()).generateAccessToken(any());
  }

  @Test
  @DisplayName("Login échoue si mot de passe incorrect")
  void testLoginWrongPassword() {
    // Given
    String username = "testuser";
    String password = "wrongpassword";
    String roleName = "ADMIN";

    Map.Entry<Utilisateur, String> utilisateurWithRole =
        new AbstractMap.SimpleEntry<>(utilisateurValide, roleName);

    when(utilisateurRepository.findByUsernameWithRole(username))
        .thenReturn(Optional.of(utilisateurWithRole));
    when(passwordEncoder.matches(password, passwordHash)).thenReturn(false);

    // When / Then
    BusinessException exception = assertThrows(BusinessException.class, () -> {
      authService.login(username, password, false);
    });

    assertEquals("Nom d'utilisateur ou mot de passe incorrect", exception.getMessage());
    verify(utilisateurRepository).findByUsernameWithRole(username);
    verify(passwordEncoder).matches(password, passwordHash);
    verify(jwtTokenProvider, never()).generateAccessToken(any());
  }

  @Test
  @DisplayName("Login échoue si compte verrouillé")
  void testLoginLockedAccount() {
    // Given
    String username = "lockeduser";
    String password = "password123";
    String roleName = "ADMIN";

    Map.Entry<Utilisateur, String> utilisateurWithRole =
        new AbstractMap.SimpleEntry<>(utilisateurVerrouille, roleName);

    when(utilisateurRepository.findByUsernameWithRole(username))
        .thenReturn(Optional.of(utilisateurWithRole));

    // When / Then
    BusinessException exception = assertThrows(BusinessException.class, () -> {
      authService.login(username, password, false);
    });

    assertEquals("Le compte est verrouillé", exception.getMessage());
    verify(utilisateurRepository).findByUsernameWithRole(username);
    verify(passwordEncoder, never()).matches(any(), any());
    verify(jwtTokenProvider, never()).generateAccessToken(any());
  }

  @Test
  @DisplayName("Login échoue si mot de passe null en base")
  void testLoginNullPassword() {
    // Given
    String username = "testuser";
    String password = "password123";
    String roleName = "ADMIN";
    Utilisateur utilisateurSansPassword =
        Utilisateur.builder().id(1L).username("testuser").motDePasse(null).locked(false).build();

    Map.Entry<Utilisateur, String> utilisateurWithRole =
        new AbstractMap.SimpleEntry<>(utilisateurSansPassword, roleName);

    when(utilisateurRepository.findByUsernameWithRole(username))
        .thenReturn(Optional.of(utilisateurWithRole));

    // When / Then
    BusinessException exception = assertThrows(BusinessException.class, () -> {
      authService.login(username, password, false);
    });

    assertEquals("Nom d'utilisateur ou mot de passe incorrect", exception.getMessage());
    verify(utilisateurRepository).findByUsernameWithRole(username);
    verify(passwordEncoder, never()).matches(any(), any());
  }
}

