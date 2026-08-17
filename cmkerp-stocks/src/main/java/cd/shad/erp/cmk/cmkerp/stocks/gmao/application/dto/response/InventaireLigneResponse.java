package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventaireLigneResponse {
  private Long id;
  private Long fkCampagne;
  private Long fkEquipement;
  private String equipementCode;
  private String equipementDesignation;
  private String equipementService;
  private String equipementStatut;
  private String resultat;
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
}
