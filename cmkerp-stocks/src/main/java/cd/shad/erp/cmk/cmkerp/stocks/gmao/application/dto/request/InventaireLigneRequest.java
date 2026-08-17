package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InventaireLigneRequest {

  @NotBlank
  private String resultat;

  @Size(max = 255)
  private String localisationConstatee;

  @Size(max = 30)
  private String etatConstate;

  @Size(max = 40)
  private String fonctionnementConstate;

  private Boolean consommablesOk;
  private Boolean piecesOk;
  private Boolean manuelUtilisateurOk;
  private Boolean manuelTechniqueOk;
  private Boolean accessoiresOk;

  private String remarque;

  @Size(max = 160)
  private String inventoriste;
}
