package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour une notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

  private Long id;
  private Long fkUtilisateur;
  private String username; // Champ dérivé calculé côté service
  private String typeNotification;
  private String statut;
  private String sujet;
  private String contenu;
  private String adresseDestinataire;
  private LocalDateTime dateProgrammee;
  private LocalDateTime dateEnvoi;
  private String reponse;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}

