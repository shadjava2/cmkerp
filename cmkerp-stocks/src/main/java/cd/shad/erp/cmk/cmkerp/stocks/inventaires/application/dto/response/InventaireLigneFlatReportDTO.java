package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO plat pour représenter une ligne d'inventaire avec les informations de l'inventaire.
 * Utilisé pour créer une structure plate dans le rapport liste (plus facile à gérer avec JasperReports).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventaireLigneFlatReportDTO {
    // Informations de l'inventaire (répétées pour chaque ligne)
    private Long inventaireId;
    private String pharmacieNom;
    private String statut;
    private String typeinventaire;
    private String dateDebut;
    private String dateFin;
    private String commentaire;
    private String dateCreation;

    // Informations de la ligne
    private Integer numeroLigne;
    private String produitNom;
    private Float quantiteTheorique;
    private Float quantitePhysique;
    private Float ecart;
    private String commentaireLigne;
}

