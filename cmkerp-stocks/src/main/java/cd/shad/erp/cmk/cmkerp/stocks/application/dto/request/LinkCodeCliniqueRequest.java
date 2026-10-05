package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Corps PATCH pour lier / délier un code CLINIQUE. */
@Data
public class LinkCodeCliniqueRequest {

  /** Code {@code TPRODUIT.CODE}. Null ou vide = délier. */
  @Size(max = 255)
  private String codeClinique;
}
