package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.LigneVenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.LigneVenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.LigneVente;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour convertir entre LigneVente (entité) et DTOs.
 */
@Component
public class LigneVenteMapper {

    /**
     * Convertit un LigneVenteRequest en entité LigneVente (pour création).
     */
    public LigneVente toEntity(LigneVenteRequest dto) {
        if (dto == null) {
            return null;
        }

        return LigneVente.builder()
                .fkVente(dto.getFkVente())
                .fkStock(dto.getFkStock())
                .qt(dto.getQt())
                .prixventes(dto.getPrixventes())
                .horsconvention(dto.getHorsconvention() != null ? dto.getHorsconvention() : 0)
                .dateCreate(LocalDateTime.now())
                .build();
    }

    /**
     * Met à jour une entité LigneVente existante à partir d'un LigneVenteRequest.
     */
    public void updateEntityFromRequest(LigneVenteRequest dto, LigneVente entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getFkStock() != null) {
            entity.setFkStock(dto.getFkStock());
        }
        if (dto.getQt() != null) {
            entity.setQt(dto.getQt());
        }
        if (dto.getPrixventes() != null) {
            entity.setPrixventes(dto.getPrixventes());
        }
        entity.setHorsconvention(dto.getHorsconvention() != null ? dto.getHorsconvention() : 0);
        entity.setDateUpdate(LocalDateTime.now());
    }

    /**
     * Convertit une entité LigneVente en Response DTO.
     */
    public LigneVenteResponse toResponse(LigneVente entity, String produitNom, Float stockActuel) {
        if (entity == null) {
            return null;
        }

        return LigneVenteResponse.builder()
                .id(entity.getId())
                .fkVente(entity.getFkVente())
                .fkStock(entity.getFkStock())
                .produitNom(produitNom)
                .stockActuel(stockActuel)
                .qt(entity.getQt())
                .prixventes(entity.getPrixventes())
                .horsconvention(entity.getHorsconvention())
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .build();
    }
}

