package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour retirer du stock périmé.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetirerStockExpireRequest {

    /**
     * ID du stock (stock_produits.id)
     */
    @NotNull(message = "L'ID du stock est requis")
    @Positive(message = "L'ID du stock doit être positif")
    private Long fkStock;

    /**
     * Date de péremption de l'alerte à retirer (optionnel).
     * Si fournie, retire uniquement l'alerte avec cette date.
     * Si null, retire toutes les alertes actives pour ce stock.
     */
    private LocalDate dateperemtion;

    /**
     * Quantité de stock expiré à retirer.
     * Peut être 0 si le produit a déjà été liquidé.
     */
    @NotNull(message = "La quantité est requise")
    @Min(value = 0, message = "La quantité ne peut pas être négative")
    private Float stockexpiree;
}

