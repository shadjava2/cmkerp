package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.request;

import lombok.Data;

@Data
public class ClotureInterventionRequest {
  private String travauxRealises;
  private String diagnostic;
  private java.math.BigDecimal coutReel;
  private Boolean remettreEnService = true;
}
