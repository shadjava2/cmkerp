package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Une saisie inventaire : lot + date de péremption + quantité trouvée (rayon / emplacement).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventaireLotEntryRequest {

    /** Numéro de lot (optionnel). */
    private String lot;

    @NotNull(message = "La date de péremption est requise")
    private LocalDate dateperemtion;

    @NotNull(message = "La quantité est requise")
    @PositiveOrZero(message = "La quantité doit être positive ou nulle")
    private Float quantite;
}
