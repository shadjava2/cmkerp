package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper.VenteMapper;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.Vente;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.repository.VenteRepository;

/**
 * Tests unitaires pour VenteQueryService.
 *
 * <p>
 * Tests de la logique de lecture des ventes sans dépendances externes (DB, réseau, etc.).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - VenteQueryService")
class VenteQueryServiceTest {

  @Mock
  private VenteRepository venteRepository;

  @Mock
  private VenteMapper venteMapper;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private VenteQueryService venteQueryService;

  private Pageable pageable;
  private Vente vente1;
  private Vente vente2;
  private VenteResponse venteResponse1;
  private VenteResponse venteResponse2;

  @BeforeEach
  void setUp() {
    pageable = PageRequest.of(0, 20);

    vente1 = Vente.builder().id(1L).fkPharmacie(1L).raisonsortie("Test 1")
        .statut(Vente.StatutVente.EN_ATTENTE).dateCreate(LocalDateTime.now()).build();

    vente2 = Vente.builder().id(2L).fkPharmacie(1L).raisonsortie("Test 2")
        .statut(Vente.StatutVente.SORTIE_USAGE).dateCreate(LocalDateTime.now()).build();

    venteResponse1 = VenteResponse.builder().id(1L).fkPharmacie(1L).raisonsortie("Test 1")
        .statut("EN ATTENTE").pharmacieNom("Pharmacie Test").build();

    venteResponse2 = VenteResponse.builder().id(2L).fkPharmacie(1L).raisonsortie("Test 2")
        .statut("SORTIE-USAGE").pharmacieNom("Pharmacie Test").build();
  }

  @Test
  @DisplayName("Récupération d'une page de ventes réussie")
  void testFindAllSuccess() {
    // Given
    List<Vente> ventes = Arrays.asList(vente1, vente2);
    long totalElements = 2L;

    when(venteRepository.findAll(0, 20, null, null, null, null, null, null)).thenReturn(ventes);
    when(venteRepository.count(null, null, null, null, null, null)).thenReturn(totalElements);
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(vente1, "Pharmacie Test")).thenReturn(venteResponse1);
    when(venteMapper.toResponse(vente2, "Pharmacie Test")).thenReturn(venteResponse2);

    // When
    PageResponse<VenteResponse> response =
        venteQueryService.findAll(pageable, null, null, null, null, null, null);

    // Then
    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertEquals(2L, response.getTotalElements());
    assertEquals(0, response.getPage());
    assertEquals(20, response.getSize());
    assertEquals(1, response.getTotalPages());
    assertEquals(false, response.isHasNext());
    assertEquals(false, response.isHasPrevious());
  }

  @Test
  @DisplayName("Récupération d'une page de ventes avec filtres")
  void testFindAllWithFilters() {
    // Given
    Long fkPharmacie = 1L;
    String statut = "EN ATTENTE";
    LocalDate dateFrom = LocalDate.of(2024, 1, 1);
    LocalDate dateTo = LocalDate.of(2024, 12, 31);
    String searchText = "Test";

    List<Vente> ventes = Collections.singletonList(vente1);
    long totalElements = 1L;

    when(venteRepository.findAll(0, 20, fkPharmacie, statut, null, dateFrom, dateTo, searchText))
        .thenReturn(ventes);
    when(venteRepository.count(fkPharmacie, statut, null, dateFrom, dateTo, searchText))
        .thenReturn(totalElements);
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(vente1, "Pharmacie Test")).thenReturn(venteResponse1);

    // When
    PageResponse<VenteResponse> response = venteQueryService.findAll(pageable, fkPharmacie, statut,
        null, dateFrom, dateTo, searchText);

    // Then
    assertNotNull(response);
    assertEquals(1, response.getContent().size());
    assertEquals(1L, response.getTotalElements());
    verify(venteRepository).findAll(0, 20, fkPharmacie, statut, null, dateFrom, dateTo, searchText);
  }

  @Test
  @DisplayName("Récupération d'une page vide")
  void testFindAllEmpty() {
    // Given
    when(venteRepository.findAll(0, 20, null, null, null, null, null, null))
        .thenReturn(Collections.emptyList());
    when(venteRepository.count(null, null, null, null, null, null)).thenReturn(0L);

    // When
    PageResponse<VenteResponse> response =
        venteQueryService.findAll(pageable, null, null, null, null, null, null);

    // Then
    assertNotNull(response);
    assertEquals(0, response.getContent().size());
    assertEquals(0L, response.getTotalElements());
    assertEquals(0, response.getTotalPages());
  }

  @Test
  @DisplayName("Récupération d'une vente par ID réussie")
  void testFindByIdSuccess() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente1));
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(vente1, "Pharmacie Test")).thenReturn(venteResponse1);

    // When
    VenteResponse response = venteQueryService.findById(1L);

    // Then
    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("Test 1", response.getRaisonsortie());
    assertEquals("Pharmacie Test", response.getPharmacieNom());
    verify(venteRepository).findById(1L);
  }

  @Test
  @DisplayName("Récupération échoue si vente n'existe pas")
  void testFindByIdNotFound() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.empty());

    // When / Then
    assertThrows(NotFoundException.class, () -> {
      venteQueryService.findById(1L);
    });

    verify(venteRepository).findById(1L);
  }

  @Test
  @DisplayName("Récupération avec pharmacie nom null si pharmacie n'existe pas")
  void testFindByIdPharmacieNotFound() {
    // Given
    when(venteRepository.findById(1L)).thenReturn(Optional.of(vente1));
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
        .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

    VenteResponse responseWithoutPharmacie = VenteResponse.builder().id(1L).fkPharmacie(1L)
        .raisonsortie("Test 1").statut("EN ATTENTE").pharmacieNom(null).build();

    when(venteMapper.toResponse(vente1, null)).thenReturn(responseWithoutPharmacie);

    // When
    VenteResponse response = venteQueryService.findById(1L);

    // Then
    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals(null, response.getPharmacieNom());
  }

  @Test
  @DisplayName("Calcul correct de hasNext et hasPrevious")
  void testPaginationCalculations() {
    // Given
    Pageable pageablePage1 = PageRequest.of(1, 10);
    List<Vente> ventes = Arrays.asList(vente1, vente2);
    long totalElements = 25L; // 3 pages au total (10, 10, 5)

    when(venteRepository.findAll(10, 10, null, null, null, null, null, null)).thenReturn(ventes);
    when(venteRepository.count(null, null, null, null, null, null)).thenReturn(totalElements);
    when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), any(Long.class)))
        .thenReturn("Pharmacie Test");
    when(venteMapper.toResponse(any(Vente.class), eq("Pharmacie Test"))).thenReturn(venteResponse1,
        venteResponse2);

    // When
    PageResponse<VenteResponse> response =
        venteQueryService.findAll(pageablePage1, null, null, null, null, null, null);

    // Then
    assertNotNull(response);
    assertEquals(1, response.getPage());
    assertEquals(10, response.getSize());
    assertEquals(25L, response.getTotalElements());
    assertEquals(3, response.getTotalPages());
    assertEquals(true, response.isHasNext()); // Page 1 sur 3, il y a une page suivante
    assertEquals(true, response.isHasPrevious()); // Page 1, il y a une page précédente
  }
}
