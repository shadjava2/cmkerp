package cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DemandeAutorisationRequest {

  @NotBlank(message = "Le motif est obligatoire")
  private String motif;
}
