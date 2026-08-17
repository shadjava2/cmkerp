package cd.shad.erp.cmk.cmkerp.platform.mapper;

import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.SiteRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.SiteResponse;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.model.Site;

/**
 * Mapper pour convertir entre Site (entité) et DTOs.
 */
@Component
public class SiteMapper {

  /**
   * Convertit un SiteRequest en entité Site (pour création).
   */
  public Site toEntity(SiteRequest dto) {
    if (dto == null) {
      return null;
    }

    return Site.builder()
        .designation(dto.getDesignation())
        .abbreviation(dto.getAbbreviation())
        .adresse(dto.getAdresse())
        .bloquer(dto.getBloquer() != null ? dto.getBloquer() : false)
        .build();
  }

  /**
   * Met à jour une entité Site existante à partir d'un SiteRequest.
   */
  public void updateEntityFromRequest(SiteRequest dto, Site entity) {
    if (dto == null || entity == null) {
      return;
    }

    if (dto.getDesignation() != null) {
      entity.setDesignation(dto.getDesignation());
    }
    if (dto.getAbbreviation() != null) {
      entity.setAbbreviation(dto.getAbbreviation());
    }
    if (dto.getAdresse() != null) {
      entity.setAdresse(dto.getAdresse());
    }
    if (dto.getBloquer() != null) {
      entity.setBloquer(dto.getBloquer());
    }
  }

  /**
   * Convertit une entité Site en SiteResponse.
   */
  public SiteResponse toResponse(Site entity) {
    if (entity == null) {
      return null;
    }

    return SiteResponse.builder()
        .id(entity.getId())
        .designation(entity.getDesignation())
        .abbreviation(entity.getAbbreviation())
        .adresse(entity.getAdresse())
        .bloquer(entity.getBloquer())
        .dateCreate(entity.getDateCreate())
        .dateUpdate(entity.getDateUpdate())
        .build();
  }
}

