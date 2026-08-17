package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO plat pour représenter une ligne de vente avec les informations de la vente.
 * Utilisé pour créer une structure plate dans le rapport liste (plus facile à gérer avec JasperReports).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenteLigneFlatReportDTO {
    // Informations de la vente (répétées pour chaque ligne)
    private Long venteId;
    private String pharmacieNom;
    private String statut;
    private String dateCreation;
    private String dateValidation;
    private String raisonSortie;
    private String demandeur;
    private BigDecimal venteTotal;

    // Informations de la ligne
    private Integer numeroLigne;
    private String produitNom;
    private Float qt;
    private BigDecimal prixventes;
    private BigDecimal totalLigne;
}

