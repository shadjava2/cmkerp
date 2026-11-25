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

  @Test
  void dmiCardexSearchDoesNotAcceptModuleDmiAlone() {
    var rule = ApiPermissionCatalog.match("GET", "/api/v1/cardex");
    assertTrue(rule.isPresent());
    for (String code : rule.get().anyOfPermissions()) {
      assertFalse(code.equals("MODULE_DMI") || code.startsWith("MODULE_DMI"),
          "MODULE_DMI ne doit pas autoriser /api/v1/cardex");
    }
  }

  @Test
  void dmiMedilineSearchDoesNotAcceptModuleDmiAlone() {
    var rule = ApiPermissionCatalog.match("GET", "/api/v1/mediline/patients");
    assertTrue(rule.isPresent());
    for (String code : rule.get().anyOfPermissions()) {
      assertFalse(code.equals("MODULE_DMI"),
          "MODULE_DMI ne doit pas autoriser /api/v1/mediline");
    }
  }

  @Test
  void unmatchedBusinessApiIsDeniedByDefault() {
    assertTrue(ApiPermissionCatalog.match("GET", "/api/v1/totally-unknown-resource").isEmpty());
  }

  @Test
  void facturationContratsEndpointsAreCovered() {
    assertTrue(ApiPermissionCatalog.match("GET", "/api/v1/facturation/contrats").isPresent());
    assertTrue(ApiPermissionCatalog.match("GET", "/api/v1/facturation/contrats/12").isPresent());
    assertTrue(ApiPermissionCatalog.match("POST", "/api/v1/facturation/contrats").isPresent());
    assertTrue(ApiPermissionCatalog.match("PUT", "/api/v1/facturation/contrats/12").isPresent());
    assertTrue(ApiPermissionCatalog.match("PUT", "/api/v1/facturation/contrats/12/actif").isPresent());
    assertTrue(ApiPermissionCatalog.match("POST", "/api/v1/facturation/contrats/import/dry-run").isPresent());
    assertTrue(ApiPermissionCatalog.match("POST", "/api/v1/facturation/contrats/import").isPresent());
    assertTrue(ApiPermissionCatalog.match("GET", "/api/v1/facturation/clinic-sync/status").isPresent());

    var list = ApiPermissionCatalog.match("GET", "/api/v1/facturation/contrats").orElseThrow();
    for (String code : list.anyOfPermissions()) {
      assertFalse(code.equals("MODULE_FACTURATION"),
          "MODULE_FACTURATION ne doit pas autoriser la liste contrats");
    }

    var dryRun = ApiPermissionCatalog.match("POST", "/api/v1/facturation/contrats/import/dry-run")
        .orElseThrow();
    assertTrue(dryRun.pathPrefix().contains("dry-run"));
  }

  @Test
  void stocksFacturationSyncRemainsCovered() {
    assertTrue(ApiPermissionCatalog.match("GET", "/api/v1/stocks/facturation-sync").isPresent());
    assertTrue(ApiPermissionCatalog.match("POST", "/api/v1/stocks/facturation-sync/apply").isPresent());
  }
}
