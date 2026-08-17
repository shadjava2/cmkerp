package cd.shad.erp.cmk.cmkerp.platform.events;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Événement de notification pour les notifications temps réel.
 *
 * <p>
 * Publié lorsqu'une notification est créée, mise à jour ou envoyée
 * pour permettre la diffusion en temps réel via Server-Sent Events (SSE).
 *

 */
@Getter
public class NotificationEvent extends DomainEvent {

    private final Long notificationId;
    private final Long fkUtilisateur;
    private final String typeNotification;
    private final String statut;
    private final String sujet;
    private final String contenu;
    private final String adresseDestinataire;
    private final LocalDateTime dateProgrammee;
    private final LocalDateTime dateEnvoi;

    public NotificationEvent(Long notificationId, Long fkUtilisateur, String typeNotification,
                             String statut, String sujet, String contenu,
                             String adresseDestinataire, LocalDateTime dateProgrammee,
                             LocalDateTime dateEnvoi) {
        super("NOTIFICATION_EVENT");
        this.notificationId = notificationId;
        this.fkUtilisateur = fkUtilisateur;
        this.typeNotification = typeNotification;
        this.statut = statut;
        this.sujet = sujet;
        this.contenu = contenu;
        this.adresseDestinataire = adresseDestinataire;
        this.dateProgrammee = dateProgrammee;
        this.dateEnvoi = dateEnvoi;
    }
}


