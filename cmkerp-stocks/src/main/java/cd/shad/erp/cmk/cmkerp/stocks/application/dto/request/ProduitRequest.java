package cd.shad.erp.cmk.cmkerp.stocks.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO de requête pour la création et la mise à jour d'un produit.
 */
@Data
public class ProduitRequest {

    private String codebarre;

    private String nomcommercial;

    private String nomscientifique;

    private Long fkForme;

    private Long fkDosage;

    private Long fkConditionnement;

    private Long fkCategorie;

    @NotNull(message = "Le prix d'achat est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix d'achat doit être supérieur à 0")
    private BigDecimal prixachat;

    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix d'achat comptable ne peut pas être négatif")
    private BigDecimal prixachatcomptable;

    @NotNull(message = "La quantité d'alerte est obligatoire")
    @Min(value = 0, message = "La quantité d'alerte ne peut pas être négative")
    private Float qtealert;

    @NotNull(message = "La quantité critique est obligatoire")
    @Min(value = 0, message = "La quantité critique ne peut pas être négative")
    private Float qtcritique;

    @NotNull(message = "Le champ périssable est obligatoire")
    private Boolean perimable;
}

