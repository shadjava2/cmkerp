package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour gérer l'audit trail (traçabilité) des opérations.
 *
 * Centralise la gestion des champs d'audit: - userCreatedId / userUpdatedId - dateCreate /
 * dateUpdate
 *
 * Architecture: Pattern Service pour centraliser la logique d'audit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditTrailService {

  /**
   * Configure les champs d'audit pour une création.
   *
   * @param entity l'entité à configurer (doit avoir userCreatedId et dateCreate)
   * @param userId l'ID de l'utilisateur qui crée
   */
  public <T> void setCreationAudit(T entity, Long userId) {
    try {
      // Utilisation de la réflexion pour définir les champs d'audit
      // Les entités doivent avoir ces méthodes ou champs
      java.lang.reflect.Method setUserCreatedId =
          entity.getClass().getMethod("setUserCreatedId", Long.class);
      java.lang.reflect.Method setDateCreate =
          entity.getClass().getMethod("setDateCreate", LocalDateTime.class);

      setUserCreatedId.invoke(entity, userId);
      setDateCreate.invoke(entity, LocalDateTime.now());

      log.debug("Audit trail de création configuré pour {}: userId={}",
          entity.getClass().getSimpleName(), userId);
    } catch (Exception e) {
      log.warn("Impossible de configurer l'audit trail pour {}: {}",
          entity.getClass().getSimpleName(), e.getMessage());
    }
  }

  /**
   * Configure les champs d'audit pour une mise à jour.
   *
   * @param entity l'entité à configurer (doit avoir userUpdatedId et dateUpdate)
   * @param userId l'ID de l'utilisateur qui met à jour
   */
  public <T> void setUpdateAudit(T entity, Long userId) {
    try {
      // Utilisation de la réflexion pour définir les champs d'audit
      java.lang.reflect.Method setUserUpdatedId =
          entity.getClass().getMethod("setUserUpdatedId", Long.class);
      java.lang.reflect.Method setDateUpdate =
          entity.getClass().getMethod("setDateUpdate", LocalDateTime.class);

      setUserUpdatedId.invoke(entity, userId);
      setDateUpdate.invoke(entity, LocalDateTime.now());

      log.debug("Audit trail de mise à jour configuré pour {}: userId={}",
          entity.getClass().getSimpleName(), userId);
    } catch (Exception e) {
      log.warn("Impossible de configurer l'audit trail pour {}: {}",
          entity.getClass().getSimpleName(), e.getMessage());
    }
  }
}

























