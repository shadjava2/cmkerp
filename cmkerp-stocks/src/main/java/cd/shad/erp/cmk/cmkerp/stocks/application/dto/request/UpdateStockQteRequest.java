package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour ajuster la quantité en stock (stock_produits.qte) — sans changement de schéma.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockQteRequest {

  @NotNull(message = "La quantité est requise")
  private Float qte;
}
