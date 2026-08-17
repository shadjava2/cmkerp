package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les lignes de requisition dans un rapport.
 * Utilisé pour remplir le template JasperReports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneRequisitionReportDTO {
    private Long id;
    private Long fkStock;
    private String nomCommercial;
    private String nomScientifique;
    private String forme;
    private String dosage;
    private String conditionnement;
    private Float quantite;
    private Float quantiteEnStock;
    private Float prixUnitaire; // Prix unitaire (optionnel, null si sans prix)
    private Float total; // Total = quantite * prixUnitaire (optionnel, null si sans prix)
}

