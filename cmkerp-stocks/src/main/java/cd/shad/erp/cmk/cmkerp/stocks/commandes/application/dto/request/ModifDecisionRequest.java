package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifDecisionRequest {
  private String decision;
  private String commentaire;
  private List<String> champsApprouves;
}
