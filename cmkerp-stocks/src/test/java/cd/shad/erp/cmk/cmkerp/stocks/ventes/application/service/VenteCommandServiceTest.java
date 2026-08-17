package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.VenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper.VenteMapper;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.Vente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.VenteRepository;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.infrastructure.persistence.VenteStoredProcedureRepository;

/**
 * Tests unitaires pour VenteCommandService.
 *
 * <p>
 * Tests de la logique métier de gestion des ventes sans dépendances externes (DB, réseau, etc.).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - VenteCommandService")
class VenteCommandServiceTest {

  @Mock
  private VenteRepository venteRepository;

  @Mock
  private VenteMapper venteMapper;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private VenteStoredProcedureRepository venteStoredProcedureRepository;

  @InjectMocks
  private VenteCommandService venteCommandService;

  private VenteRequest venteRequest;
  private Vente vente;
  private VenteResponse venteResponse;
  private Long currentUserId;

  @BeforeEach
  void setUp() {
    currentUserId = 1L;

    venteRequest =
        VenteRequest.builder().fkPharmacie(1L).raisonsortie("Test").demandeur("Test User").build();

    vente = Vente.builder().id(1L).fkPharmacie(1L).raisonsortie("Test").demandeur("Test User")
        .statut(Vente.StatutVente.EN_ATTENTE).dateCreate(LocalDateTime.now())
        .userCreatedId(currentUserId).build();

    venteResponse = VenteResponse.builder().id(1L).fkPharmacie(1L).raisonsortie("Test")
        .demandeur("Test User").statut("EN ATTENTE").pharmacieNom("Pharmacie Test").build();

    // Désactiver les stored procedures par défaut pour les tests
    ReflectionTestUtils.setField(venteCommandService, "useStoredProcedures", false);
  }

  @Test
  @DisplayName("Création d'une vente réussie")
  void testCreateVenteSuccess() {
    // Given
    when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(1L))).thenReturn(1L); // Pharmacie
                                                                                                 // existe
    when(venteMapper.toEntity(venteRequest)).thenReturn(vente);
    // Simuler que l'ID est généré après save
    ArgumentCaptor<Vente> venteCaptor = ArgumentCaptor.forClass(Vente.class);
    when(venteRepository.save(venteCaptor.capture())).thenAnswer(invocation -> {
      Vente savedVente = venteCaptor.getValue();
      // Simuler que l'ID est généré après save
      ReflectionTestUtils.setField(savedVente, "id", 1L);
      return 1;
    });
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(vente, "Pharmacie Test")).thenReturn(venteResponse);

    // When
    VenteResponse response = venteCommandService.create(venteRequest, currentUserId);

    // Then
    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("Test", response.getRaisonsortie());
    verify(venteRepository).save(any(Vente.class));
    verify(venteRepository).findById(1L);
  }

  @Test
  @DisplayName("Création échoue si pharmacie n'existe pas")
  void testCreateVentePharmacieNotFound() {
    // Given
    when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(1L))).thenReturn(0L); // Pharmacie
                                                                                                 // n'existe
                                                                                                 // pas

    // When / Then
    assertThrows(NotFoundException.class, () -> {
      venteCommandService.create(venteRequest, currentUserId);
    });

    verify(venteRepository, never()).save(any(Vente.class));
  }

  @Test
  @DisplayName("Mise à jour d'une vente réussie")
  void testUpdateVenteSuccess() {
    // Given
    VenteRequest updateRequest =
        VenteRequest.builder().fkPharmacie(1L).raisonsortie("Updated").build();

    Vente updatedVente = Vente.builder().id(1L).fkPharmacie(1L).raisonsortie("Updated")
        .statut(Vente.StatutVente.EN_ATTENTE).build();

    VenteResponse updatedResponse = VenteResponse.builder().id(1L).raisonsortie("Updated")
        .pharmacieNom("Pharmacie Test").build();

    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(1L))).thenReturn(1L);
    when(venteRepository.update(any(Vente.class))).thenReturn(1);
    when(venteRepository.findById(1L)).thenReturn(Optional.of(updatedVente));
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(updatedVente, "Pharmacie Test")).thenReturn(updatedResponse);

    // When
    VenteResponse response = venteCommandService.update(1L, updateRequest, currentUserId);

    // Then
    assertNotNull(response);
    assertEquals("Updated", response.getRaisonsortie());
    verify(venteRepository).update(any(Vente.class));
  }

  @Test
  @DisplayName("Mise à jour échoue si vente n'existe pas")
  void testUpdateVenteNotFound() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.empty());

    // When / Then
    assertThrows(NotFoundException.class, () -> {
      venteCommandService.update(1L, venteRequest, currentUserId);
    });

    verify(venteRepository, never()).update(any(Vente.class));
  }

  @Test
  @DisplayName("Mise à jour échoue si vente est déjà validée")
  void testUpdateVenteAlreadyValidated() {
    // Given
    Vente validatedVente = Vente.builder().id(1L).statut(Vente.StatutVente.SORTIE_USAGE).build();

    when(venteRepository.findById(1L)).thenReturn(Optional.of(validatedVente));

    // When / Then
    assertThrows(BusinessException.class, () -> {
      venteCommandService.update(1L, venteRequest, currentUserId);
    });

    verify(venteRepository, never()).update(any(Vente.class));
  }

  @Test
  @DisplayName("Validation d'une vente réussie (sans stored procedure)")
  void testValiderVenteSuccess() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(venteRepository.update(any(Vente.class))).thenReturn(1);

    // When
    venteCommandService.valider(1L, currentUserId, "SORTIE-USAGE");

    // Then
    verify(venteRepository).findById(1L);
    verify(venteRepository).update(any(Vente.class));
  }

  @Test
  @DisplayName("Validation échoue si vente n'existe pas")
  void testValiderVenteNotFound() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.empty());

    // When / Then
    assertThrows(NotFoundException.class, () -> {
      venteCommandService.valider(1L, currentUserId, "SORTIE-USAGE");
    });

    verify(venteRepository, never()).update(any(Vente.class));
  }

  @Test
  @DisplayName("Validation avec stored procedure réussie")
  void testValiderVenteWithStoredProcedure() {
    // Given
    ReflectionTestUtils.setField(venteCommandService, "useStoredProcedures", true);

    VenteStoredProcedureRepository.SpResult successResult =
        new VenteStoredProcedureRepository.SpResult(0, "Validation réussie");

    when(venteStoredProcedureRepository.validateVente(1L, "SORTIE-USAGE", currentUserId))
        .thenReturn(successResult);

    // When
    venteCommandService.valider(1L, currentUserId, "SORTIE-USAGE");

    // Then
    verify(venteStoredProcedureRepository).validateVente(1L, "SORTIE-USAGE", currentUserId);
    verify(venteRepository, never()).findById(any());
    verify(venteRepository, never()).update(any());
  }

  @Test
  @DisplayName("Validation avec stored procedure échoue si code de retour != 0")
  void testValiderVenteWithStoredProcedureFailure() {
    // Given
    ReflectionTestUtils.setField(venteCommandService, "useStoredProcedures", true);

    VenteStoredProcedureRepository.SpResult errorResult =
        new VenteStoredProcedureRepository.SpResult(5, "Stock insuffisant");

    when(venteStoredProcedureRepository.validateVente(1L, "SORTIE-USAGE", currentUserId))
        .thenReturn(errorResult);

    // When / Then
    assertThrows(BusinessException.class, () -> {
      venteCommandService.valider(1L, currentUserId, "SORTIE-USAGE");
    });

    verify(venteStoredProcedureRepository).validateVente(1L, "SORTIE-USAGE", currentUserId);
  }

  @Test
  @DisplayName("Annulation d'une vente réussie")
  void testAnnulerVenteSuccess() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(venteRepository.update(any(Vente.class))).thenReturn(1);

    // When
    venteCommandService.annuler(1L, currentUserId);

    // Then
    verify(venteRepository).findById(1L);
    verify(venteRepository).update(any(Vente.class));
  }

  @Test
  @DisplayName("Annulation avec remboursement réussie")
  void testAnnulerVenteAvecRemboursementSuccess() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(venteRepository.update(any(Vente.class))).thenReturn(1);

    // When
    venteCommandService.annulerAvecRemboursement(1L, currentUserId);

    // Then
    verify(venteRepository).findById(1L);
    verify(venteRepository).update(any(Vente.class));
  }
}
