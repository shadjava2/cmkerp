package cd.shad.erp.cmk.cmkerp.platform.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.common.infrastructure.adapter.EmailService;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;

/**
 * Consumer Kafka pour les événements utilisateur.
 */
@Component
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class UserEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

  private final EmailService emailService;
  private final UtilisateurRepository utilisateurRepository;

  public UserEventConsumer(@Autowired(required = false) EmailService emailService,
      UtilisateurRepository utilisateurRepository) {
    this.emailService = emailService;
    this.utilisateurRepository = utilisateurRepository;
  }

  /**
   * Consomme les événements utilisateur depuis Kafka.
   *
   * @param event l'événement utilisateur
   */
  @KafkaListener(topics = "cmkerp-user-events", groupId = "${spring.kafka.consumer.group-id:cmkerp-api-group}", errorHandler = "kafkaErrorHandler")
  public void consumeUserEvent(UserEvent event) {
    log.info("Événement utilisateur reçu -> type: {}, userId: {}, username: {}", event.getType(), event.getUserId(),
        event.getUsername());

    try {
      switch (event.getType()) {
        case USER_CREATED:
          handleUserCreated(event);
          break;
        case USER_UPDATED:
          handleUserUpdated(event);
          break;
        case USER_LOCKED:
          handleUserLocked(event);
          break;
        case USER_UNLOCKED:
          handleUserUnlocked(event);
          break;
        case USER_DELETED:
          handleUserDeleted(event);
          break;
        default:
          log.warn("Type d'événement utilisateur non géré: {}", event.getType());
      }
    } catch (Exception e) {
      log.error("Erreur lors du traitement de l'événement utilisateur -> type: {}, userId: {}", event.getType(),
          event.getUserId(), e);
      // Ne pas propager l'exception pour éviter le retry Kafka (ou configurer un Dead
      // Letter Topic)
    }
  }

  /**
   * Traite l'événement USER_CREATED : envoi d'un email de bienvenue.
   *
   * @param event l'événement USER_CREATED
   */
  private void handleUserCreated(UserEvent event) {
    log.info("Traitement de l'événement USER_CREATED -> userId: {}, username: {}", event.getUserId(),
        event.getUsername());

    // Récupérer l'utilisateur depuis la base de données
    utilisateurRepository.findByUsername(event.getUsername()).ifPresentOrElse(
        utilisateur -> {
          // Récupérer l'email depuis la table utilisateurs si la colonne existe
          String email = utilisateurRepository.findEmailByUsername(event.getUsername())
              .orElseGet(() -> {
                // Fallback : générer un email basé sur le username si la colonne n'existe pas
                log.debug("Email non trouvé dans la base, utilisation du fallback pour username: {}",
                    event.getUsername());
                return event.getUsername() + "@cmkerp.local";
              });

          log.info("Envoi de l'email de bienvenue -> userId: {}, email: {}", event.getUserId(), email);
          if (emailService != null) {
            emailService.sendWelcomeEmail(email, event.getUsername());
          } else {
            log.debug("EmailService non disponible, email de bienvenue non envoyé");
          }
        },
        () -> log.warn("Utilisateur non trouvé pour l'événement USER_CREATED -> username: {}", event.getUsername()));
  }

  /**
   * Traite l'événement USER_UPDATED : envoi d'une notification par email.
   *
   * @param event l'événement USER_UPDATED
   */
  private void handleUserUpdated(UserEvent event) {
    log.info("Traitement de l'événement USER_UPDATED -> userId: {}, username: {}", event.getUserId(),
        event.getUsername());

    // Récupérer l'utilisateur depuis la base de données
    utilisateurRepository.findByUsername(event.getUsername()).ifPresentOrElse(
        utilisateur -> {
          // Récupérer l'email depuis la table utilisateurs si la colonne existe
          String email = utilisateurRepository.findEmailByUsername(event.getUsername())
              .orElseGet(() -> {
                // Fallback : générer un email basé sur le username si la colonne n'existe pas
                log.debug("Email non trouvé dans la base, utilisation du fallback pour username: {}",
                    event.getUsername());
                return event.getUsername() + "@cmkerp.local";
              });

          String subject = "Mise à jour de votre compte CMK-ERP";
          String content = String.format(
              "Bonjour %s,\n\n"
                  + "Votre compte a été mis à jour dans CMK-ERP.\n"
                  + "Si vous n'avez pas effectué cette modification, veuillez contacter l'administrateur.\n\n"
                  + "Cordialement,\n"
                  + "L'équipe CMK-ERP",
              event.getUsername());

          log.info("Envoi de la notification de mise à jour -> userId: {}, email: {}", event.getUserId(), email);
          if (emailService != null) {
            emailService.sendNotificationEmail(email, subject, content);
          } else {
            log.debug("EmailService non disponible, notification non envoyée");
          }
        },
        () -> log.warn("Utilisateur non trouvé pour l'événement USER_UPDATED -> username: {}", event.getUsername()));
  }

  /**
   * Traite l'événement USER_LOCKED : envoi d'une notification par email.
   *
   * @param event l'événement USER_LOCKED
   */
  private void handleUserLocked(UserEvent event) {
    log.warn("Traitement de l'événement USER_LOCKED -> userId: {}, username: {}", event.getUserId(),
        event.getUsername());

    // Récupérer l'utilisateur depuis la base de données
    utilisateurRepository.findByUsername(event.getUsername()).ifPresentOrElse(
        utilisateur -> {
          // Récupérer l'email depuis la table utilisateurs si la colonne existe
          String email = utilisateurRepository.findEmailByUsername(event.getUsername())
              .orElseGet(() -> {
                // Fallback : générer un email basé sur le username si la colonne n'existe pas
                log.debug("Email non trouvé dans la base, utilisation du fallback pour username: {}",
                    event.getUsername());
                return event.getUsername() + "@cmkerp.local";
              });

          String subject = "Verrouillage de votre compte CMK-ERP";
          String content = String.format(
              "Bonjour %s,\n\n"
                  + "Votre compte a été verrouillé dans CMK-ERP pour des raisons de sécurité.\n"
                  + "Si vous pensez qu'il s'agit d'une erreur, veuillez contacter l'administrateur système.\n\n"
                  + "Cordialement,\n"
                  + "L'équipe CMK-ERP",
              event.getUsername());

          log.info("Envoi de la notification de verrouillage -> userId: {}, email: {}", event.getUserId(), email);
          if (emailService != null) {
            emailService.sendNotificationEmail(email, subject, content);
          } else {
            log.debug("EmailService non disponible, notification non envoyée");
          }
        },
        () -> log.warn("Utilisateur non trouvé pour l'événement USER_LOCKED -> username: {}", event.getUsername()));
  }

  /**
   * Traite l'événement USER_UNLOCKED : envoi d'une notification par email.
   *
   * @param event l'événement USER_UNLOCKED
   */
  private void handleUserUnlocked(UserEvent event) {
    log.info("Traitement de l'événement USER_UNLOCKED -> userId: {}, username: {}", event.getUserId(),
        event.getUsername());

    // Récupérer l'utilisateur depuis la base de données
    utilisateurRepository.findByUsername(event.getUsername()).ifPresentOrElse(
        utilisateur -> {
          // Récupérer l'email depuis la table utilisateurs si la colonne existe
          String email = utilisateurRepository.findEmailByUsername(event.getUsername())
              .orElseGet(() -> {
                // Fallback : générer un email basé sur le username si la colonne n'existe pas
                log.debug("Email non trouvé dans la base, utilisation du fallback pour username: {}",
                    event.getUsername());
                return event.getUsername() + "@cmkerp.local";
              });

          String subject = "Déverrouillage de votre compte CMK-ERP";
          String content = String.format(
              "Bonjour %s,\n\n"
                  + "Votre compte a été déverrouillé dans CMK-ERP.\n"
                  + "Vous pouvez maintenant vous connecter normalement.\n"
                  + "Si vous n'avez pas demandé ce déverrouillage, veuillez contacter l'administrateur système.\n\n"
                  + "Cordialement,\n"
                  + "L'équipe CMK-ERP",
              event.getUsername());

          log.info("Envoi de la notification de déverrouillage -> userId: {}, email: {}", event.getUserId(), email);
          if (emailService != null) {
            emailService.sendNotificationEmail(email, subject, content);
          } else {
            log.debug("EmailService non disponible, notification non envoyée");
          }
        },
        () -> log.warn("Utilisateur non trouvé pour l'événement USER_UNLOCKED -> username: {}", event.getUsername()));
  }

  /**
   * Traite l'événement USER_DELETED : notification et nettoyage des données
   * associées.
   *
   * <p>
   * Note: La suppression physique ou soft delete de l'utilisateur est
   * généralement
   * effectuée dans la couche service avant la publication de l'événement. Ce
   * handler
   * se concentre sur les actions post-suppression :
   * <ul>
   * <li>Envoi d'une notification par email (si l'utilisateur existe encore)</li>
   * <li>Logging détaillé de la suppression pour audit</li>
   * <li>Nettoyage des données associées si nécessaire (tokens, sessions,
   * etc.)</li>
   * </ul>
   *
   * <p>
   * Pour l'anonymisation complète des données, considérer l'implémentation d'un
   * service dédié qui anonymise les données personnelles dans toutes les tables
   * associées (audit_events, sessions, etc.) conformément au RGPD.
   *
   * @param event l'événement USER_DELETED
   */
  private void handleUserDeleted(UserEvent event) {
    log.warn("Traitement de l'événement USER_DELETED -> userId: {}, username: {}", event.getUserId(),
        event.getUsername());

    // Tenter de récupérer l'utilisateur (peut ne plus exister si suppression
    // physique)
    utilisateurRepository.findByUsername(event.getUsername()).ifPresentOrElse(
        utilisateur -> {
          // Récupérer l'email depuis la table utilisateurs si la colonne existe
          String email = utilisateurRepository.findEmailByUsername(event.getUsername())
              .orElseGet(() -> {
                // Fallback : générer un email basé sur le username si la colonne n'existe pas
                log.debug("Email non trouvé dans la base, utilisation du fallback pour username: {}",
                    event.getUsername());
                return event.getUsername() + "@cmkerp.local";
              });

          String subject = "Suppression de votre compte CMK-ERP";
          String content = String.format(
              "Bonjour %s,\n\n"
                  + "Votre compte a été supprimé dans CMK-ERP.\n"
                  + "Toutes vos données personnelles ont été supprimées conformément à notre politique de confidentialité.\n"
                  + "Si vous pensez qu'il s'agit d'une erreur, veuillez contacter l'administrateur système dans les plus brefs délais.\n\n"
                  + "Cordialement,\n"
                  + "L'équipe CMK-ERP",
              event.getUsername());

          log.info("Envoi de la notification de suppression -> userId: {}, email: {}", event.getUserId(), email);
          if (emailService != null) {
            emailService.sendNotificationEmail(email, subject, content);
          } else {
            log.debug("EmailService non disponible, notification non envoyée");
          }
        },
        () -> {
          // L'utilisateur a déjà été supprimé (suppression physique)
          log.info("Utilisateur déjà supprimé de la base de données -> userId: {}, username: {}", event.getUserId(),
              event.getUsername());
        });

    // Logging détaillé pour audit et conformité
    log.warn("Suppression d'utilisateur traitée -> userId: {}, username: {}. "
        + "Les données associées doivent être nettoyées par les services appropriés (sessions, tokens, etc.).",
        event.getUserId(), event.getUsername());
  }
}
