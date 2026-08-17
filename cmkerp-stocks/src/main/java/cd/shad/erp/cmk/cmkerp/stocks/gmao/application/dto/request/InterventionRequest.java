package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InterventionRequest {

  @NotNull
  private Long fkEquipement;

  @NotBlank
  private String typeIntervention;

  @NotBlank
  private String priorite;

  private String statut;

  @NotBlank
  @Size(max = 255)
  private String titre;

  private String description;
  private String diagnostic;
  private String travauxRealises;

  @Size(max = 160)
  private String technicienNom;

  private Long technicienUserId;
  private Long fkPharmacie;
  private LocalDateTime datePlanifiee;
  private BigDecimal coutEstime;
  private BigDecimal coutReel;
}
