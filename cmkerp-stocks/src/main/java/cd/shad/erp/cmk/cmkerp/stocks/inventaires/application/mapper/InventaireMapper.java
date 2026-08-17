package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.InventaireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.InventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.Inventaire;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper pour convertir entre Inventaire (entité) et DTOs.
 */
@Component
public class InventaireMapper {

    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER_WITH_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /**
     * Convertit un InventaireRequest en entité Inventaire (pour création).
     */
    public Inventaire toEntity(InventaireRequest dto) {
        if (dto == null) {
            return null;
        }

        LocalDateTime dateDebut = null;
        if (dto.getDate_debut() != null && !dto.getDate_debut().trim().isEmpty()) {
            try {
                // Essayer d'abord avec le format avec millisecondes
                if (dto.getDate_debut().contains(".")) {
                    dateDebut = LocalDateTime.parse(dto.getDate_debut(), ISO_DATETIME_FORMATTER_WITH_MS);
                } else {
                    dateDebut = LocalDateTime.parse(dto.getDate_debut(), ISO_DATETIME_FORMATTER);
                }
            } catch (Exception e) {
                // Si le parsing échoue, utiliser la date actuelle
                dateDebut = LocalDateTime.now();
            }
        } else {
            dateDebut = LocalDateTime.now();
        }

        return Inventaire.builder()
                .fkPharmacie(dto.getFkPharmacie())
                .date_debut(dateDebut)
                // date_fin n'est pas dans le DTO car elle est mise à jour automatiquement
                .statut(dto.getStatut() != null ? Inventaire.StatutInventaire.fromDbValue(dto.getStatut()) : Inventaire.StatutInventaire.EN_COURS)
                .commentaire(dto.getCommentaire())
                .typeinventaire(dto.getTypeinventaire() != null ? Inventaire.TypeInventaire.fromDbValue(dto.getTypeinventaire()) : Inventaire.TypeInventaire.PHYSIQUE)
                .dateCreate(LocalDateTime.now())
                .build();
    }

    /**
     * Met à jour une entité Inventaire existante à partir d'un InventaireRequest.
     */
    public void updateEntityFromRequest(InventaireRequest dto, Inventaire entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getFkPharmacie() != null) {
            entity.setFkPharmacie(dto.getFkPharmacie());
        }
        if (dto.getDate_debut() != null && !dto.getDate_debut().trim().isEmpty()) {
            try {
                if (dto.getDate_debut().contains(".")) {
                    entity.setDate_debut(LocalDateTime.parse(dto.getDate_debut(), ISO_DATETIME_FORMATTER_WITH_MS));
                } else {
                    entity.setDate_debut(LocalDateTime.parse(dto.getDate_debut(), ISO_DATETIME_FORMATTER));
                }
            } catch (Exception e) {
                // Si le parsing échoue, garder la valeur actuelle
            }
        }
        // date_fin n'est pas mise à jour depuis le DTO car elle est gérée automatiquement
        if (dto.getStatut() != null) {
            entity.setStatut(Inventaire.StatutInventaire.fromDbValue(dto.getStatut()));
        }
        entity.setCommentaire(dto.getCommentaire());
        if (dto.getTypeinventaire() != null) {
            entity.setTypeinventaire(Inventaire.TypeInventaire.fromDbValue(dto.getTypeinventaire()));
        }
        entity.setDateUpdate(LocalDateTime.now());
    }

    /**
     * Convertit une entité Inventaire en Response DTO.
     */
    public InventaireResponse toResponse(Inventaire entity, String pharmacieNom) {
        if (entity == null) {
            return null;
        }

        return InventaireResponse.builder()
                .id(entity.getId())
                .fkPharmacie(entity.getFkPharmacie())
                .pharmacieNom(pharmacieNom)
                .date_debut(entity.getDate_debut())
                .date_fin(entity.getDate_fin())
                .statut(entity.getStatut() != null ? entity.getStatut().getDbValue() : "EN COURS")
                .commentaire(entity.getCommentaire())
                .typeinventaire(entity.getTypeinventaire() != null ? entity.getTypeinventaire().getDbValue() : "PHYSIQUE")
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .build();
    }
}

