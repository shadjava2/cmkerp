package cd.shad.erp.cmk.cmkerp.sharedkernel.exception;

/**
 * Exception thrown for low-level/infrastructure problems. Examples: database connection error,
 * transaction failure, I/O error.
 */
public class TechnicalException extends CmkBaseException {

  private static final long serialVersionUID = 1L;
  private static final String ERROR_CODE = "ERR_TECHNICAL";

  /**
   * Construct a TechnicalException with a message.
   *
   * @param message the error message describing the technical problem
   */
  public TechnicalException(String message) {
    super(ERROR_CODE, message);
  }

  /**
   * Construct a TechnicalException with message and cause.
   *
   * @param message the error message describing the technical problem
   * @param cause the underlying exception (typically from Spring or database)
   */
  public TechnicalException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }

  /**
   * Construct a TechnicalException with a custom error code and message.
   *
   * @param errorCode custom error code (e.g., "ERR_DB_CONNECTION")
   * @param message the error message
   */
  public TechnicalException(String errorCode, String message) {
    super(errorCode, message);
  }

  /**
   * Construct a TechnicalException with custom error code, message and cause.
   *
   * @param errorCode custom error code
   * @param message the error message
   * @param cause the underlying exception
   */
  public TechnicalException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
