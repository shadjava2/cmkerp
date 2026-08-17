package cd.shad.erp.cmk.cmkerp.stocks.application.validation;

import org.springframework.stereotype.Service;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour valider les transactions avant save/update/delete.
 *
 * Centralise les validations qui doivent être effectuées avant toute opération de persistance pour
 * garantir l'intégrité des données.
 *
 * Architecture: Pattern Service pour centraliser les validations transactionnelles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionValidationService {

  /**
   * Valide qu'une opération peut être effectuée.
   *
   * @param entity l'entité à valider
   * @param operation l'opération (CREATE, UPDATE, DELETE)
   * @throws BusinessException si la validation échoue
   */
  public <T> void validateBeforeOperation(T entity, OperationType operation) {
    if (entity == null) {
      throw new IllegalArgumentException(
          String.format("L'entité ne peut pas être nulle pour l'opération %s", operation));
    }

    log.debug("Validation avant {} pour {}", operation, entity.getClass().getSimpleName());

    // Validations communes
    validateEntityState(entity, operation);

    // Validations spécifiques selon l'opération
    switch (operation) {
      case CREATE:
        validateBeforeCreate(entity);
        break;
      case UPDATE:
        validateBeforeUpdate(entity);
        break;
      case DELETE:
        validateBeforeDelete(entity);
        break;
      default:
        log.warn("Type d'opération non géré: {}", operation);
    }
  }

  /**
   * Valide l'état de l'entité.
   */
  private <T> void validateEntityState(T _entity,
      OperationType operation) {
    // Validation de base - peut être étendue selon les besoins
    log.debug("Validation de l'état de l'entité pour {}", operation);
  }

  /**
   * Validations spécifiques avant création.
   */
  private <T> void validateBeforeCreate(T _entity) {
    log.debug("Validations spécifiques avant création");
    // À étendre selon les besoins
  }

  /**
   * Validations spécifiques avant mise à jour.
   */
  private <T> void validateBeforeUpdate(T _entity) {
    log.debug("Validations spécifiques avant mise à jour");
    // À étendre selon les besoins
  }

  /**
   * Validations spécifiques avant suppression.
   */
  private <T> void validateBeforeDelete(T _entity) {
    log.debug("Validations spécifiques avant suppression");
    // À étendre selon les besoins
  }

  /**
   * Type d'opération.
   */
  public enum OperationType {
    CREATE, UPDATE, DELETE
  }
}
