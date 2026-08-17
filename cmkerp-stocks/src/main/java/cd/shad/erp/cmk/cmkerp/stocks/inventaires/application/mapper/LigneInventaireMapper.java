package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.mapper;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.LigneInventaireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.LigneInventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service.LigneInventaireQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model.LigneInventaire;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir entre LigneInventaire (entité) et DTOs.
 */
@Component
public class LigneInventaireMapper {

    /**
     * Met à jour une entité LigneInventaire existante à partir d'un LigneInventaireRequest.
     * Note: Les lignes sont créées automatiquement par une procédure stockée,
     * on ne peut que les mettre à jour.
     */
    public void updateEntityFromRequest(LigneInventaireRequest dto, LigneInventaire entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getQuantite_physique() != null) {
            entity.setQuantite_physique(dto.getQuantite_physique());
        }
        entity.setCommentaire(dto.getCommentaire());
    }

    /**
     * Convertit une entité LigneInventaire en Response DTO avec les informations du produit.
     * @param entity L'entité LigneInventaire
     * @param produitInfo Les informations du produit (nom commercial, nom scientifique, forme, dosage, conditionnement, péremption)
     */
    public LigneInventaireResponse toResponse(LigneInventaire entity, LigneInventaireQueryService.ProduitInfo produitInfo) {
        if (entity == null) {
            return null;
        }

        // Calculer l'écart (quantite_physique - quantite_theorique)
        Float ecart = null;
        if (entity.getQuantite_physique() != null && entity.getQuantite_theorique() != null) {
            ecart = entity.getQuantite_physique() - entity.getQuantite_theorique();
        }

        String nomcommercial = produitInfo != null ? produitInfo.nomcommercial : null;

        return LigneInventaireResponse.builder()
                .id(entity.getId())
                .fkInventaire(entity.getFkInventaire())
                .fkStock(entity.getFkStock())
                .produitNom(nomcommercial) // Pour compatibilité
                .nomcommercial(nomcommercial)
                .nomscientifique(produitInfo != null ? produitInfo.nomscientifique : null)
                .forme(produitInfo != null ? produitInfo.forme : null)
                .dosage(produitInfo != null ? produitInfo.dosage : null)
                .conditionnement(produitInfo != null ? produitInfo.conditionnement : null)
                .peremption(produitInfo != null ? produitInfo.peremption : null)
                .codebarre(produitInfo != null ? produitInfo.codebarre : null)
                .operationnel(produitInfo != null ? produitInfo.operationnel : null)
                .quantite_theorique(entity.getQuantite_theorique())
                .quantite_physique(entity.getQuantite_physique())
                .ecart(ecart)
                .commentaire(entity.getCommentaire())
                .dateCreate(entity.getDateCreate())
                .dateUpdate(entity.getDateUpdate())
                .userCreatedId(entity.getUserCreatedId())
                .userUpdatedId(entity.getUserUpdatedId())
                .build();
    }
}

