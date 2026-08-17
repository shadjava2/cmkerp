package cd.shad.erp.cmk.cmkerp.sharedkernel.exception;

/**
 * Exception thrown when an expected entity is not found in the database.
 */
public class NotFoundException extends CmkBaseException {

  private static final long serialVersionUID = 1L;
  private static final String ERROR_CODE = "ERR_NOT_FOUND";

  /**
   * Construct a NotFoundException with a simple message.
   *
   * @param message the error message
   */
  public NotFoundException(String message) {
    super(ERROR_CODE, message);
  }

  /**
   * Construct a NotFoundException with message and cause.
   *
   * @param message the error message
   * @param cause the underlying exception
   */
  public NotFoundException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }

  /**
   * Factory method: create a NotFoundException for a missing entity by ID.
   *
   * @param entityName the name of the entity class (e.g., "User")
   * @param id the id value that was not found
   * @return a new NotFoundException
   */
  public static NotFoundException entity(String entityName, Object id) {
    String message = String.format("%s with id '%s' not found", entityName, id);
    return new NotFoundException(message);
  }

  /**
   * Factory method: create a NotFoundException for a missing entity by field value.
   *
   * @param entityName the name of the entity class (e.g., "User")
   * @param field the field name (e.g., "username")
   * @param value the field value that was not found
   * @return a new NotFoundException
   */
  public static NotFoundException byField(String entityName, String field, Object value) {
    String message = String.format("%s with %s='%s' not found", entityName, field, value);
    return new NotFoundException(message);
  }
}
