package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReouvertureDecisionRequest {
  private String decision;
  private String nouvelleDateLimite;
  private String commentaire;
}
