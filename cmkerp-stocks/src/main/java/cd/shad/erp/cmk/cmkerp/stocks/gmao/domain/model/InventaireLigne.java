package cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventaireLigne {

  public enum Resultat {
    A_VERIFIER, PRESENT, ABSENT, DEPLACE, HORS_SERVICE, NON_IDENTIFIE
  }

  private Long id;
  private Long fkCampagne;
  private Long fkEquipement;
  private Resultat resultat;
  private String localisationSysteme;
  private String localisationConstatee;
  private String etatConstate;
  private String fonctionnementConstate;
  private Boolean consommablesOk;
  private Boolean piecesOk;
  private Boolean manuelUtilisateurOk;
  private Boolean manuelTechniqueOk;
  private Boolean accessoiresOk;
  private String remarque;
  private String inventoriste;
  private LocalDateTime dateControle;
  private boolean ecart;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreateId;
  private Long userUpdateId;

  // Jointure lecture
  private String equipementCode;
  private String equipementDesignation;
  private String equipementService;
  private String equipementStatut;
}
