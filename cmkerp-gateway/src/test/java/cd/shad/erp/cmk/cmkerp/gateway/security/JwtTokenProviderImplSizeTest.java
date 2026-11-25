package cd.shad.erp.cmk.cmkerp.gateway.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;

/**
 * Garde-fou : le JWT d'accès ne doit pas embarquer le catalogue permissions
 * (sinon Set-Cookie dépasse maxHttpHeaderSize / limites navigateur).
 */
class JwtTokenProviderImplSizeTest {

  private JwtTokenProviderImpl provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProviderImpl();
    ReflectionTestUtils.setField(provider, "jwtSecret",
        "test-secret-key-must-be-at-least-256-bits-long-for-hs256-ok");
    ReflectionTestUtils.setField(provider, "accessTokenExpiration", 3_600_000L);
    ReflectionTestUtils.setField(provider, "refreshTokenExpiration", 86_400_000L);
    ReflectionTestUtils.setField(provider, "relaxedValidation", false);
  }

  @Test
  void accessToken_staysCompact_evenWithHugePermissionCatalog() {
    Set<String> permissions = new HashSet<>();
    for (int i = 0; i < 300; i++) {
      permissions.add("MODULE_FACTURATION_REFERENTIAL_PERMISSION_" + i);
    }

    UserPermissions user = UserPermissions.builder()
        .userId(1L)
        .roleId(5L)
        .username("schadrack")
        .locked(false)
        .permissions(permissions)
        .build();

    String token = provider.generateAccessToken(user, true);
    int bytes = token.getBytes(StandardCharsets.US_ASCII).length;

    assertTrue(bytes < 2048, "access JWT trop gros pour cookie: " + bytes + " octets");
    assertTrue(provider.getPermissionsFromToken(token).isEmpty());
    assertFalse(token.contains("MODULE_FACTURATION"));
  }
}
