package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour activer ou désactiver un produit au niveau du stock (stock_produits.operationnel).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleOperationnelRequest {

    @NotNull(message = "Le statut opérationnel est requis")
    private Boolean operationnel;
}
