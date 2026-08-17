package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.ApprovisionnementRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.ApprovisionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.Approvisionnement;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour convertir entre Approvisionnement (entité) et DTOs.
 */
@Component
public class ApprovisionnementMapper {

    /**
     * Convertit un ApprovisionnementRequest en entité Approvisionnement (pour création).
     */
    public Approvisionnement toEntity(ApprovisionnementRequest dto) {
        if (dto == null) {
            return null;
        }

        return Approvisionnement.builder()
                .fkFournisseur(dto.getFkFournisseur())
                .fkPharmacie(dto.getFkPharmacie())
                .fkEchangeDevise(dto.getFkEchangeDevise())
                .statut(Approvisionnement.StatutApprovisionnement.EN_ATTENTE) // Statut initial
                .numbonliv(dto.getNumbonliv())
                .taux(toShortTaux(dto.getTaux()))
                .datebonliv(dto.getDatebonliv())
                .dateCreate(LocalDateTime.now())
                .build();
    }

    /**
     * Met à jour une entité Approvisionnement existante à partir d'un ApprovisionnementRequest.
     */
    public void updateEntityFromRequest(ApprovisionnementRequest dto, Approvisionnement entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getFkFournisseur() != null) {
            entity.setFkFournisseur(dto.getFkFournisseur());
        }
        if (dto.getFkPharmacie() != null) {
            entity.setFkPharmacie(dto.getFkPharmacie());
        }
        if (dto.getFkEchangeDevise() != null) {
            entity.setFkEchangeDevise(dto.getFkEchangeDevise());
        }
        if (dto.getNumbonliv() != null) {
            entity.setNumbonliv(dto.getNumbonliv());
        }
        if (dto.getTaux() != null) {
            entity.setTaux(toShortTaux(dto.getTaux()));
        }
        if (dto.getDatebonliv() != null) {
            entity.setDatebonliv(dto.getDatebonliv());
        }
        entity.setDateUpdate(LocalDateTime.now());
    }

    private static Short toShortTaux(Integer taux) {
        if (taux == null) {
            return null;
        }
        return taux.shortValue();
    }

    /**
     * Convertit une entité Approvisionnement en Response DTO.
     */
    public ApprovisionnementResponse toResponse(Approvisionnement entity, String fournisseurNom, String pharmacieNom, String echangeDeviseMonnaie) {
        if (entity == null) {
            return null;
        }

        return ApprovisionnementResponse.builder()
                .id(entity.getId())
                .fkFournisseur(entity.getFkFournisseur())
                .fournisseurNom(fournisseurNom)
                .fkPharmacie(entity.getFkPharmacie())
                .pharmacieNom(pharmacieNom)
                .fkEchangeDevise(entity.getFkEchangeDevise())
                .echangeDeviseMonnaie(echangeDeviseMonnaie)
                .statut(entity.getStatut().name())
                .numbonliv(entity.getNumbonliv())
                .taux(entity.getTaux())
                .datebonliv(entity.getDatebonliv())
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .peutEtreAnnule(entity.peutEtreAnnule())
                .build();
    }
}

