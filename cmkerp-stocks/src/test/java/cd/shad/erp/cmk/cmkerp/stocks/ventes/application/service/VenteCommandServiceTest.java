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

/**
 * Tests unitaires pour VenteCommandService (logique métier sans SP).
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
  }

  @Test
  @DisplayName("Création d'une vente réussie")
  void testCreateVenteSuccess() {
    when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(1L))).thenReturn(1L);
    when(venteMapper.toEntity(venteRequest)).thenReturn(vente);
    ArgumentCaptor<Vente> venteCaptor = ArgumentCaptor.forClass(Vente.class);
    when(venteRepository.save(venteCaptor.capture())).thenAnswer(invocation -> {
      ReflectionTestUtils.setField(venteCaptor.getValue(), "id", 1L);
      return 1;
    });
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(vente, "Pharmacie Test")).thenReturn(venteResponse);

    VenteResponse response = venteCommandService.create(venteRequest, currentUserId);

    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("Test", response.getRaisonsortie());
    verify(venteRepository).save(any(Vente.class));
    verify(venteRepository).findById(1L);
  }

  @Test
  @DisplayName("Création échoue si pharmacie n'existe pas")
  void testCreateVentePharmacieNotFound() {
    when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(1L))).thenReturn(0L);

    assertThrows(NotFoundException.class, () -> {
      venteCommandService.create(venteRequest, currentUserId);
    });

    verify(venteRepository, never()).save(any(Vente.class));
  }

  @Test
  @DisplayName("Mise à jour d'une vente réussie")
  void testUpdateVenteSuccess() {
    VenteRequest updateRequest =
        VenteRequest.builder().fkPharmacie(1L).raisonsortie("Updated").build();

    Vente updatedVente = Vente.builder().id(1L).fkPharmacie(1L).raisonsortie("Updated")
        .statut(Vente.StatutVente.EN_ATTENTE).build();

    VenteResponse updatedResponse = VenteResponse.builder().id(1L).raisonsortie("Updated")
        .pharmacieNom("Pharmacie Test").build();

    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente), Optional.of(updatedVente));
    when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(1L))).thenReturn(1L);
    when(venteRepository.update(any(Vente.class))).thenReturn(1);
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(updatedVente, "Pharmacie Test")).thenReturn(updatedResponse);

    VenteResponse response = venteCommandService.update(1L, updateRequest, currentUserId);

    assertNotNull(response);
    assertEquals("Updated", response.getRaisonsortie());
    verify(venteRepository).update(any(Vente.class));
  }

  @Test
  @DisplayName("Mise à jour échoue si vente n'existe pas")
  void testUpdateVenteNotFound() {
    when(venteRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> {
      venteCommandService.update(1L, venteRequest, currentUserId);
    });

    verify(venteRepository, never()).update(any(Vente.class));
  }

  @Test
  @DisplayName("Mise à jour échoue si vente est déjà validée")
  void testUpdateVenteAlreadyValidated() {
    Vente validatedVente = Vente.builder().id(1L).statut(Vente.StatutVente.SORTIE_USAGE).build();

    when(venteRepository.findById(1L)).thenReturn(Optional.of(validatedVente));

    assertThrows(BusinessException.class, () -> {
      venteCommandService.update(1L, venteRequest, currentUserId);
    });

    verify(venteRepository, never()).update(any(Vente.class));
  }

  @Test
  @DisplayName("Validation d'une vente réussie")
  void testValiderVenteSuccess() {
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(venteRepository.update(any(Vente.class))).thenReturn(1);

    venteCommandService.valider(1L, currentUserId, "SORTIE-USAGE");

    verify(venteRepository).findById(1L);
    verify(venteRepository).update(any(Vente.class));
  }

  @Test
  @DisplayName("Validation échoue si vente n'existe pas")
  void testValiderVenteNotFound() {
    when(venteRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> {
      venteCommandService.valider(1L, currentUserId, "SORTIE-USAGE");
    });

    verify(venteRepository, never()).update(any(Vente.class));
  }

  @Test
  @DisplayName("Annulation d'une vente réussie")
  void testAnnulerVenteSuccess() {
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(venteRepository.update(any(Vente.class))).thenReturn(1);

    venteCommandService.annuler(1L, currentUserId);

    verify(venteRepository).findById(1L);
    verify(venteRepository).update(any(Vente.class));
  }

  @Test
  @DisplayName("Annulation avec remboursement réussie")
  void testAnnulerVenteAvecRemboursementSuccess() {
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
    when(venteRepository.update(any(Vente.class))).thenReturn(1);

    venteCommandService.annulerAvecRemboursement(1L, currentUserId);

    verify(venteRepository).findById(1L);
    verify(venteRepository).update(any(Vente.class));
  }
}
