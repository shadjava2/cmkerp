package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateRequisitionStatusRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.RequisitionResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Service pour la gestion des requisitions (écriture uniquement).
 */
@Service
@Transactional
@Slf4j
public class RequisitionCommandService {

  private final JdbcTemplate jdbcTemplate;
  private final RequisitionQueryService requisitionQueryService;

  public RequisitionCommandService(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
      RequisitionQueryService requisitionQueryService) {
    this.jdbcTemplate = jdbcTemplate;
    this.requisitionQueryService = requisitionQueryService;
  }

  /**
   * Met à jour le statut d'une requisition.
   *
   * @param id ID de la requisition
   * @param request Requête contenant le nouveau statut et optionnellement un commentaire
   * @param currentUserId ID de l'utilisateur actuel
   * @return RequisitionResponse La requisition mise à jour
   */
  public RequisitionResponse updateStatus(Long id, UpdateRequisitionStatusRequest request,
      Long currentUserId) {
    log.debug("Mise à jour du statut de la requisition ID: {} -> {}", id, request.getStatut());

    // Vérifier que la requisition existe
    String checkSql = "SELECT COUNT(*) FROM requisitions WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(checkSql, Long.class, id);
    if (count == null || count == 0) {
      throw cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException.entity("Requisition",
          id);
    }

    // Vérifier que le statut est valide
    // On vérifie juste que le statut demandé est dans la liste valide
    if (!request.getStatut().matches("EN ATTENTE|VALIDEE|REJETEE|TRANSFEREE|RECEPTIONNEE")) {
      throw new BusinessException("Statut invalide: " + request.getStatut());
    }

    // Construire la requête SQL de mise à jour
    StringBuilder updateSql =
        new StringBuilder("UPDATE requisitions SET statut = ?, dateupdate = ?, userupdateid = ?");
    java.util.List<Object> params = new java.util.ArrayList<>();
    params.add(request.getStatut());
    params.add(Timestamp.valueOf(LocalDateTime.now()));
    params.add(currentUserId);

    // Si un commentaire est fourni, le mettre à jour
    if (request.getCommentaire() != null && !request.getCommentaire().trim().isEmpty()) {
      updateSql.append(", commentaire = ?");
      params.add(request.getCommentaire().trim());
    }

    updateSql.append(" WHERE id = ?");
    params.add(id);

    int rows = jdbcTemplate.update(updateSql.toString(), params.toArray());
    if (rows == 0) {
      throw new BusinessException("Échec de la mise à jour du statut de la requisition");
    }

    log.info("Statut de la requisition mis à jour avec succès: ID={}, statut={}", id,
        request.getStatut());

    // Récupérer la requisition mise à jour
    return requisitionQueryService.findById(id);
  }
}

