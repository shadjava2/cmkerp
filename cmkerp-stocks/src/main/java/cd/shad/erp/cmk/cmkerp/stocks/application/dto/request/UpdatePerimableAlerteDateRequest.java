package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour modifier la date de péremption d'une alerte active.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePerimableAlerteDateRequest {

    @NotNull(message = "La date de péremption est requise")
    private LocalDate dateperemtion;
}
