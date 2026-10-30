package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FacturationSyncApplyRequest {

  public enum Action {
    /** Copier PAU Beach → CH */
    PRICE_BEACH_TO_CH,
    /** Copier PAU CH → Beach */
    PRICE_CH_TO_BEACH,
    /** Importer fiche TPRODUIT+TSTOCK depuis CH vers Beach */
    IMPORT_CH_TO_BEACH,
    /** Importer fiche TPRODUIT+TSTOCK depuis Beach vers CH */
    IMPORT_BEACH_TO_CH
  }

  @NotNull
  private Action action;

  @NotEmpty
  private List<String> codes = new ArrayList<>();
}
