package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour un stock disponible (pour remplacer un produit).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDisponibleResponse {
    private Long id;
    private String nomCommercial;
    private String nomScientifique;
    private String forme;
    private String dosage;
    private String conditionnement;
    private Double quantiteEnStock;
}

