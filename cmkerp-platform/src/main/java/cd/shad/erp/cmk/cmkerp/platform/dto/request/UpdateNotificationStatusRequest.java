package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête pour la mise à jour du statut d'une notification.
 */
@Data
public class UpdateNotificationStatusRequest {

  @NotBlank(message = "Le statut est obligatoire")
  private String statut; // pending, sent, failed, cancelled
}

