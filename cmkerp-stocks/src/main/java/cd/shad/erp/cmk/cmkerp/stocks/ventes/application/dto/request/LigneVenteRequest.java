package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO de requête pour la création et la mise à jour d'une ligne de vente.
 */
@Data
public class LigneVenteRequest {

    @NotNull(message = "La vente est obligatoire")
    private Long fkVente;

    private Long fkStock;

    private Float qt;

    private BigDecimal prixventes;

    private Integer horsconvention;
}

