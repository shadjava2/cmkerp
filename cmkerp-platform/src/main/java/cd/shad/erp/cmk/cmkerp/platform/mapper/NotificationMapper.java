package cd.shad.erp.cmk.cmkerp.platform.mapper;

import org.springframework.stereotype.Component;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.NotificationRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.NotificationResponse;
import cd.shad.erp.cmk.cmkerp.platform.notification.domain.model.Notification;

/**
 * Mapper pour convertir entre Notification (entité) et DTOs.
 */
@Component
public class NotificationMapper {

  /**
   * Convertit un NotificationRequest en entité Notification (pour création).
   */
  public Notification toEntity(NotificationRequest dto) {
    if (dto == null) {
      return null;
    }

    return Notification.builder().fkUtilisateur(dto.getFkUtilisateur())
        .typeNotification(dto.getTypeNotification()).statut("pending") // Statut par défaut à la
                                                                       // création
        .sujet(dto.getSujet()).contenu(dto.getContenu())
        .adresseDestinataire(dto.getAdresseDestinataire()).dateProgrammee(dto.getDateProgrammee())
        .build();
  }

  /**
   * Convertit une entité Notification en NotificationResponse.
   */
  public NotificationResponse toResponse(Notification entity) {
    if (entity == null) {
      return null;
    }

    return NotificationResponse.builder().id(entity.getId())
        .fkUtilisateur(entity.getFkUtilisateur()).typeNotification(entity.getTypeNotification())
        .statut(entity.getStatut()).sujet(entity.getSujet()).contenu(entity.getContenu())
        .adresseDestinataire(entity.getAdresseDestinataire())
        .dateProgrammee(entity.getDateProgrammee()).dateEnvoi(entity.getDateEnvoi())
        .reponse(entity.getReponse()).dateCreate(entity.getDateCreate())
        .dateUpdate(entity.getDateUpdate()).userCreatedId(entity.getUserCreatedId())
        .userUpdatedId(entity.getUserUpdatedId()).build();
  }
}

