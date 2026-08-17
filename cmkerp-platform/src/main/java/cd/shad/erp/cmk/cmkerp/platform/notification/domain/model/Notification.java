package cd.shad.erp.cmk.cmkerp.platform.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Notification - Notification système.
 *
 * <p>Une Notification est un agrégat qui représente une notification envoyée à un utilisateur.
 * Elle peut être de type email, SMS, ou notification interne.
 *
 * <p>Méthodes métier disponibles :
 * <ul>
 *   <li>{@link #envoyer()} - Marquer la notification comme envoyée</li>
 *   <li>{@link #marquerCommeEnvoyee(LocalDateTime)} - Marquer la notification comme envoyée avec date</li>
 *   <li>{@link #marquerCommeErreur(String)} - Marquer la notification comme erreur</li>
 *   <li>{@link #peutEtreEnvoyee()} - Vérifier si la notification peut être envoyée</li>
 * </ul>
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fkUtilisateur")
    private Long fkUtilisateur;

    @Column(name = "type_notification")
    private String typeNotification;  // enum en base : "email", "sms", "interne"

    private String statut;            // enum en base : "EN_ATTENTE", "SENT", "ERROR", "CANCELLED"

    private String sujet;

    @Column(columnDefinition = "text")
    private String contenu;

    @Column(name = "adresse_destinataire")
    private String adresseDestinataire;

    @Column(name = "date_programmee")
    private LocalDateTime dateProgrammee;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(columnDefinition = "text")
    private String reponse;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;

    // ============================================
    // MÉTHODES MÉTIER - Règles de l'agrégat
    // ============================================

    /**
     * Constantes pour les statuts de notification.
     */
    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_SENT = "SENT";
    public static final String STATUT_ERROR = "ERROR";
    public static final String STATUT_CANCELLED = "CANCELLED";

    /**
     * Constantes pour les types de notification.
     */
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_SMS = "sms";
    public static final String TYPE_INTERNE = "interne";

    /**
     * Vérifie si la notification peut être envoyée.
     * Une notification peut être envoyée si :
     * <ul>
     *   <li>Son statut est EN_ATTENTE</li>
     *   <li>Elle a un destinataire (adresse_destinataire ou fkUtilisateur)</li>
     *   <li>Elle a un sujet et un contenu</li>
     * </ul>
     *
     * @return true si la notification peut être envoyée, false sinon
     */
    public boolean peutEtreEnvoyee() {
        if (!STATUT_EN_ATTENTE.equals(statut)) {
            return false;
        }
        if (fkUtilisateur == null && (adresseDestinataire == null || adresseDestinataire.trim().isEmpty())) {
            return false;
        }
        if (sujet == null || sujet.trim().isEmpty()) {
            return false;
        }
        if (contenu == null || contenu.trim().isEmpty()) {
            return false;
        }
        // Vérifier si la date programmée est passée ou nulle
        if (dateProgrammee != null && dateProgrammee.isAfter(LocalDateTime.now())) {
            return false;
        }
        return true;
    }

    /**
     * Marque la notification comme envoyée avec la date d'envoi actuelle.
     */
    public void envoyer() {
        marquerCommeEnvoyee(LocalDateTime.now());
    }

    /**
     * Marque la notification comme envoyée avec une date d'envoi spécifiée.
     *
     * @param dateEnvoi la date d'envoi
     */
    public void marquerCommeEnvoyee(LocalDateTime dateEnvoi) {
        if (!peutEtreEnvoyee() && !STATUT_EN_ATTENTE.equals(statut)) {
            throw new IllegalStateException("Impossible de marquer comme envoyée une notification qui n'est pas en attente");
        }
        this.statut = STATUT_SENT;
        this.dateEnvoi = dateEnvoi != null ? dateEnvoi : LocalDateTime.now();
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Marque la notification comme erreur avec un message d'erreur.
     *
     * @param messageErreur le message d'erreur
     */
    public void marquerCommeErreur(String messageErreur) {
        this.statut = STATUT_ERROR;
        this.reponse = messageErreur != null ? messageErreur : "Erreur lors de l'envoi";
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule la notification.
     */
    public void annuler() {
        if (STATUT_SENT.equals(statut)) {
            throw new IllegalStateException("Impossible d'annuler une notification déjà envoyée");
        }
        this.statut = STATUT_CANCELLED;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si la notification est en attente.
     *
     * @return true si la notification est en attente, false sinon
     */
    public boolean estEnAttente() {
        return STATUT_EN_ATTENTE.equals(statut);
    }

    /**
     * Vérifie si la notification a été envoyée.
     *
     * @return true si la notification a été envoyée, false sinon
     */
    public boolean estEnvoyee() {
        return STATUT_SENT.equals(statut);
    }

    /**
     * Vérifie si la notification est en erreur.
     *
     * @return true si la notification est en erreur, false sinon
     */
    public boolean estEnErreur() {
        return STATUT_ERROR.equals(statut);
    }

    /**
     * Vérifie si la notification est annulée.
     *
     * @return true si la notification est annulée, false sinon
     */
    public boolean estAnnulee() {
        return STATUT_CANCELLED.equals(statut);
    }

    /**
     * Change l'adresse destinataire de la notification.
     *
     * @param nouvelleAdresse la nouvelle adresse
     */
    public void changerAdresseDestinataire(String nouvelleAdresse) {
        this.adresseDestinataire = nouvelleAdresse != null ? nouvelleAdresse.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Programme l'envoi de la notification pour une date ultérieure.
     *
     * @param dateProgrammee la date programmée
     */
    public void programmerPour(LocalDateTime dateProgrammee) {
        if (dateProgrammee != null && dateProgrammee.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date programmée ne peut pas être dans le passé");
        }
        this.dateProgrammee = dateProgrammee;
        this.dateUpdate = LocalDateTime.now();
    }
}

