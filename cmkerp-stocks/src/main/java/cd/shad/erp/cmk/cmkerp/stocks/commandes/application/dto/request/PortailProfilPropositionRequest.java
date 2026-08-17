package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortailProfilPropositionRequest {
  private String motif;
  private List<ChampProposition> champs;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChampProposition {
    private String champ;
    private String valeurActuelle;
    private String valeurProposee;
  }
}
