package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les données de ligne d'approvisionnement dans le rapport.
 * Utilisé pour remplir le template JasperReports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneApprovReportDTO {
    private Integer numero; // Numéro de ligne (1, 2, 3...)
    private String produitNom;
    private Float quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal totalUsd;
    private BigDecimal totalConversion; // Peut être null si pas de conversion
}

