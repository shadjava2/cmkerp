package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les lignes de transfert dans un rapport.
 * Utilisé pour remplir le template JasperReports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneTransfertReportDTO {
    private Long id;
    private Long fkStock;
    private String nomCommercial;
    private String nomScientifique;
    private String forme;
    private String dosage;
    private String conditionnement;
    private Double quantiteDemandee;
    private Double quantite;
    private Double quantiteEnStock;
}

