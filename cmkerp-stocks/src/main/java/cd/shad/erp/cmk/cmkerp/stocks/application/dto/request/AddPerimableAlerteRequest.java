package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour ajouter une alerte de péremption avec approv = false.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPerimableAlerteRequest {

    /**
     * ID du stock (stock_produits.id)
     */
    @NotNull(message = "L'ID du stock est requis")
    @Positive(message = "L'ID du stock doit être positif")
    private Long fkStock;

    /**
     * ID de l'approvisionnement (approvisionnement.id) - optionnel
     */
    private Long fkAprov;

    /**
     * Date de péremption
     */
    @NotNull(message = "La date de péremption est requise")
    private LocalDate dateperemtion;

    /** Numéro de lot (optionnel). */
    private String lot;

    /** Quantité associée à cette alerte (inventaire / retrait stock expiré). */
    private Float stockexpiree;
}

