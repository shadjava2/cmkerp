package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.LigneTransfertInterne;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour convertir entre LigneTransfertInterne (entité) et DTOs.
 */
@Component
public class LigneTransfertInterneMapper {

    /**
     * Convertit un CreateLigneTransfertInterneRequest en entité LigneTransfertInterne (pour création).
     */
    public LigneTransfertInterne toEntity(CreateLigneTransfertInterneRequest dto, Long fkTransfertInterne) {
        if (dto == null) {
            return null;
        }

        return LigneTransfertInterne.builder()
                .fkTransfertInterne(fkTransfertInterne)
                .fkStock(dto.getFkStock())
                .fkAlertePeremption(dto.getFkAlertePeremption())
                .quantite(dto.getQuantite())
                .dateCreate(LocalDateTime.now())
                .build();
    }

    /**
     * Met à jour une entité LigneTransfertInterne existante à partir d'un UpdateLigneTransfertInterneRequest.
     */
    public void updateEntityFromRequest(UpdateLigneTransfertInterneRequest dto, LigneTransfertInterne entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getFkStock() != null) {
            entity.setFkStock(dto.getFkStock());
        }
        if (dto.getFkAlertePeremption() != null) {
            entity.setFkAlertePeremption(dto.getFkAlertePeremption());
        }
        if (dto.getQuantite() != null) {
            entity.setQuantite(dto.getQuantite());
        }
        entity.setDateUpdate(LocalDateTime.now());
    }

    /**
     * Convertit une entité LigneTransfertInterne en Response DTO.
     */
    public LigneTransfertInterneResponse toResponse(LigneTransfertInterne entity, String produitNom) {
        if (entity == null) {
            return null;
        }

        return LigneTransfertInterneResponse.builder()
                .id(entity.getId())
                .fkTransfertInterne(entity.getFkTransfertInterne())
                .fkStock(entity.getFkStock())
                .fkAlertePeremption(entity.getFkAlertePeremption())
                .produitNom(produitNom)
                .quantite(entity.getQuantite())
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .build();
    }
}

