package cd.shad.erp.cmk.cmkerp.platform.notification.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.NotificationResponse;
import cd.shad.erp.cmk.cmkerp.platform.notification.domain.model.Notification;
import cd.shad.erp.cmk.cmkerp.platform.notification.domain.repository.NotificationRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Service pour la gestion des notifications (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées aux notifications.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Récupère les notifications avec filtres optionnels.
     */
    public List<NotificationResponse> findAll(Long fkUtilisateur, String statut) {
        log.debug("Récupération des notifications - utilisateur: {}, statut: {}", fkUtilisateur, statut);

        List<Notification> notifications;

        if (fkUtilisateur != null && statut != null) {
            notifications = notificationRepository.findByUtilisateurAndStatut(fkUtilisateur, statut);
        } else if (fkUtilisateur != null) {
            notifications = notificationRepository.findByUtilisateur(fkUtilisateur);
        } else if (statut != null) {
            notifications = notificationRepository.findByStatut(statut);
        } else {
            notifications = notificationRepository.findAll();
        }

        return notifications.stream()
                .map(this::notificationToResponseWithUsername)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une notification par son ID.
     */
    public NotificationResponse findById(Long id) {
        log.debug("Récupération de la notification ID: {}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Notification", id));

        return notificationToResponseWithUsername(notification);
    }

    /**
     * Convertit une Notification (domain) en NotificationResponse (DTO) avec le nom d'utilisateur.
     */
    private NotificationResponse notificationToResponseWithUsername(Notification notification) {
        if (notification == null) {
            return null;
        }

        NotificationResponse response = NotificationResponse.builder()
                .id(notification.getId())
                .fkUtilisateur(notification.getFkUtilisateur())
                .typeNotification(notification.getTypeNotification())
                .statut(notification.getStatut())
                .sujet(notification.getSujet())
                .contenu(notification.getContenu())
                .adresseDestinataire(notification.getAdresseDestinataire())
                .dateProgrammee(notification.getDateProgrammee())
                .dateEnvoi(notification.getDateEnvoi())
                .reponse(notification.getReponse())
                .dateCreate(notification.getDateCreate())
                .dateUpdate(notification.getDateUpdate())
                .build();

        // Enrichir avec le nom d'utilisateur
        if (notification.getFkUtilisateur() != null) {
            utilisateurRepository.findById(notification.getFkUtilisateur())
                    .ifPresent(utilisateur -> response.setUsername(utilisateur.getUsername()));
        }

        return response;
    }
}

