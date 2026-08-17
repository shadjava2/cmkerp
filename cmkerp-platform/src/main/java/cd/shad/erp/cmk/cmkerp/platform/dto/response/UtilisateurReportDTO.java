package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les données du rapport utilisateurs.
 * Utilisé pour remplir le template JasperReports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurReportDTO {
    private Long id;
    private String username;
    private String nom;
    private String postnom;
    private String prenom;
    private String sexe; // "M" pour Masculin, "F" pour Féminin
    private String specialite;
    private String carted;
    private String roleName;
    private Boolean locked;
    private Boolean initPassword;
    private Boolean isLoginCard;
    private String dateCreate; // Formaté en String pour le rapport
    private String dateUpdate; // Formaté en String pour le rapport
}

