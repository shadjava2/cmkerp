package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO plat pour représenter une ligne de transfert avec les informations du transfert.
 * Utilisé pour créer une structure plate dans le rapport liste (plus facile à gérer avec JasperReports).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransfertLigneFlatReportDTO {
    // Informations du transfert (répétées pour chaque ligne)
    private Long transfertId;
    private Long requisitionNumero;
    private String pharmacieDemandeurNom;
    private String statut;
    private String dateCreation;

    // Informations de la ligne
    private Integer numeroLigne;
    private String nomCommercial;
    private String nomScientifique;
    private String forme;
    private String dosage;
    private String conditionnement;
    private Double quantiteDemandee;
    private Double quantite;
    private Double quantiteEnStock;
}

