package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour un utilisateur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponse {

  private Long id;
  private String username;
  private String nom;
  private String postnom;
  private String prenom;
  private String sexe; // "M" pour Masculin, "F" pour Féminin
  private String specialite;
  private String carted;
  private Long fkRole;
  private String roleName; // Champ dérivé calculé côté service
  private Boolean locked;
  private Boolean initPassword;
  private Boolean isLoginCard;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}

