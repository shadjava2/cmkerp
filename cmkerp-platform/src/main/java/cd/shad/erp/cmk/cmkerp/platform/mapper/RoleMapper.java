package cd.shad.erp.cmk.cmkerp.platform.mapper;

import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.RoleRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.RoleResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;

/**
 * Mapper pour convertir entre Role (entité) et DTOs.
 */
@Component
public class RoleMapper {

  /**
   * Convertit un RoleRequest en entité Role (pour création).
   */
  public Role toEntity(RoleRequest dto) {
    if (dto == null) {
      return null;
    }

    return Role.builder()
        .nom(dto.getNom())
        .description(dto.getDescription())
        .build();
  }

  /**
   * Met à jour une entité Role existante à partir d'un RoleRequest.
   */
  public void updateEntityFromRequest(RoleRequest dto, Role entity) {
    if (dto == null || entity == null) {
      return;
    }

    if (dto.getNom() != null) {
      entity.setNom(dto.getNom());
    }
    if (dto.getDescription() != null) {
      entity.setDescription(dto.getDescription());
    }
  }

  /**
   * Convertit une entité Role en RoleResponse.
   */
  public RoleResponse toResponse(Role entity) {
    if (entity == null) {
      return null;
    }

    return RoleResponse.builder()
        .id(entity.getId())
        .nom(entity.getNom())
        .description(entity.getDescription())
        .dateCreate(entity.getDateCreate())
        .dateUpdate(entity.getDateUpdate())
        .build();
  }
}

