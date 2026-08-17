package cd.shad.erp.cmk.cmkerp.platform.mapper;

import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.PharmacieRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model.Pharmacie;

/**
 * Mapper pour convertir entre Pharmacie (entité) et DTOs.
 */
@Component
public class PharmacieMapper {

  /**
   * Convertit un PharmacieRequest en entité Pharmacie (pour création).
   */
  public Pharmacie toEntity(PharmacieRequest dto) {
    if (dto == null) {
      return null;
    }

    return Pharmacie.builder()
        .fkSite(dto.getFkSite())
        .designation(dto.getDesignation())
        .typePharmacie(dto.getTypePharmacie())
        .codeimmo(dto.getCodeimmo())
        .typeHospi(dto.getTypeHospi())
        .build();
  }

  /**
   * Met à jour une entité Pharmacie existante à partir d'un PharmacieRequest.
   */
  public void updateEntityFromRequest(PharmacieRequest dto, Pharmacie entity) {
    if (dto == null || entity == null) {
      return;
    }

    if (dto.getFkSite() != null) {
      entity.setFkSite(dto.getFkSite());
    }
    if (dto.getDesignation() != null) {
      entity.setDesignation(dto.getDesignation());
    }
    if (dto.getTypePharmacie() != null) {
      entity.setTypePharmacie(dto.getTypePharmacie());
    }
    if (dto.getCodeimmo() != null) {
      entity.setCodeimmo(dto.getCodeimmo());
    }
    if (dto.getTypeHospi() != null) {
      entity.setTypeHospi(dto.getTypeHospi());
    }
  }

  /**
   * Convertit une entité Pharmacie en PharmacieResponse.
   */
  public PharmacieResponse toResponse(Pharmacie entity) {
    if (entity == null) {
      return null;
    }

    return PharmacieResponse.builder()
        .id(entity.getId())
        .fkSite(entity.getFkSite())
        .designation(entity.getDesignation())
        .typePharmacie(entity.getTypePharmacie())
        .codeimmo(entity.getCodeimmo())
        .typeHospi(entity.getTypeHospi())
        .dateCreate(entity.getDateCreate())
        .dateUpdate(entity.getDateUpdate())
        .build();
  }
}

