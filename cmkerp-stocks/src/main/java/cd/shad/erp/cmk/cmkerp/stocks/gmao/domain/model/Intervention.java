package cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Intervention {

  public enum Type {
    CORRECTIVE, PREVENTIVE, INSPECTION
  }

  public enum Priorite {
    BASSE, NORMALE, HAUTE, URGENTE
  }

  public enum Statut {
    BROUILLON, PLANIFIEE, EN_COURS, CLOTUREE, ANNULEE
  }

  private Long id;
  private String numero;
  private Long fkEquipement;
  private String equipementCode;
  private String equipementDesignation;
  private Type typeIntervention;
  private Priorite priorite;
  private Statut statut;
  private String titre;
  private String description;
  private String diagnostic;
  private String travauxRealises;
  private String technicienNom;
  private Long technicienUserId;
  private Long fkPharmacie;
  private LocalDateTime dateDemande;
  private LocalDateTime datePlanifiee;
  private LocalDateTime dateDebut;
  private LocalDateTime dateCloture;
  private BigDecimal coutEstime;
  private BigDecimal coutReel;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreateId;
  private Long userUpdateId;
}
