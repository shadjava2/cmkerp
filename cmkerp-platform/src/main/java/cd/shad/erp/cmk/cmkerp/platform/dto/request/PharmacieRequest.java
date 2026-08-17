package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de requête pour la création et la mise à jour d'une pharmacie.
 */
@Data
public class PharmacieRequest {

  @NotNull(message = "Le site est obligatoire")
  private Long fkSite;

  @NotBlank(message = "La désignation est obligatoire")
  private String designation;

  private String typePharmacie;
  private String codeimmo;
  private String typeHospi;
}

