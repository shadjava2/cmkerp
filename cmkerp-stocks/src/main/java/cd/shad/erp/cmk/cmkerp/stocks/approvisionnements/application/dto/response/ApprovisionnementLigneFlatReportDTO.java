package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO plat pour représenter une ligne d'approvisionnement avec les informations de l'approvisionnement.
 * Utilisé pour créer une structure plate dans le rapport liste (plus facile à gérer avec JasperReports).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovisionnementLigneFlatReportDTO {
    // Informations de l'approvisionnement (répétées pour chaque ligne)
    private String numBon;
    private String fournisseurNom;
    private String pharmacieNom;
    private String dateBl;
    private String dateCreation;
    private String statut;
    private String devise;
    private String taux;
    private BigDecimal approvTotalUsd;
    private BigDecimal approvTotalConversion;

    // Informations de la ligne
    private Integer numeroLigne;
    private String produitNom;
    private Float quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal totalUsd;
    private BigDecimal totalConversion;
}

