package cd.shad.erp.cmk.cmkerp.platform.mapper;

import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.PermissionResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;

/**
 * Mapper pour convertir entre Permission (entité) et DTOs.
 * Les permissions sont généralement en lecture seule.
 */
@Component
public class PermissionMapper {

  /**
   * Convertit une entité Permission en PermissionResponse.
   */
  public PermissionResponse toResponse(Permission entity) {
    if (entity == null) {
      return null;
    }

    return PermissionResponse.builder()
        .id(entity.getId())
        .nom(entity.getNom())
        .description(entity.getDescription())
        .dateCreate(entity.getDateCreate())
        .dateUpdate(entity.getDateUpdate())
        .build();
  }
}

