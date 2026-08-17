package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête pour la création et la mise à jour d'un site.
 */
@Data
public class SiteRequest {

  @NotBlank(message = "La désignation est obligatoire")
  private String designation;

  private String abbreviation;
  private String adresse;
  private Boolean bloquer;
}

