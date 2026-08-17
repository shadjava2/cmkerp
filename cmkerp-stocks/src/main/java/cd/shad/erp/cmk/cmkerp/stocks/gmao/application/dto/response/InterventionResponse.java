package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterventionResponse {
  private Long id;
  private String numero;
  private Long fkEquipement;
  private String equipementCode;
  private String equipementDesignation;
  private String typeIntervention;
  private String priorite;
  private String statut;
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
}
