package cd.shad.erp.cmk.cmkerp.pos.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.CreateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.UpdateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.LigneTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.mapper.LigneTransfertInterneMapper;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository.LigneTransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.pos.transferts.domain.repository.TransfertInterneRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Command Service pour la gestion des lignes de transfert interne (écriture uniquement) - module POS.
 */
@Service("posLigneTransfertInterneCommandService")
@Transactional
@Slf4j
public class LigneTransfertInterneCommandService {

    private final LigneTransfertInterneRepository ligneTransfertInterneRepository;
    private final TransfertInterneRepository transfertInterneRepository;
    private final LigneTransfertInterneMapper ligneTransfertInterneMapper;
    private final TransfertInterneStockValidator stockValidator;
    private final TransfertInterneProduitLookup produitLookup;

    public LigneTransfertInterneCommandService(
            @Qualifier("posLigneTransfertInterneJdbcRepositoryImpl") LigneTransfertInterneRepository ligneTransfertInterneRepository,
            @Qualifier("posTransfertInterneJdbcRepositoryImpl") TransfertInterneRepository transfertInterneRepository,
            @Qualifier("posLigneTransfertInterneMapper") LigneTransfertInterneMapper ligneTransfertInterneMapper,
            TransfertInterneStockValidator stockValidator,
            TransfertInterneProduitLookup produitLookup) {
        this.ligneTransfertInterneRepository = ligneTransfertInterneRepository;
        this.transfertInterneRepository = transfertInterneRepository;
        this.ligneTransfertInterneMapper = ligneTransfertInterneMapper;
        this.stockValidator = stockValidator;
        this.produitLookup = produitLookup;
    }

    /**
     * Crée une nouvelle ligne de transfert interne.
     */
    public LigneTransfertInterneResponse create(Long transfertInterneId, CreateLigneTransfertInterneRequest request, Long currentUserId) {
        log.debug("Création d'une nouvelle ligne de transfert interne pour transfert: {}", transfertInterneId);

        // Vérifier que le transfert interne existe et est modifiable
        TransfertInterne transfert = transfertInterneRepository.findById(transfertInterneId)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", transfertInterneId));

        if (transfert.getStatut() != TransfertInterne.StatutTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible d'ajouter une ligne à un transfert interne validé ou annulé");
        }

        // Vérifier que le stock existe
        if (request.getFkStock() != null) {
            stockValidator.verifyStockExists(request.getFkStock());
            stockValidator.verifyStockNotExpired(request.getFkStock());
            stockValidator.verifyStockAvailable(
                    request.getFkStock(), request.getQuantite(), transfert.getFkPharmacieSource(), transfertInterneId, null);
        }

        // Créer la ligne
        LigneTransfertInterne ligne = ligneTransfertInterneMapper.toEntity(request, transfertInterneId);
        ligne.setUserCreatedId(currentUserId);
        ligne.setDateCreate(LocalDateTime.now());

        int rows = ligneTransfertInterneRepository.save(ligne);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de la ligne de transfert interne");
        }

        log.info("Ligne de transfert interne créée avec succès: ID: {}", ligne.getId());

        // Récupérer la ligne créée
        LigneTransfertInterne created = ligneTransfertInterneRepository.findById(ligne.getId())
                .orElseThrow(() -> new BusinessException("Ligne créée mais introuvable"));

        String produitNom = getProduitNom(created.getFkStock());
        return ligneTransfertInterneMapper.toResponse(created, produitNom);
    }

    /**
     * Met à jour une ligne de transfert interne existante.
     */
    public LigneTransfertInterneResponse update(Long transfertInterneId, Long ligneId, UpdateLigneTransfertInterneRequest request, Long currentUserId) {
        log.debug("Mise à jour de la ligne de transfert interne ID: {}", ligneId);

        // Vérifier que le transfert interne existe et est modifiable
        TransfertInterne transfert = transfertInterneRepository.findById(transfertInterneId)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", transfertInterneId));

        if (transfert.getStatut() != TransfertInterne.StatutTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible de modifier une ligne d'un transfert interne validé ou annulé");
        }

        LigneTransfertInterne ligne = ligneTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> NotFoundException.entity("LigneTransfertInterne", ligneId));

        // Utiliser le stock de la ligne existante si non fourni dans la requête
        Long stockId = request.getFkStock() != null ? request.getFkStock() : ligne.getFkStock();
        Float quantityToCheck = request.getQuantite() != null ? request.getQuantite() : ligne.getQuantite();

        // Vérifier que le stock existe si fourni
        if (stockId != null) {
            stockValidator.verifyStockExists(stockId);
            stockValidator.verifyStockNotExpired(stockId);
            stockValidator.verifyStockAvailable(
                    stockId, quantityToCheck, transfert.getFkPharmacieSource(), transfertInterneId, ligneId);
        }

        // Mettre à jour la ligne
        ligneTransfertInterneMapper.updateEntityFromRequest(request, ligne);
        ligne.setUserUpdatedId(currentUserId);
        ligne.setDateUpdate(LocalDateTime.now());

        int rows = ligneTransfertInterneRepository.update(ligne);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour de la ligne de transfert interne");
        }

        log.info("Ligne de transfert interne mise à jour avec succès: ID: {}", ligneId);

        // Récupérer la ligne mise à jour
        LigneTransfertInterne updated = ligneTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> new BusinessException("Ligne mise à jour mais introuvable"));

        String produitNom = getProduitNom(updated.getFkStock());
        return ligneTransfertInterneMapper.toResponse(updated, produitNom);
    }

    /**
     * Supprime une ligne de transfert interne.
     */
    public void delete(Long transfertInterneId, Long ligneId) {
        log.debug("Suppression de la ligne de transfert interne ID: {}", ligneId);

        // Vérifier que le transfert interne existe et est modifiable
        TransfertInterne transfert = transfertInterneRepository.findById(transfertInterneId)
                .orElseThrow(() -> NotFoundException.entity("TransfertInterne", transfertInterneId));

        if (transfert.getStatut() != TransfertInterne.StatutTransfertInterne.EN_ATTENTE) {
            throw new BusinessException("Impossible de supprimer une ligne d'un transfert interne validé ou annulé");
        }

        // Vérifier que la ligne existe
        ligneTransfertInterneRepository.findById(ligneId)
                .orElseThrow(() -> NotFoundException.entity("LigneTransfertInterne", ligneId));

        int rows = ligneTransfertInterneRepository.delete(ligneId);
        if (rows == 0) {
            throw new BusinessException("Échec de la suppression de la ligne de transfert interne");
        }

        log.info("Ligne de transfert interne supprimée avec succès: ID: {}", ligneId);
    }

    private String getProduitNom(Long fkStock) {
        if (fkStock == null) {
            return null;
        }
        return produitLookup.resolveNomsByStockIds(List.of(fkStock)).get(fkStock);
    }
}

