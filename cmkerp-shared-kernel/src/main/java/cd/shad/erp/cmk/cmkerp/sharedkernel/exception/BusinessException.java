package cd.shad.erp.cmk.cmkerp.sharedkernel.exception;

/**
 * Exception thrown for business/functional validation errors. Examples: invalid input, constraint
 * violation, business rule failure.
 */
public class BusinessException extends CmkBaseException {

  private static final long serialVersionUID = 1L;
  private static final String ERROR_CODE = "ERR_BUSINESS";

  /**
   * Construct a BusinessException with a message.
   *
   * @param message the error message describing the business rule violation
   */
  public BusinessException(String message) {
    super(ERROR_CODE, message);
  }

  /**
   * Construct a BusinessException with message and cause.
   *
   * @param message the error message describing the business rule violation
   * @param cause the underlying exception
   */
  public BusinessException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }

  /**
   * Construct a BusinessException with a custom error code and message.
   *
   * @param errorCode custom error code (e.g., "ERR_INVALID_USER_STATUS")
   * @param message the error message
   */
  public BusinessException(String errorCode, String message) {
    super(errorCode, message);
  }

  /**
   * Construct a BusinessException with custom error code, message and cause.
   *
   * @param errorCode custom error code
   * @param message the error message
   * @param cause the underlying exception
   */
  public BusinessException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
