package cd.shad.erp.cmk.cmkerp.platform.notification.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.notification.domain.model.Notification;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Notification.
 *
 * <p>Cette interface définit le contrat de persistance pour les notifications.
 * L'implémentation sera fournie dans la couche infrastructure.
 */
public interface NotificationRepository {

    /**
     * Trouve une notification par son ID.
     *
     * @param id l'ID de la notification
     * @return Optional contenant la notification si elle existe
     */
    Optional<Notification> findById(Long id);

    /**
     * Trouve toutes les notifications d'un utilisateur.
     *
     * @param utilisateurId l'ID de l'utilisateur
     * @return liste des notifications
     */
    List<Notification> findByUtilisateur(Long utilisateurId);

    /**
     * Trouve toutes les notifications avec un statut donné.
     *
     * @param statut le statut recherché
     * @return liste des notifications
     */
    List<Notification> findByStatut(String statut);

    /**
     * Trouve toutes les notifications d'un utilisateur avec un statut donné.
     *
     * @param utilisateurId l'ID de l'utilisateur
     * @param statut le statut recherché
     * @return liste des notifications
     */
    List<Notification> findByUtilisateurAndStatut(Long utilisateurId, String statut);

    /**
     * Récupère toutes les notifications.
     *
     * @return liste de toutes les notifications
     */
    List<Notification> findAll();

    /**
     * Sauvegarde une nouvelle notification.
     *
     * @param notification la notification à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Notification notification);

    /**
     * Met à jour une notification existante.
     *
     * @param notification la notification à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Notification notification);
}

