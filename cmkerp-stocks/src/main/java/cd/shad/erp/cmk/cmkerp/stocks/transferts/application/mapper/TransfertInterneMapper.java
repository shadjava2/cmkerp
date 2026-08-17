package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.TransfertInterne;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour convertir entre TransfertInterne (entité) et DTOs.
 */
@Component
public class TransfertInterneMapper {

    /**
     * Convertit un CreateTransfertInterneRequest en entité TransfertInterne (pour création).
     */
    public TransfertInterne toEntity(CreateTransfertInterneRequest dto) {
        if (dto == null) {
            return null;
        }

        return TransfertInterne.builder()
                .fkPharmacieSource(dto.getFkPharmacieSource())
                .fkPharmacieDestination(dto.getFkPharmacieDestination())
                .statut(TransfertInterne.StatutTransfertInterne.EN_ATTENTE) // Statut initial
                .commentaire(dto.getCommentaire())
                .dateCreate(LocalDateTime.now())
                .build();
    }

    /**
     * Met à jour une entité TransfertInterne existante à partir d'un CreateTransfertInterneRequest.
     */
    public void updateEntityFromRequest(CreateTransfertInterneRequest dto, TransfertInterne entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getFkPharmacieSource() != null) {
            entity.setFkPharmacieSource(dto.getFkPharmacieSource());
        }
        if (dto.getFkPharmacieDestination() != null) {
            entity.setFkPharmacieDestination(dto.getFkPharmacieDestination());
        }
        entity.setCommentaire(dto.getCommentaire());
        entity.setDateUpdate(LocalDateTime.now());
    }

    /**
     * Convertit une entité TransfertInterne en Response DTO.
     */
    public TransfertInterneResponse toResponse(TransfertInterne entity, String pharmacieSourceNom, String pharmacieDestinationNom) {
        if (entity == null) {
            return null;
        }

        return TransfertInterneResponse.builder()
                .id(entity.getId())
                .fkPharmacieSource(entity.getFkPharmacieSource())
                .pharmacieSourceNom(pharmacieSourceNom)
                .fkPharmacieDestination(entity.getFkPharmacieDestination())
                .pharmacieDestinationNom(pharmacieDestinationNom)
                .statut(entity.getStatut() != null ? entity.getStatut().getDbValue() : "EN ATTENTE")
                .commentaire(entity.getCommentaire())
                .perime(entity.getPerime())
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .peutEtreAnnule(entity.peutEtreAnnule())
                .build();
    }
}

