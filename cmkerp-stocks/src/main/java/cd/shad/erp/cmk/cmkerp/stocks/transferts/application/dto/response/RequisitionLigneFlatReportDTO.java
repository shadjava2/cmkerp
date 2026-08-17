package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO plat pour représenter une ligne de requisition avec les informations de la requisition.
 * Utilisé pour créer une structure plate dans le rapport liste (plus facile à gérer avec JasperReports).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionLigneFlatReportDTO {
    // Informations de la requisition (répétées pour chaque ligne)
    private Long requisitionId;
    private String pharmacieNom;
    private String pharmacieStockNom;
    private String statut;
    private String dateCreation;
    private String commentaire;
    private Boolean urgent;

    // Informations de la ligne
    private Integer numeroLigne;
    private String nomCommercial;
    private String nomScientifique;
    private String forme;
    private String dosage;
    private String conditionnement;
    private Float quantite;
    private Float quantiteEnStock;
    private Float prixUnitaire; // Optionnel, null si sans prix
    private Float total; // Optionnel, null si sans prix
}

