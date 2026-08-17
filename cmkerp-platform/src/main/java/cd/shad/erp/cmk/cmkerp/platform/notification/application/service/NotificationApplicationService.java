package cd.shad.erp.cmk.cmkerp.platform.notification.application.service;

import cd.shad.erp.cmk.cmkerp.platform.dto.request.NotificationRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.request.UpdateNotificationStatusRequest;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.NotificationResponse;
import cd.shad.erp.cmk.cmkerp.platform.notification.application.port.NotificationPort;
import cd.shad.erp.cmk.cmkerp.platform.notification.domain.model.Notification;
import cd.shad.erp.cmk.cmkerp.platform.notification.domain.repository.NotificationRepository;
import cd.shad.erp.cmk.cmkerp.platform.notification.domain.service.NotificationDomainService;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service pour la gestion des notifications.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationDomainService notificationDomainService;
    private final NotificationPort notificationPort;

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public NotificationResponse findById(Long id) {
        log.debug("Récupération de la notification ID: {}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Notification", id));

        return notificationToResponseWithUsername(notification);
    }

    public NotificationResponse create(NotificationRequest request, Long currentUserId) {
        log.debug("Création d'une nouvelle notification pour l'utilisateur ID: {}", request.getFkUtilisateur());

        // Vérifier que l'utilisateur destinataire existe
        Utilisateur utilisateur = utilisateurRepository.findById(request.getFkUtilisateur())
                .orElseThrow(() -> NotFoundException.entity("Utilisateur", request.getFkUtilisateur()));

        // Créer l'agrégat Notification
        Notification notification = Notification.builder()
                .fkUtilisateur(request.getFkUtilisateur())
                .typeNotification(request.getTypeNotification())
                .statut(Notification.STATUT_EN_ATTENTE)
                .sujet(request.getSujet())
                .contenu(request.getContenu())
                .adresseDestinataire(request.getAdresseDestinataire())
                .dateProgrammee(request.getDateProgrammee())
                .userCreatedId(currentUserId)
                .dateCreate(LocalDateTime.now())
                .build();

        // Validation métier via Domain Service
        notificationDomainService.validerCreationNotification(notification);

        // Utiliser les méthodes métier de l'agrégat
        if (!notification.peutEtreEnvoyee()) {
            throw new BusinessException("La notification ne peut pas être envoyée avec les paramètres fournis");
        }

        // Sauvegarder via le repository
        int rows = notificationRepository.save(notification);
        if (rows == 0) {
            throw new BusinessException("Échec de la création de la notification");
        }

        // Récupérer la notification créée avec son ID
        List<Notification> created = notificationRepository.findByUtilisateur(request.getFkUtilisateur())
                .stream()
                .filter(n -> n.getSujet().equals(request.getSujet())
                        && n.getDateCreate() != null
                        && n.getDateCreate().isAfter(LocalDateTime.now().minusMinutes(1)))
                .limit(1)
                .collect(Collectors.toList());

        if (created.isEmpty()) {
            throw new BusinessException("Erreur lors de la récupération de la notification créée");
        }

        Notification createdNotification = created.get(0);

        // Déclencher l'envoi asynchrone de la notification (email/SMS)
        sendNotificationAsync(createdNotification, utilisateur);

        log.info("Notification créée avec succès: ID={}, type={}", createdNotification.getId(), createdNotification.getTypeNotification());
        return notificationToResponseWithUsername(createdNotification);
    }

    /**
     * Envoie la notification de manière asynchrone (email/SMS).
     * Cette méthode est exécutée dans un thread séparé pour ne pas bloquer le thread HTTP.
     *
     * <p>Pour garantir que l'événement est publié après le commit de la transaction,
     * considérer l'utilisation d'un pattern Outbox (table outbox_events) ou
     * d'un TransactionalEventListener avec un événement Spring ApplicationEvent.
     *
     * @param notification la notification créée
     * @param utilisateur l'utilisateur destinataire
     */
    @Async("cmkerpAsyncExecutor")
    public void sendNotificationAsync(Notification notification, Utilisateur utilisateur) {
        try {
            // Récupérer l'adresse email du destinataire si disponible
            String adresseDestinataire = notification.getAdresseDestinataire();
            if (adresseDestinataire == null || adresseDestinataire.isBlank()) {
                log.debug("Aucune adresse fournie pour la notification ID: {}", notification.getId());
                notification.marquerCommeErreur("Adresse destinataire manquante");
                notificationRepository.update(notification);
                return;
            }

            String typeNotification = notification.getTypeNotification();

            // Utiliser le port pour l'envoi (découplé de l'implémentation)
            notificationPort.sendNotification(
                    typeNotification,
                    adresseDestinataire,
                    notification.getSujet(),
                    notification.getContenu()
            );

            // Marquer la notification comme envoyée
            notification.envoyer();
            notificationRepository.update(notification);

            log.info("Notification envoyée avec succès -> notificationId: {}, type: {}, to: {}",
                    notification.getId(), typeNotification, adresseDestinataire);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification ID: {}", notification.getId(), e);
            notification.marquerCommeErreur("Erreur lors de l'envoi: " + e.getMessage());
            notificationRepository.update(notification);
        }
    }

    public NotificationResponse updateStatus(Long id, UpdateNotificationStatusRequest request, Long currentUserId) {
        log.debug("Mise à jour du statut de la notification ID: {} -> {}", id, request.getStatut());

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> NotFoundException.entity("Notification", id));

        // Validation métier via Domain Service
        notificationDomainService.validerTransitionStatut(notification, request.getStatut());

        // Utiliser les méthodes métier de l'agrégat pour changer le statut
        if (Notification.STATUT_SENT.equalsIgnoreCase(request.getStatut())) {
            notification.envoyer();
        } else if (Notification.STATUT_CANCELLED.equalsIgnoreCase(request.getStatut())) {
            notification.annuler();
        } else if (Notification.STATUT_ERROR.equalsIgnoreCase(request.getStatut())) {
            notification.marquerCommeErreur("Mise à jour manuelle");
        }

        notification.setUserUpdatedId(currentUserId);
        notification.setDateUpdate(LocalDateTime.now());

        int rows = notificationRepository.update(notification);
        if (rows == 0) {
            throw new BusinessException("Échec de la mise à jour du statut de la notification");
        }

        log.info("Statut de la notification mis à jour avec succès: ID={}, statut={}", notification.getId(), notification.getStatut());
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

