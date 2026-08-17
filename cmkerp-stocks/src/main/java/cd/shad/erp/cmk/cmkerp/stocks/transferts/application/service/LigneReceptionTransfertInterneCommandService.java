package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateLigneReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper.LigneReceptionTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.ReceptionTransfertInterne;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.LigneReceptionTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.domain.repository.ReceptionTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Command Service pour la gestion des lignes de réception de transfert interne (écriture uniquement).
 */
@Service
@Transactional
@Slf4j
public class LigneReceptionTransfertInterneCommandService {

    private final LigneReceptionTransfertInterneRepository ligneReceptionTransfertInterneRepository;
    private final ReceptionTransfertInterneRepository receptionTransfertInterneRepository;
    private final LigneReceptionTransfertInterneMapper ligneReceptionTransfertInterneMapper;
    private final LigneReceptionTransfertInterneQueryService ligneReceptionTransfertInterneQueryService;
    private final JdbcTemplate jdbcTemplate;

    public LigneReceptionTransfertInterneCommandService(
            LigneReceptionTransfertInterneRepository ligneReceptionTransfertInterneRepository,
            ReceptionTransfertInterneRepository receptionTransfertInterneRepository,
            LigneReceptionTransfertInterneMapper ligneReceptionTransfertInterneMapper,
            LigneReceptionTransfertInterneQueryService ligneReceptionTransfertInterneQueryService,
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.ligneReceptionTransfertInterneRepository = ligneReceptionTransfertInterneRepository;
        this.receptionTransfertInterneRepository = receptionTransfertInterneRepository;
        this.ligneReceptionTransfertInterneMapper = ligneReceptionTransfertInterneMapper;
        this.ligneReceptionTransfertInterneQueryService = ligneReceptionTransfertInterneQueryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Met à jour une ligne de réception de transfert interne existante.
     */
    public LigneReceptionTransfertInterneResponse update(Long receptionId, Long ligneId, UpdateLigneReceptionTransfertInterneRequest request, Long currentUserId) {
        log.debug("Mise à jour de la ligne de réception - réceptionId: {}, ligneId: {}", receptionId, ligneId);

        // Vérifier que la réception existe et est modifiable
        ReceptionTransfertInterne reception = receptionTransfertInterneRepository.findById(receptionId)
                .orElseThrow(() -> NotFoundException.entity("ReceptionTransfertInterne", receptionId));

        if (reception.getStatut() != ReceptionTransfertInterne.StatutReceptionTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible de modifier une ligne d'une réception validée ou annulée");
        }

        LigneReceptionTransfertInterne ligne = ligneReceptionTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> NotFoundException.entity("LigneReceptionTransfertInterne", ligneId));

        // Vérifier que la ligne appartient à la réception
        if (!ligne.getFkReceptionTransfertInterne().equals(receptionId)) {
            throw new BusinessException("La ligne n'appartient pas à cette réception");
        }

        // Valider la quantité et vérifier que le produit n'est pas périmé
        if (request.getQuantite() != null && request.getQuantite() > 0) {
            if (request.getQuantite() < 0) {
                throw new BusinessException("La quantité ne peut pas être négative");
            }
            if (ligne.getQuantiteTransferee() != null && request.getQuantite() > ligne.getQuantiteTransferee()) {
                throw new BusinessException(
                        String.format("La quantité à réceptionner (%.2f) ne peut pas dépasser la quantité transférée (%.2f)",
                                request.getQuantite(), ligne.getQuantiteTransferee()));
            }

            // Vérifier que le produit n'est pas périmé
            String peremption = getPeremption(ligne.getFkStock());
            if (peremption != null && !peremption.trim().isEmpty()) {
                String[] dates = peremption.split(",");
                java.time.LocalDate today = java.time.LocalDate.now();
                for (String dateStr : dates) {
                    try {
                        java.time.LocalDate datePeremption = java.time.LocalDate.parse(dateStr.trim());
                        if (datePeremption.isBefore(today) || datePeremption.isEqual(today)) {
                            throw new BusinessException("Impossible de réceptionner un produit périmé. Date de péremption: " + dateStr.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Erreur lors de la vérification de la date de péremption: {}", dateStr, e);
                    }
                }
            }
        }

        // Mettre à jour la ligne
        ligneReceptionTransfertInterneMapper.updateEntityFromRequest(request, ligne);
        ligne.setUserUpdatedId(currentUserId);
        ligne.setDateUpdate(LocalDateTime.now());

        int rows = ligneReceptionTransfertInterneRepository.update(ligne);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de la ligne de réception");
        }

        log.info("Ligne de réception mise à jour avec succès: ID: {}", ligneId);

        // Récupérer la ligne mise à jour avec toutes les informations
        return ligneReceptionTransfertInterneQueryService.findById(ligneId);
    }

    /**
     * Supprime une ligne de réception de transfert interne.
     */
    public void delete(Long receptionId, Long ligneId) {
        log.debug("Suppression de la ligne de réception - réceptionId: {}, ligneId: {}", receptionId, ligneId);

        // Vérifier que la réception existe et est modifiable
        ReceptionTransfertInterne reception = receptionTransfertInterneRepository.findById(receptionId)
                .orElseThrow(() -> NotFoundException.entity("ReceptionTransfertInterne", receptionId));

        if (reception.getStatut() != ReceptionTransfertInterne.StatutReceptionTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible de supprimer une ligne d'une réception validée ou annulée");
        }

        // Vérifier que la ligne existe
        LigneReceptionTransfertInterne ligne = ligneReceptionTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> NotFoundException.entity("LigneReceptionTransfertInterne", ligneId));

        // Vérifier que la ligne appartient à la réception
        if (!ligne.getFkReceptionTransfertInterne().equals(receptionId)) {
            throw new BusinessException("La ligne n'appartient pas à cette réception");
        }

        int rows = ligneReceptionTransfertInterneRepository.delete(ligneId);
        if (rows == 0) {
            throw new BusinessException("Échec de la suppression de la ligne de réception");
        }

        log.info("Ligne de réception supprimée avec succès: ID: {}", ligneId);
    }

    private String getPeremption(Long fkStock) {
        if (fkStock == null) {
            return null;
        }
        String sql = """
            SELECT GROUP_CONCAT(dateperemtion ORDER BY dateperemtion) AS peremption
            FROM perimable_alerte_stock
            WHERE fkStock = ? AND notifactif = TRUE
            GROUP BY fkStock
            """;
        try {
            return jdbcTemplate.queryForObject(sql, String.class, fkStock);
        } catch (Exception e) {
            // Pas de péremption enregistrée
            return null;
        }
    }
}

