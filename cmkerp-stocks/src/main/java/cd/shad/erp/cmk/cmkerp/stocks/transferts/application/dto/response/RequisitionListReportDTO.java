package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour une requisition dans un rapport liste.
 * Utilisé pour remplir le template JasperReports de liste.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionListReportDTO {
    private Long id;
    private String pharmacieNom;
    private String pharmacieStockNom;
    private String statut;
    private String dateCreation;
    private String commentaire;
    private Boolean urgent;
}

