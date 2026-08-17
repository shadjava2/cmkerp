package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ligne perimable_alerte_stock pour saisie inventaire (lot + péremption + quantité).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerimableAlerteStockResponse {

    private Long id;
    private Long fkStock;
    private String lot;
    private LocalDate dateperemtion;
    private Float stockexpiree;
}
