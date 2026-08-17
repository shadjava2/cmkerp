package cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.ApprovisionnementCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.request.DecisionAutorisationRequest;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.response.AutorisationOperationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model.AutorisationOperation;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model.AutorisationOperation.StatutAutorisation;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.infrastructure.persistence.AutorisationOperationJdbcRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class AutorisationOperationCommandService {

  private final AutorisationOperationJdbcRepository repository;
  private final AutorisationOperationQueryService queryService;
  private final JdbcTemplate jdbcTemplate;
  private final ApprovisionnementCommandService approvisionnementCommandService;

  public AutorisationOperationCommandService(
      AutorisationOperationJdbcRepository repository,
      AutorisationOperationQueryService queryService,
      JdbcTemplate jdbcTemplate,
      @Lazy ApprovisionnementCommandService approvisionnementCommandService) {
    this.repository = repository;
    this.queryService = queryService;
    this.jdbcTemplate = jdbcTemplate;
    this.approvisionnementCommandService = approvisionnementCommandService;
  }

  public AutorisationOperationResponse creerDemande(String tableCible, Long enregistrementId,
      String typeOperation, String motif, Long userId) {
    if (repository.findPending(tableCible, enregistrementId, typeOperation).isPresent()) {
      throw new BusinessException("Une demande d'autorisation est déjà en attente pour cette opération");
    }

    AutorisationOperation auth = AutorisationOperation.builder()
        .tableCible(tableCible)
        .enregistrementId(enregistrementId)
        .typeOperation(typeOperation)
        .statut(StatutAutorisation.EN_ATTENTE)
        .motif(motif)
        .dateCreate(LocalDateTime.now())
        .userCreateId(userId)
        .build();

    Long id = repository.insert(auth);
    if (id == null) {
      throw new BusinessException("Échec de la création de la demande d'autorisation");
    }
    log.info("Demande d'autorisation créée: id={}, table={}, enregistrement={}", id, tableCible,
        enregistrementId);
    return queryService.findById(id);
  }

  public AutorisationOperationResponse approuver(Long id, DecisionAutorisationRequest request,
      Long adminUserId) {
    AutorisationOperation auth = repository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("AutorisationOperation", id));

    if (auth.getStatut() != StatutAutorisation.EN_ATTENTE) {
      throw new BusinessException("Cette demande a déjà été traitée");
    }

    LocalDateTime now = LocalDateTime.now();
    auth.setStatut(StatutAutorisation.APPROUVEE);
    auth.setUserDecideId(adminUserId);
    auth.setDateDecision(now);
    auth.setDateUpdate(now);
    auth.setCommentaireDecision(request != null ? request.getCommentaire() : null);
    repository.updateDecision(auth);

    executerOperationApprouvee(auth, adminUserId);
    return queryService.findById(id);
  }

  public AutorisationOperationResponse rejeter(Long id, DecisionAutorisationRequest request,
      Long adminUserId) {
    AutorisationOperation auth = repository.findById(id)
        .orElseThrow(() -> NotFoundException.entity("AutorisationOperation", id));

    if (auth.getStatut() != StatutAutorisation.EN_ATTENTE) {
      throw new BusinessException("Cette demande a déjà été traitée");
    }

    LocalDateTime now = LocalDateTime.now();
    auth.setStatut(StatutAutorisation.REJETEE);
    auth.setUserDecideId(adminUserId);
    auth.setDateDecision(now);
    auth.setDateUpdate(now);
    auth.setCommentaireDecision(request != null ? request.getCommentaire() : null);
    repository.updateDecision(auth);

    log.info("Demande d'autorisation rejetée: id={}", id);
    return queryService.findById(id);
  }

  public boolean hasApprovedAnnulation(String tableCible, Long enregistrementId) {
    return repository
        .findApproved(tableCible, enregistrementId, AutorisationOperation.TYPE_ANNULATION)
        .isPresent();
  }

  public boolean hasPendingAnnulation(String tableCible, Long enregistrementId) {
    return repository
        .findPending(tableCible, enregistrementId, AutorisationOperation.TYPE_ANNULATION)
        .isPresent();
  }

  private void executerOperationApprouvee(AutorisationOperation auth, Long adminUserId) {
    if (AutorisationOperation.TABLE_APPROVISIONNEMENT.equals(auth.getTableCible())
        && AutorisationOperation.TYPE_ANNULATION.equals(auth.getTypeOperation())) {
      approvisionnementCommandService.annulerAvecAutorisation(auth.getEnregistrementId(), adminUserId);
    }
  }
}
