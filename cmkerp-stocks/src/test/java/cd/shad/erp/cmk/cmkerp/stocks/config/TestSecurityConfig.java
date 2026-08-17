package cd.shad.erp.cmk.cmkerp.stocks.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;

/**
 * Configuration de test pour fournir les beans de sécurité nécessaires aux tests d'intégration.
 *
 * <p>
 * Cette configuration fournit des mocks/stubs pour les beans de sécurité qui ne sont normalement
 * disponibles que dans le module gateway.
 */
@TestConfiguration
public class TestSecurityConfig {

  /**
   * Fournit un mock de JwtTokenProvider pour les tests.
   *
   * <p>
   * Cette implémentation de test retourne des valeurs simples pour permettre aux tests de passer
   * sans avoir besoin d'une vraie implémentation JWT.
   */
  @Bean
  @Primary
  public JwtTokenProvider jwtTokenProvider() {
    return new JwtTokenProvider() {
      @Override
      public String generateAccessToken(UserPermissions userPermissions) {
        return "mock-access-token";
      }

      @Override
      public String generateRefreshToken(UserPermissions userPermissions) {
        return "mock-refresh-token";
      }

      @Override
      public boolean validateToken(String token) {
        return token != null && !token.isEmpty();
      }

      @Override
      public String getUsernameFromToken(String token) {
        return "test-user";
      }

      @Override
      public Long getUserIdFromToken(String token) {
        return 1L;
      }
    };
  }
}
