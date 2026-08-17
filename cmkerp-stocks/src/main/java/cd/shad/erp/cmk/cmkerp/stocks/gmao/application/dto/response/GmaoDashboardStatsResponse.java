package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GmaoDashboardStatsResponse {
  private long equipementsActifs;
  private long equipementsEnPanne;
  private long equipementsEnMaintenance;
  private long interventionsOuvertes;
  private long interventionsEnCours;
  private long plansEnRetard;
}
