package cd.shad.erp.cmk.cmkerp.platform.notification.domain.service;

import cd.shad.erp.cmk.cmkerp.platform.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Domain Service pour le domaine Notification - Gestion des notifications.
 *
 * <p>Ce service contient la logique métier pure liée aux notifications qui ne peut pas
 * être encapsulée dans l'agrégat Notification lui-même.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Validation des règles métier complexes sur les notifications</li>
 *   <li>Validation des transitions de statut</li>
 * </ul>
 *
 * <p>Note : La plupart de la logique métier est déjà dans l'agrégat Notification
 * (méthodes peutEtreEnvoyee(), marquerCommeEnvoyee(), etc.).
 * Ce service contient uniquement les règles qui nécessitent plusieurs agrégats
 * ou des validations transverses.
 */
@Service
@RequiredArgsConstructor
public class NotificationDomainService {

    /**
     * Valide qu'une notification peut être créée.
     * Vérifie que les règles métier sont respectées.
     *
     * @param notification la notification à valider
     * @throws IllegalArgumentException si la notification ne peut pas être créée
     */
    public void validerCreationNotification(Notification notification) {
        if (notification.getFkUtilisateur() == null &&
            (notification.getAdresseDestinataire() == null || notification.getAdresseDestinataire().trim().isEmpty())) {
            throw new IllegalArgumentException("Une notification doit avoir un destinataire (utilisateur ou adresse)");
        }

        if (notification.getSujet() == null || notification.getSujet().trim().isEmpty()) {
            throw new IllegalArgumentException("Une notification doit avoir un sujet");
        }

        if (notification.getContenu() == null || notification.getContenu().trim().isEmpty()) {
            throw new IllegalArgumentException("Une notification doit avoir un contenu");
        }

        // Valider le type de notification
        if (notification.getTypeNotification() != null) {
            String type = notification.getTypeNotification().toLowerCase();
            if (!type.equals(Notification.TYPE_EMAIL) &&
                !type.equals(Notification.TYPE_SMS) &&
                !type.equals(Notification.TYPE_INTERNE)) {
                throw new IllegalArgumentException(
                    String.format("Type de notification invalide: %s. Types acceptés: email, sms, interne", type));
            }
        }
    }

    /**
     * Valide qu'une notification peut être envoyée.
     * Utilise la méthode métier de l'agrégat et ajoute des validations supplémentaires si nécessaire.
     *
     * @param notification la notification à valider
     * @return true si la notification peut être envoyée, false sinon
     */
    public boolean peutEnvoyer(Notification notification) {
        if (notification == null) {
            return false;
        }

        // Utiliser la méthode métier de l'agrégat
        return notification.peutEtreEnvoyee();
    }

    /**
     * Valide qu'une notification peut changer de statut.
     * Vérifie les transitions autorisées.
     *
     * @param notification la notification
     * @param nouveauStatut le nouveau statut
     * @throws IllegalStateException si la transition n'est pas autorisée
     */
    public void validerTransitionStatut(Notification notification, String nouveauStatut) {
        if (notification == null) {
            throw new IllegalArgumentException("La notification ne peut pas être null");
        }

        String statutActuel = notification.getStatut();

        // Transition depuis EN_ATTENTE
        if (Notification.STATUT_EN_ATTENTE.equals(statutActuel)) {
            if (!nouveauStatut.equals(Notification.STATUT_SENT) &&
                !nouveauStatut.equals(Notification.STATUT_ERROR) &&
                !nouveauStatut.equals(Notification.STATUT_CANCELLED)) {
                throw new IllegalStateException(
                    String.format("Transition non autorisée de %s vers %s", statutActuel, nouveauStatut));
            }
        }

        // Une notification SENT ne peut plus changer de statut (sauf erreur ?)
        if (Notification.STATUT_SENT.equals(statutActuel)) {
            if (!nouveauStatut.equals(Notification.STATUT_ERROR)) {
                throw new IllegalStateException(
                    String.format("Une notification déjà envoyée ne peut pas passer au statut %s", nouveauStatut));
            }
        }

        // Une notification CANCELLED ne peut plus changer de statut
        if (Notification.STATUT_CANCELLED.equals(statutActuel)) {
            throw new IllegalStateException("Une notification annulée ne peut pas changer de statut");
        }
    }
}

