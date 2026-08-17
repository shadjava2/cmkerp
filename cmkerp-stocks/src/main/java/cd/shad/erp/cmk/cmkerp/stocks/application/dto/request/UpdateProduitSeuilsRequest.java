package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour les seuils produit (colonnes qtealert / qtcritique inchangées en base).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProduitSeuilsRequest {

  @NotNull(message = "Le stock alerte est requis")
  private Float qtealert;

  @NotNull(message = "Le stock critique est requis")
  private Float qtcritique;
}
