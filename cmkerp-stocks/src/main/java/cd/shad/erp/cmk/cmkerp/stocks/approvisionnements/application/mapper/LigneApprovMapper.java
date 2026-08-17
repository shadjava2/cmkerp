package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.mapper;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.LigneApprovRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model.LigneApprov;

/**
 * Mapper pour convertir entre LigneApprov (entité) et DTOs.
 */
@Component
public class LigneApprovMapper {

  /**
   * Convertit un LigneApprovRequest en entité LigneApprov (pour création).
   */
  public LigneApprov toEntity(LigneApprovRequest dto) {
    if (dto == null) {
      return null;
    }

    LigneApprov ligne = LigneApprov.builder().fkApprov(dto.getFkApprov()).fkStock(dto.getFkStock())
        .qt(dto.getQt()).prixachat(dto.getPrixachat()).totalfournisseur(dto.getTotalfournisseur())
        .dateCreate(LocalDateTime.now()).build();

    // Calculer le prix d'achat total
    ligne.calculerPrixAchatTotal();

    return ligne;
  }

  /**
   * Met à jour une entité LigneApprov existante à partir d'un LigneApprovRequest.
   */
  public void updateEntityFromRequest(LigneApprovRequest dto, LigneApprov entity) {
    if (dto == null || entity == null) {
      return;
    }

    if (dto.getFkStock() != null) {
      entity.setFkStock(dto.getFkStock());
    }
    if (dto.getQt() != null) {
      entity.setQt(dto.getQt());
    }
    if (dto.getPrixachat() != null) {
      entity.setPrixachat(dto.getPrixachat());
    }
    if (dto.getTotalfournisseur() != null) {
      entity.setTotalfournisseur(dto.getTotalfournisseur());
    }

    // Recalculer le prix d'achat total
    entity.calculerPrixAchatTotal();
    entity.setDateUpdate(LocalDateTime.now());
  }

  /**
   * Convertit une entité LigneApprov en Response DTO.
   */
  public LigneApprovResponse toResponse(LigneApprov entity, String produitNom) {
    return toResponse(entity, null, produitNom, null);
  }

  public LigneApprovResponse toResponse(LigneApprov entity, String produitNom, Float stockActuel) {
    return toResponse(entity, null, produitNom, stockActuel);
  }

  public LigneApprovResponse toResponse(
      LigneApprov entity, Long produitId, String produitNom, Float stockActuel) {
    if (entity == null) {
      return null;
    }

    return LigneApprovResponse.builder().id(entity.getId()).fkApprov(entity.getFkApprov())
        .fkStock(entity.getFkStock()).produitId(produitId).produitNom(produitNom).qt(entity.getQt())
        .stockActuel(stockActuel)
        .prixachat(entity.getPrixachat()).prixachattotal(entity.getPrixachattotal())
        .totalfournisseur(entity.getTotalfournisseur()).dateCreate(entity.getDateCreate())
        .dateUpdate(entity.getDateUpdate()).userCreatedId(entity.getUserCreatedId())
        .userUpdatedId(entity.getUserUpdatedId()).build();
  }
}

