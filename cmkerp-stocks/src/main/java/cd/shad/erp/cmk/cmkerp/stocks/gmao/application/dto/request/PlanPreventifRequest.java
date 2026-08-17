package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PlanPreventifRequest {

  @NotNull
  private Long fkEquipement;

  @NotBlank
  @Size(max = 255)
  private String libelle;

  @Min(1)
  private int frequenceJours = 90;

  @NotNull
  private LocalDate prochaineEcheance;

  private LocalDate derniereExecution;
  private Boolean actif;
  private String notes;
}
