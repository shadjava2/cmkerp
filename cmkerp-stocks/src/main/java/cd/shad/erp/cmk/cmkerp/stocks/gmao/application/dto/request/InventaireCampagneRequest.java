package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class InventaireCampagneRequest {

  @NotBlank
  @Size(max = 255)
  private String libelle;

  @NotNull
  private LocalDate dateDebut;

  private LocalDate dateFinPrevue;

  @Size(max = 160)
  private String perimetreService;

  @Size(max = 40)
  private String perimetreCategorie;

  @Size(max = 160)
  private String responsable;

  private String notes;

  /** Si true, génère immédiatement les lignes depuis le parc filtré. */
  private Boolean genererLignes;
}
