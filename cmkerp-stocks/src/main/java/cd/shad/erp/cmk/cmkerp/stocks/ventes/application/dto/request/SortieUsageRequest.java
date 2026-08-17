package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corps de la requête de sortie pour usage (miroir de l'annulation).
 */
@Data
public class SortieUsageRequest {

  @Size(max = 255)
  private String raisonsortie;

  @Size(max = 255)
  private String demandeur;
}
