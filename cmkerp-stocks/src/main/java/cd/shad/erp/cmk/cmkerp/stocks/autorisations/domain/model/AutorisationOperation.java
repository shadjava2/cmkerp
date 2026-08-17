package cd.shad.erp.cmk.cmkerp.stocks.autorisations.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutorisationOperation {

  public static final String TABLE_APPROVISIONNEMENT = "approvsionnements";
  public static final String TYPE_ANNULATION = "ANNULATION";

  public enum StatutAutorisation {
    EN_ATTENTE,
    APPROUVEE,
    REJETEE
  }

  private Long id;
  private String tableCible;
  private Long enregistrementId;
  private String typeOperation;
  private StatutAutorisation statut;
  private String motif;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreateId;
  private Long userDecideId;
  private LocalDateTime dateDecision;
  private String commentaireDecision;
}
