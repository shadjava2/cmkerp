package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour représenter un approvisionnement avec ses lignes dans un rapport liste.
 * Utilisé pour remplir le template JasperReports de liste d'approvisionnements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovisionnementListReportDTO {
    // Informations de l'approvisionnement
    private String numBon;
    private String fournisseurNom;
    private String pharmacieNom;
    private String dateBl;
    private String dateCreation;
    private String statut;
    private String devise;
    private String taux;

    // Totaux
    private BigDecimal totalUsd;
    private BigDecimal totalConversion;

    // Lignes de l'approvisionnement
    private List<LigneApprovReportDTO> lignes;
}

