package cd.shad.erp.cmk.cmkerp.pos.transferts.application.mapper;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.CreateLigneReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.UpdateLigneReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.LigneReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneReceptionTransfertInterne;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour convertir entre LigneReceptionTransfertInterne (entité) et DTOs (module POS).
 */
@Component("posLigneReceptionTransfertInterneMapper")
public class LigneReceptionTransfertInterneMapper {

    /**
     * Convertit un CreateLigneReceptionTransfertInterneRequest en entité LigneReceptionTransfertInterne (pour création).
     */
    public LigneReceptionTransfertInterne toEntity(CreateLigneReceptionTransfertInterneRequest dto, Long fkReceptionTransfertInterne) {
        if (dto == null) {
            return null;
        }

        return LigneReceptionTransfertInterne.builder()
                .fkReceptionTransfertInterne(fkReceptionTransfertInterne)
                .fkStock(dto.getFkStock())
                .fkAlertePeremption(dto.getFkAlertePeremption())
                .quantiteDemandee(dto.getQuantiteDemandee())
                .quantiteTransferee(dto.getQuantiteTransferee())
                .quantite(dto.getQuantite())
                .dateCreate(LocalDateTime.now())
                .build();
    }

    /**
     * Met à jour une entité LigneReceptionTransfertInterne existante à partir d'un UpdateLigneReceptionTransfertInterneRequest.
     */
    public void updateEntityFromRequest(UpdateLigneReceptionTransfertInterneRequest dto, LigneReceptionTransfertInterne entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getQuantite() != null) {
            entity.setQuantite(dto.getQuantite());
        }
        entity.setDateUpdate(LocalDateTime.now());
    }

    /**
     * Convertit une entité LigneReceptionTransfertInterne en Response DTO avec toutes les informations produit.
     */
    public LigneReceptionTransfertInterneResponse toResponse(
            LigneReceptionTransfertInterne entity,
            String nomCommercial,
            String nomScientifique,
            String forme,
            String dosage,
            String conditionnement,
            String peremption,
            Float stockActuel) {
        if (entity == null) {
            return null;
        }

        return LigneReceptionTransfertInterneResponse.builder()
                .id(entity.getId())
                .fkReceptionTransfertInterne(entity.getFkReceptionTransfertInterne())
                .fkStock(entity.getFkStock())
                .fkAlertePeremption(entity.getFkAlertePeremption())
                .nomCommercial(nomCommercial)
                .nomScientifique(nomScientifique)
                .forme(forme)
                .dosage(dosage)
                .conditionnement(conditionnement)
                .peremption(peremption)
                .quantiteTransferee(entity.getQuantiteTransferee())
                .quantite(entity.getQuantite())
                .stockActuel(stockActuel)
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .build();
    }
}

