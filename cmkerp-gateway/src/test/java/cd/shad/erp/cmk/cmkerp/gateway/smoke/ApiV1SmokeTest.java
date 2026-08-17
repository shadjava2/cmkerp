package cd.shad.erp.cmk.cmkerp.gateway.smoke;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.API_V1;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import cd.shad.erp.cmk.cmkerp.gateway.dto.request.LoginRequest;

/**
 * Tests de fumée (smoke tests) pour valider que l'API v1 est correctement exposée.
 *
 * <p>
 * Ces tests vérifient que :
 * <ul>
 * <li>Les endpoints principaux de l'API v1 sont accessibles (pas de 404)</li>
 * <li>Les routes sont correctement câblées</li>
 * <li>La structure de versioning est respectée</li>
 * </ul>
 *
 * <p>
 * <strong>Note :</strong> Ces tests ne vérifient pas la logique métier, seulement que les endpoints
 * existent et répondent (même avec 401/403 si non authentifié). Le but est de détecter rapidement
 * les régressions lors de refactoring.
 *
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Tests de fumée - API v1")
class ApiV1SmokeTest {

  @Autowired
  private TestRestTemplate restTemplate;

  /**
   * Test que l'endpoint de santé est accessible.
   *
   * <p>
   * Vérifie que GET /api/v1/health (ou équivalent) répond sans erreur 404. Note : L'endpoint peut
   * retourner 200, 401, ou 403 selon la configuration, mais jamais 404 si la route est correctement
   * câblée.
   */
  @Test
  @DisplayName("GET /api/v1/health - Endpoint de santé accessible")
  void testHealthEndpoint() {
    // Act
    ResponseEntity<String> response = restTemplate.getForEntity(API_V1 + "/health", String.class);

    // Assert
    // L'endpoint doit exister (pas de 404)
    // Il peut retourner 200, 401, ou 403 selon la configuration de sécurité
    assertThat(response.getStatusCode()).as("L'endpoint de santé doit exister (pas de 404)")
        .isNotEqualTo(HttpStatus.NOT_FOUND);
  }

  /**
   * Test que l'endpoint d'authentification est accessible.
   *
   * <p>
   * Vérifie que POST /api/v1/auth/login répond sans erreur 404. Avec des credentials invalides, on
   * s'attend à 401 (Unauthorized), mais jamais 404 si la route est correctement câblée.
   */
  @Test
  @DisplayName("POST /api/v1/auth/login - Endpoint d'authentification accessible")
  void testAuthLoginEndpoint() {
    // Arrange
    LoginRequest loginRequest = new LoginRequest("test-user", "test-password", false);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

    // Act
    ResponseEntity<String> response =
        restTemplate.exchange(API_V1 + "/auth/login", HttpMethod.POST, request, String.class);

    // Assert
    // L'endpoint doit exister (pas de 404)
    // Avec des credentials invalides, on s'attend à 401 (Unauthorized)
    assertThat(response.getStatusCode())
        .as("L'endpoint d'authentification doit exister (pas de 404)")
        .isNotEqualTo(HttpStatus.NOT_FOUND);

    // Si l'endpoint existe, il doit retourner 401 (credentials invalides) ou 400 (validation)
    assertThat(response.getStatusCode())
        .as("Avec des credentials invalides, on s'attend à 401 ou 400")
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.BAD_REQUEST);
  }

  /**
   * Test que les endpoints protégés sont accessibles.
   *
   * <p>
   * Vérifie que les endpoints GET répondent sans erreur 404. Sans authentification, on s'attend à
   * 401 (Unauthorized) ou 403 (Forbidden), mais jamais 404 si la route est correctement câblée.
   *
   * @param endpointPath le chemin de l'endpoint à tester (sans le préfixe /api/v1)
   * @param displayName le nom d'affichage pour le test
   */
  @ParameterizedTest(name = "{1}")
  @CsvSource({"/sites, GET /api/v1/sites - Endpoint de liste des sites accessible",
      "/users, GET /api/v1/users - Endpoint de liste des utilisateurs accessible",
      "/pharmacies, GET /api/v1/pharmacies - Endpoint de liste des pharmacies accessible",
      "/notifications, GET /api/v1/notifications - Endpoint de liste des notifications accessible",
      "/roles, GET /api/v1/roles - Endpoint de liste des rôles accessible",
      "/permissions, GET /api/v1/permissions - Endpoint de liste des permissions accessible",
      "/dashboard/pharmacies?userId=1, GET /api/v1/dashboard/pharmacies - Endpoint de dashboard accessible",
      "/pos/dashboard/stats, GET /api/v1/pos/dashboard/stats - Dashboard POS stats accessible",
      "/pos/products, GET /api/v1/pos/products - POS produits accessible",
      "/pos/references/formes, GET /api/v1/pos/references/formes - POS références accessible",
      "/pos/fournisseurs, GET /api/v1/pos/fournisseurs - POS fournisseurs accessible",
      "/pos/transferts-internes, GET /api/v1/pos/transferts-internes - POS transferts internes accessible",
      "/stocks/dashboard/stats, GET /api/v1/stocks/dashboard/stats - Dashboard Stocks stats accessible"})
  @DisplayName("Tests d'accessibilité des endpoints protégés")
  void testProtectedEndpoints(String endpointPath, String displayName) {
    // Act
    ResponseEntity<String> response =
        restTemplate.getForEntity(API_V1 + endpointPath, String.class);

    // Assert
    // L'endpoint doit exister (pas de 404)
    // Sans authentification, on s'attend à 401 (Unauthorized) ou 403 (Forbidden)
    assertThat(response.getStatusCode()).as("L'endpoint %s doit exister (pas de 404)", endpointPath)
        .isNotEqualTo(HttpStatus.NOT_FOUND);

    // Si l'endpoint existe, il doit retourner 401 (non authentifié) ou 403 (non autorisé)
    assertThat(response.getStatusCode())
        .as("Sans authentification, on s'attend à 401 ou 403 pour %s", endpointPath)
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }
}
