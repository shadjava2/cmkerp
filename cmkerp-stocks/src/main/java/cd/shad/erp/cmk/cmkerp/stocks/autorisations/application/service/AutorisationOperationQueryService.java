package cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.response.AutorisationOperationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model.AutorisationOperation;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.infrastructure.persistence.AutorisationOperationJdbcRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorisationOperationQueryService {

  private final AutorisationOperationJdbcRepository repository;
  private final JdbcTemplate jdbcTemplate;

  public PageResponse<AutorisationOperationResponse> findAll(Pageable pageable, String statut) {
    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();
    List<AutorisationOperation> rows = repository.findAll(statut, limit, offset);
    long total = repository.count(statut);
    List<AutorisationOperationResponse> content = rows.stream().map(this::toResponse).toList();
    return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
  }

  public AutorisationOperationResponse findById(Long id) {
    AutorisationOperation auth = repository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("AutorisationOperation", id));
    return toResponse(auth);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public boolean hasApprovedAnnulation(String tableCible, Long enregistrementId) {
    try {
      return repository
          .findApproved(tableCible, enregistrementId, AutorisationOperation.TYPE_ANNULATION)
          .isPresent();
    } catch (Exception e) {
      return false;
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public boolean hasPendingAnnulation(String tableCible, Long enregistrementId) {
    try {
      return repository
          .findPending(tableCible, enregistrementId, AutorisationOperation.TYPE_ANNULATION)
          .isPresent();
    } catch (Exception e) {
      return false;
    }
  }

  private AutorisationOperationResponse toResponse(AutorisationOperation auth) {
    return AutorisationOperationResponse.builder()
        .id(auth.getId())
        .tableCible(auth.getTableCible())
        .enregistrementId(auth.getEnregistrementId())
        .typeOperation(auth.getTypeOperation())
        .statut(auth.getStatut().name())
        .motif(auth.getMotif())
        .dateCreate(auth.getDateCreate())
        .dateUpdate(auth.getDateUpdate())
        .userCreateId(auth.getUserCreateId())
        .userCreateNom(getUserNom(auth.getUserCreateId()))
        .userDecideId(auth.getUserDecideId())
        .userDecideNom(getUserNom(auth.getUserDecideId()))
        .dateDecision(auth.getDateDecision())
        .commentaireDecision(auth.getCommentaireDecision())
        .libelleCible(resolveLibelleCible(auth))
        .build();
  }

  private String resolveLibelleCible(AutorisationOperation auth) {
    if (AutorisationOperation.TABLE_APPROVISIONNEMENT.equals(auth.getTableCible())) {
      try {
        return jdbcTemplate.queryForObject(
            "SELECT numbonliv FROM approvsionnements WHERE id = ?",
            String.class, auth.getEnregistrementId());
      } catch (Exception e) {
        return "Approvisionnement #" + auth.getEnregistrementId();
      }
    }
    return auth.getTableCible() + " #" + auth.getEnregistrementId();
  }

  private String getUserNom(Long userId) {
    if (userId == null) {
      return null;
    }
    try {
      return jdbcTemplate.queryForObject(
          "SELECT CONCAT(COALESCE(nom,''), ' ', COALESCE(prenom,'')) FROM utilisateurs WHERE id = ?",
          String.class, userId);
    } catch (Exception e) {
      return null;
    }
  }
}
