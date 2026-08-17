package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Saisie inventaire multi-lots : la somme des quantités devient la quantité physique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventaireLotsRequest {

    @NotEmpty(message = "Au moins une ligne lot/date/quantité est requise")
    @Valid
    private List<InventaireLotEntryRequest> entrees;
}
