package cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutorisationOperationResponse {

  private Long id;
  private String tableCible;
  private Long enregistrementId;
  private String typeOperation;
  private String statut;
  private String motif;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreateId;
  private String userCreateNom;
  private Long userDecideId;
  private String userDecideNom;
  private LocalDateTime dateDecision;
  private String commentaireDecision;
  /** Libellé contextuel (ex. numéro de bon). */
  private String libelleCible;
}
