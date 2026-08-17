package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour un transfert dans un rapport liste.
 * Utilisé pour remplir le template JasperReports de liste.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransfertListReportDTO {
    private Long id;
    private Long requisitionNumero;
    private String pharmacieDemandeurNom;
    private String statut;
    private String dateCreation;
}

