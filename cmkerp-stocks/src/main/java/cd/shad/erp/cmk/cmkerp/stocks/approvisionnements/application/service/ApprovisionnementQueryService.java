package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.ApprovisionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.mapper.ApprovisionnementMapper;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.Approvisionnement;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.repository.ApprovisionnementRepository;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.service.AutorisationOperationQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model.AutorisationOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Service pour la gestion des approvisionnements (lecture uniquement).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApprovisionnementQueryService {

  private final ApprovisionnementRepository approvisionnementRepository;
  private final ApprovisionnementMapper approvisionnementMapper;
  private final AutorisationOperationQueryService autorisationOperationQueryService;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Récupère une page d'approvisionnements avec filtres.
   */
  public PageResponse<ApprovisionnementResponse> findAll(Pageable pageable, Long fkPharmacie,
      String statut, Long fkFournisseur, LocalDate dateFrom, LocalDate dateTo, String searchText,
      String produitQ, Long produitId) {
    int offset = (int) pageable.getOffset();
    int limit = pageable.getPageSize();

    List<Approvisionnement> approvisionnements = approvisionnementRepository.findAll(offset, limit,
        fkPharmacie, statut, fkFournisseur, dateFrom, dateTo, searchText, produitQ, produitId);
    long totalElements = approvisionnementRepository.count(fkPharmacie, statut, fkFournisseur,
        dateFrom, dateTo, searchText, produitQ, produitId);

    // Récupérer les désignations via JOINs
    List<ApprovisionnementResponse> responses = approvisionnements.stream().map(approv -> {
      String fournisseurNom = getFournisseurNom(approv.getFkFournisseur());
      String pharmacieNom = getPharmacieNom(approv.getFkPharmacie());
      String echangeDeviseMonnaie =
          approv.getFkEchangeDevise() != null ? getEchangeDeviseMonnaie(approv.getFkEchangeDevise())
              : null;
      ApprovisionnementResponse response = approvisionnementMapper.toResponse(approv, fournisseurNom, pharmacieNom,
          echangeDeviseMonnaie);
      enrichAutorisationFlags(approv, response);
      enrichUserNames(response);
      return response;
    }).collect(Collectors.toList());

    int pageSize = pageable.getPageSize();
    int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
    if (totalElements == 0) {
      totalPages = 0;
    }

    return PageResponse.<ApprovisionnementResponse>builder().content(responses)
        .page(pageable.getPageNumber()).size(pageSize).totalElements(totalElements)
        .totalPages(totalPages).hasNext(pageable.getPageNumber() < totalPages - 1)
        .hasPrevious(pageable.getPageNumber() > 0).build();
  }

  /**
   * Récupère un approvisionnement par son ID.
   */
  public ApprovisionnementResponse findById(Long id) {
    Approvisionnement approvisionnement = approvisionnementRepository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("Approvisionnement", id));

    String fournisseurNom = getFournisseurNom(approvisionnement.getFkFournisseur());
    String pharmacieNom = getPharmacieNom(approvisionnement.getFkPharmacie());
    String echangeDeviseMonnaie = approvisionnement.getFkEchangeDevise() != null
        ? getEchangeDeviseMonnaie(approvisionnement.getFkEchangeDevise())
        : null;

    ApprovisionnementResponse response = approvisionnementMapper.toResponse(approvisionnement, fournisseurNom, pharmacieNom,
        echangeDeviseMonnaie);
    enrichAutorisationFlags(approvisionnement, response);
    enrichUserNames(response);
    return response;
  }

  private void enrichAutorisationFlags(Approvisionnement entity, ApprovisionnementResponse response) {
    response.setNecessiteAutorisationAnnulation(entity.necessiteAutorisationAnnulation());
    response.setDemandeAnnulationEnCours(
        autorisationOperationQueryService.hasPendingAnnulation(
            AutorisationOperation.TABLE_APPROVISIONNEMENT, entity.getId()));
  }

  private void enrichUserNames(ApprovisionnementResponse response) {
    response.setUserCreateNom(getUserDisplayName(response.getUserCreatedId()));
    response.setUserUpdateNom(getUserDisplayName(response.getUserUpdatedId()));
  }

  private String getUserDisplayName(Long userId) {
    if (userId == null) {
      return null;
    }
    try {
      return jdbcTemplate.query(
          "SELECT prenom, nom, username FROM utilisateurs WHERE id = ?",
          rs -> {
            if (!rs.next()) {
              return null;
            }
            String prenom = rs.getString("prenom");
            String nom = rs.getString("nom");
            String username = rs.getString("username");
            String full = String.join(" ",
                prenom != null ? prenom.trim() : "",
                nom != null ? nom.trim() : "").trim();
            if (!full.isEmpty()) {
              return full;
            }
            return username != null && !username.isBlank() ? username.trim() : null;
          },
          userId);
    } catch (Exception e) {
      log.warn("Utilisateur non trouvé pour ID: {}", userId);
      return null;
    }
  }

  private String getFournisseurNom(Long fkFournisseur) {
    if (fkFournisseur == null) {
      return null;
    }
    String sql = "SELECT nom FROM fournisseurs WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, String.class, fkFournisseur);
    } catch (Exception e) {
      log.warn("Fournisseur non trouvé pour ID: {}", fkFournisseur);
      return null;
    }
  }

  private String getPharmacieNom(Long fkPharmacie) {
    if (fkPharmacie == null) {
      return null;
    }
    String sql = "SELECT designation FROM pharmacies WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, String.class, fkPharmacie);
    } catch (Exception e) {
      log.warn("Pharmacie non trouvée pour ID: {}", fkPharmacie);
      return null;
    }
  }

  private String getEchangeDeviseMonnaie(Long fkEchangeDevise) {
    if (fkEchangeDevise == null) {
      return null;
    }
    String sql = "SELECT monnaieechange FROM echange_devise WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, String.class, fkEchangeDevise);
    } catch (Exception e) {
      log.warn("EchangeDevise non trouvé pour ID: {}", fkEchangeDevise);
      return null;
    }
  }
}

