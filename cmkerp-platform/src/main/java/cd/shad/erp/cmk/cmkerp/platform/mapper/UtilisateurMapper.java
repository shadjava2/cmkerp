package cd.shad.erp.cmk.cmkerp.platform.mapper;

import org.springframework.stereotype.Component;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.UtilisateurRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.UtilisateurResponse;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;

/**
 * Mapper pour convertir entre Utilisateur (entité) et DTOs.
 */
@Component
public class UtilisateurMapper {

  /**
   * Convertit un UtilisateurRequest en entité Utilisateur (pour création). Les champs techniques
   * (dateCreate, userCreatedId, etc.) ne sont pas définis ici, ils seront gérés par le service.
   */
  public Utilisateur toEntity(UtilisateurRequest dto) {
    if (dto == null) {
      return null;
    }

    return Utilisateur.builder().username(dto.getUsername()).nom(dto.getNom())
        .postnom(dto.getPostnom()).prenom(dto.getPrenom()).sexe(dto.getSexe())
        .specialite(dto.getSpecialite()).carted(dto.getCarted()).fkRole(dto.getFkRole())
        .locked(dto.getLocked() != null ? dto.getLocked() : false)
        .initPassword(dto.getInitPassword() != null ? dto.getInitPassword() : false)
        .isLoginCard(dto.getIsLoginCard() != null ? dto.getIsLoginCard() : false).build();
  }

  /**
   * Met à jour une entité Utilisateur existante à partir d'un UtilisateurRequest. Les champs
   * techniques ne sont pas modifiés ici.
   */
  public void updateEntityFromRequest(UtilisateurRequest dto, Utilisateur entity) {
    if (dto == null || entity == null) {
      return;
    }

    if (dto.getUsername() != null) {
      entity.setUsername(dto.getUsername());
    }
    if (dto.getNom() != null) {
      entity.setNom(dto.getNom());
    }
    if (dto.getPostnom() != null) {
      entity.setPostnom(dto.getPostnom());
    }
    if (dto.getPrenom() != null) {
      entity.setPrenom(dto.getPrenom());
    }
    if (dto.getSexe() != null) {
      entity.setSexe(dto.getSexe());
    }
    if (dto.getSpecialite() != null) {
      entity.setSpecialite(dto.getSpecialite());
    }
    if (dto.getCarted() != null) {
      entity.setCarted(dto.getCarted());
    }
    if (dto.getFkRole() != null) {
      entity.setFkRole(dto.getFkRole());
    }
    if (dto.getLocked() != null) {
      entity.setLocked(dto.getLocked());
    }
    if (dto.getInitPassword() != null) {
      entity.setInitPassword(dto.getInitPassword());
    }
    if (dto.getIsLoginCard() != null) {
      entity.setIsLoginCard(dto.getIsLoginCard());
    }
  }

  /**
   * Convertit une entité Utilisateur en UtilisateurResponse.
   * 
   * @param entity l'entité Utilisateur
   * @param role le rôle de l'utilisateur (optionnel, pour remplir roleName)
   */
  public UtilisateurResponse toResponse(Utilisateur entity, Role role) {
    if (entity == null) {
      return null;
    }

    return UtilisateurResponse.builder().id(entity.getId()).username(entity.getUsername())
        .nom(entity.getNom()).postnom(entity.getPostnom()).prenom(entity.getPrenom())
        .sexe(entity.getSexe()).specialite(entity.getSpecialite()).carted(entity.getCarted())
        .fkRole(entity.getFkRole()).roleName(role != null ? role.getNom() : null)
        .locked(entity.getLocked()).initPassword(entity.getInitPassword())
        .isLoginCard(entity.getIsLoginCard()).dateCreate(entity.getDateCreate())
        .dateUpdate(entity.getDateUpdate()).userCreatedId(entity.getUserCreatedId())
        .userUpdatedId(entity.getUserUpdatedId()).build();
  }

  /**
   * Convertit une entité Utilisateur en UtilisateurResponse (sans rôle).
   * 
   * @param entity l'entité Utilisateur
   */
  public UtilisateurResponse toResponse(Utilisateur entity) {
    return toResponse(entity, null);
  }
}

