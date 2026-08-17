package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.ReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts.ReceptionTransfertInterne;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour convertir entre ReceptionTransfertInterne (entité) et DTOs.
 */
@Component
public class ReceptionTransfertInterneMapper {

    /**
     * Convertit un CreateReceptionTransfertInterneRequest en entité ReceptionTransfertInterne (pour création).
     */
    public ReceptionTransfertInterne toEntity(CreateReceptionTransfertInterneRequest dto) {
        if (dto == null) {
            return null;
        }

        return ReceptionTransfertInterne.builder()
                .fkTransfertInterne(dto.getFkTransfertInterne())
                .statut(ReceptionTransfertInterne.StatutReceptionTransfertInterne.EN_ATTENTE) // Statut initial
                .dateCreate(LocalDateTime.now())
                .build();
    }

    /**
     * Convertit une entité ReceptionTransfertInterne en Response DTO.
     */
    public ReceptionTransfertInterneResponse toResponse(
            ReceptionTransfertInterne entity,
            String transfertInterneNumero,
            Long fkPharmacieSource,
            String pharmacieSourceNom,
            Long fkPharmacieDestination,
            String pharmacieDestinationNom) {
        if (entity == null) {
            return null;
        }

        // Calculer statutTransfert : "transfert périmé" si perime=true, sinon "transfert stock"
        String statutTransfert = (entity.getPerime() != null && entity.getPerime()) ? "transfert périmé" : "transfert stock";

        return ReceptionTransfertInterneResponse.builder()
                .id(entity.getId())
                .fkTransfertInterne(entity.getFkTransfertInterne())
                .transfertInterneNumero(transfertInterneNumero)
                .fkPharmacieSource(fkPharmacieSource)
                .pharmacieSourceNom(pharmacieSourceNom)
                .fkPharmacieDestination(fkPharmacieDestination)
                .pharmacieDestinationNom(pharmacieDestinationNom)
                .statut(entity.getStatut() != null ? entity.getStatut().getDbValue() : "EN ATTENTE")
                .perime(entity.getPerime())
                .statutTransfert(statutTransfert)
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .peutEtreAnnule(entity.peutEtreAnnule())
                .build();
    }
}

