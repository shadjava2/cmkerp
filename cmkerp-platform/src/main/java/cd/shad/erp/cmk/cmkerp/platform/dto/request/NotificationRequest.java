package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * DTO de requête pour la création d'une notification.
 */
@Data
public class NotificationRequest {

  @NotNull(message = "L'utilisateur destinataire est obligatoire")
  private Long fkUtilisateur;

  @NotBlank(message = "Le type de notification est obligatoire")
  private String typeNotification; // email, sms

  @NotBlank(message = "Le sujet est obligatoire")
  private String sujet;

  @NotBlank(message = "Le contenu est obligatoire")
  private String contenu;

  private String adresseDestinataire;
  private LocalDateTime dateProgrammee;
}

