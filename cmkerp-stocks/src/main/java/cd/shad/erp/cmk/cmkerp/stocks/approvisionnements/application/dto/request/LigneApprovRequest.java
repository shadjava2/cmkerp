package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO de requête pour la création et la mise à jour d'une ligne d'approvisionnement.
 */
@Data
public class LigneApprovRequest {

    @NotNull(message = "L'approvisionnement est obligatoire")
    private Long fkApprov;

    private Long fkStock;

    @Min(value = 0, message = "La quantité ne peut pas être négative")
    private Float qt;

    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix d'achat ne peut pas être négatif")
    private BigDecimal prixachat;

    @DecimalMin(value = "0.0", inclusive = true, message = "Le total fournisseur ne peut pas être négatif")
    private BigDecimal totalfournisseur;
}

