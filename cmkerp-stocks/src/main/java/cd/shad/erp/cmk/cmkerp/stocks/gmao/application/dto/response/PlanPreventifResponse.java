package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlanPreventifResponse {
  private Long id;
  private Long fkEquipement;
  private String equipementCode;
  private String equipementDesignation;
  private String libelle;
  private int frequenceJours;
  private LocalDate prochaineEcheance;
  private LocalDate derniereExecution;
  private boolean actif;
  private boolean enRetard;
  private String notes;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
}
