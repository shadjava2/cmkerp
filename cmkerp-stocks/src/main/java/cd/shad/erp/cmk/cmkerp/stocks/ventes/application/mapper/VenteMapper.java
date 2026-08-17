package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.VenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model.Vente;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour convertir entre Vente (entité) et DTOs.
 */
@Component
public class VenteMapper {

    /**
     * Convertit un VenteRequest en entité Vente (pour création).
     */
    public Vente toEntity(VenteRequest dto) {
        if (dto == null) {
            return null;
        }

        return Vente.builder()
                .fkEntreprise(dto.getFkEntreprise() != null ? dto.getFkEntreprise() : 0L)
                .fkPatient(dto.getFkPatient() != null ? dto.getFkPatient() : 0L)
                .fkPharmacie(dto.getFkPharmacie())
                .statut(Vente.StatutVente.EN_ATTENTE) // Statut initial
                .taux(dto.getTaux() != null ? dto.getTaux() : (short) 0)
                .typepaiement(dto.getTypepaiement() != null ? dto.getTypepaiement() : "-")
                .raisonsortie(dto.getRaisonsortie())
                .demandeur(dto.getDemandeur())
                .fkPatientMediline(dto.getFkPatientMediline())
                .fkFicheMedicale(dto.getFkFicheMedicale())
                .dateCreate(LocalDateTime.now())
                .dateUpdate(LocalDateTime.now())
                .build();
    }

    /**
     * Met à jour une entité Vente existante à partir d'un VenteRequest.
     */
    public void updateEntityFromRequest(VenteRequest dto, Vente entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getFkEntreprise() != null) {
            entity.setFkEntreprise(dto.getFkEntreprise());
        } else {
            entity.setFkEntreprise(0L);
        }
        if (dto.getFkPatient() != null) {
            entity.setFkPatient(dto.getFkPatient());
        } else {
            entity.setFkPatient(0L);
        }
        if (dto.getFkPharmacie() != null) {
            entity.setFkPharmacie(dto.getFkPharmacie());
        }
        if (dto.getTaux() != null) {
            entity.setTaux(dto.getTaux());
        } else {
            entity.setTaux((short) 0);
        }
        if (dto.getTypepaiement() != null) {
            entity.setTypepaiement(dto.getTypepaiement());
        } else {
            entity.setTypepaiement("-");
        }
        entity.setRaisonsortie(dto.getRaisonsortie());
        entity.setDemandeur(dto.getDemandeur());
        entity.setFkPatientMediline(dto.getFkPatientMediline());
        entity.setFkFicheMedicale(dto.getFkFicheMedicale());
        entity.setDateUpdate(LocalDateTime.now());
    }

    /**
     * Convertit une entité Vente en Response DTO.
     */
    public VenteResponse toResponse(Vente entity, String pharmacieNom) {
        if (entity == null) {
            return null;
        }

        return VenteResponse.builder()
                .id(entity.getId())
                .fkEntreprise(entity.getFkEntreprise())
                .fkPatient(entity.getFkPatient())
                .fkPharmacie(entity.getFkPharmacie())
                .pharmacieNom(pharmacieNom)
                .statut(entity.getStatut() != null ? entity.getStatut().getDbValue() : "EN ATTENTE") // Retourne les valeurs de la base de données pour le frontend
                .taux(entity.getTaux())
                .typepaiement(entity.getTypepaiement())
                .raisonsortie(entity.getRaisonsortie())
                .demandeur(entity.getDemandeur())
                .fkPatientMediline(entity.getFkPatientMediline())
                .fkFicheMedicale(entity.getFkFicheMedicale())
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .peutEtreAnnule(entity.peutEtreAnnule())
                .build();
    }
}

