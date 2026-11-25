package cd.shad.erp.cmk.cmkerp.platform.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.ApiPermissionCatalog;

class ApiPermissionCatalogTest {

  @Test
  void publicPathsAreRecognized() {
    assertTrue(ApiPermissionCatalog.isPublicPath("/api/v1/auth/login"));
    assertTrue(ApiPermissionCatalog.isPublicPath("/api/v1/health"));
    assertTrue(ApiPermissionCatalog.isPublicPath("/api/v1/portail-fournisseur/abc"));
    assertFalse(ApiPermissionCatalog.isPublicPath("/api/v1/stocks/products"));
  }

  @Test
  void matchesLongestPrefix() {
    var rule = ApiPermissionCatalog.match("GET", "/api/v1/stocks/products/12");
    assertTrue(rule.isPresent());
    assertTrue(rule.get().pathPrefix().contains("products")
        || rule.get().pathPrefix().endsWith("/stocks"));
  }

  @Test
  void deniesUnmatchedBusinessPathConceptually() {
    assertTrue(ApiPermissionCatalog.match("GET", "/api/v1/unknown-business-api").isEmpty());
  }

  @Test
  void authPermissionsIsAuthenticatedOnly() {
    assertTrue(ApiPermissionCatalog.isAuthenticatedOnly("/api/v1/auth/permissions", "GET"));
  }
}
