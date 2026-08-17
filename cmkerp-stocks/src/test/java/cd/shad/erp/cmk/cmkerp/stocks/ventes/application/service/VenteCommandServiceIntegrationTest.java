package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.config.TestApplication;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.VenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;

/**
 * Tests d'intégration pour VenteCommandService.
 *
 * <p>
 * Ces tests nécessitent une base de données réelle (H2 en mémoire pour les tests). Ils vérifient
 * l'intégration complète entre le service, le repository et la base de données.
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'intégration - VenteCommandService")
class VenteCommandServiceIntegrationTest {

  @Autowired
  private VenteCommandService venteCommandService;

  @Autowired
  private VenteQueryService venteQueryService;

  @Test
  @DisplayName("Création d'une vente avec base de données réelle")
  @Sql(scripts = "/test-data/pharmacies.sql",
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  void testCreateVenteIntegration() {
    // Given
    VenteRequest request = VenteRequest.builder().fkPharmacie(1L).raisonsortie("Test Integration")
        .demandeur("Test User").build();

    Long currentUserId = 1L;

    // When
    VenteResponse response = venteCommandService.create(request, currentUserId);

    // Then
    assertNotNull(response);
    assertNotNull(response.getId());
    assertEquals("Test Integration", response.getRaisonsortie());
    assertEquals("Test User", response.getDemandeur());
  }

  @Test
  @DisplayName("Création échoue si pharmacie n'existe pas en base")
  void testCreateVentePharmacieNotFoundIntegration() {
    // Given
    VenteRequest request = VenteRequest.builder().fkPharmacie(99999L) // Pharmacie inexistante
        .raisonsortie("Test").build();

    // When / Then
    assertThrows(NotFoundException.class, () -> {
      venteCommandService.create(request, 1L);
    });
  }

  @Test
  @DisplayName("Validation d'une vente avec base de données réelle")
  @Sql(scripts = {"/test-data/pharmacies.sql", "/test-data/ventes.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @org.junit.jupiter.api.Disabled("Désactivé temporairement - problème de configuration de stored procedure SP_VALIDATE_VENTE")
  void testValiderVenteIntegration() {
    // Given
    Long venteId = 1L;
    Long currentUserId = 1L;

    // When
    venteCommandService.valider(venteId, currentUserId, "SORTIE-USAGE");

    // Then
    // Vérifier que la vente a été validée en récupérant son statut
    VenteResponse vente = venteQueryService.findById(venteId);
    assertEquals("SORTIE-USAGE", vente.getStatut());
  }
}
