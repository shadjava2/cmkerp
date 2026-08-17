package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour une requisition dans un rapport individuel.
 * Utilisé pour remplir le template JasperReports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionReportDTO {
    // Informations de la requisition
    private Long id;
    private String pharmacieNom;
    private String pharmacieStockNom;
    private String statut;
    private String dateCreation;
    private String commentaire;
    private Boolean urgent;

    // Lignes de la requisition
    private List<LigneRequisitionReportDTO> lignes;
}

