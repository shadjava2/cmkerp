package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête pour la création et la mise à jour d'un rôle.
 */
@Data
public class RoleRequest {

  @NotBlank(message = "Le nom du rôle est obligatoire")
  private String nom;

  private String description;
}

