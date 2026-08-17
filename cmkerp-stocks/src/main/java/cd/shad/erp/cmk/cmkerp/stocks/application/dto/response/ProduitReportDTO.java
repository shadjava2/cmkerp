package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les données du rapport produits.
 * Utilisé pour remplir le template JasperReports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitReportDTO {
    private Long id;
    private String codebarre;
    private String nomcommercial;
    private String nomscientifique;
    private String forme;
    private String dosage;
    private String conditionnement;
    private String categorie;
    private Float stockencours;
    private Boolean isactif;
    private String peremption;
    private BigDecimal prixachat;
    private Float qtealert;
    private Float qtcritique;
    private Boolean perimable;
    private LocalDateTime dateApprov; // Date d'approvisionnement (pour achat risqué)
}

