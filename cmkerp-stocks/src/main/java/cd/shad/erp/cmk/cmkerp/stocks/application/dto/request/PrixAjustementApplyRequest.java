package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrixAjustementApplyRequest {

  public enum Direction {
    /** Copie produits.prixachat → TSTOCK.PAU */
    ERP_TO_CLINIQUE,
    /** Copie TSTOCK.PAU → produits.prixachat */
    CLINIQUE_TO_ERP,
    /** Restaure ancienprixachat → prixachat et ancienpau → TSTOCK.PAU */
    RESTORE
  }

  @NotNull
  private Direction direction;

  @NotEmpty
  @Size(max = 200)
  private List<Long> produitIds = new ArrayList<>();
}
