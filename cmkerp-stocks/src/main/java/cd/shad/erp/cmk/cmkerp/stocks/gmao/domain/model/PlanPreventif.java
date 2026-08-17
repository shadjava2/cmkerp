package cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPreventif {

  private Long id;
  private Long fkEquipement;
  private String equipementCode;
  private String equipementDesignation;
  private String libelle;
  private int frequenceJours;
  private LocalDate prochaineEcheance;
  private LocalDate derniereExecution;
  private boolean actif;
  private String notes;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreateId;
  private Long userUpdateId;

  public boolean isEnRetard() {
    return actif && prochaineEcheance != null && prochaineEcheance.isBefore(LocalDate.now());
  }
}
